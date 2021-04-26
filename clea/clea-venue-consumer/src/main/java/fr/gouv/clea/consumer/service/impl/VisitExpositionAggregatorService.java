package fr.gouv.clea.consumer.service.impl;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import fr.gouv.clea.consumer.configuration.VenueConsumerConfiguration;
import fr.gouv.clea.consumer.model.ExposedVisitEntity;
import fr.gouv.clea.consumer.model.Visit;
import fr.gouv.clea.consumer.repository.IExposedVisitRepository;
import fr.gouv.clea.consumer.service.IStatService;
import fr.gouv.clea.consumer.service.IVisitExpositionAggregatorService;
import fr.gouv.clea.scoring.configuration.exposure.ExposureTimeConfiguration;
import fr.gouv.clea.scoring.configuration.exposure.ExposureTimeRule;
import fr.inria.clea.lsp.utils.TimeUtils;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class VisitExpositionAggregatorService implements IVisitExpositionAggregatorService {

    private final IExposedVisitRepository repository;
    private final ExposureTimeConfiguration exposureTimeConfig;
    private final VenueConsumerConfiguration consumerConfig;
    private final IStatService statService;

    @Autowired
    public VisitExpositionAggregatorService(
            IExposedVisitRepository repository,
            VenueConsumerConfiguration configuration,
            ExposureTimeConfiguration exposureTimeConfiguration,
            IStatService statService) {
        this.repository = repository;
        this.consumerConfig = configuration;
        this.exposureTimeConfig = exposureTimeConfiguration;
        this.statService = statService;
    }

    @Override
    public void updateExposureCount(Visit visit) {
        Instant periodStartAsInstant = this.periodStartFromCompressedPeriodStartAsInstant(visit.getCompressedPeriodStartTime());
        long scanTimeSlot = Duration.between(periodStartAsInstant, visit.getQrCodeScanTime()).toSeconds() / consumerConfig.getDurationUnitInSeconds();
        if (scanTimeSlot < 0) {
            log.warn("LTId: {}, qrScanTime: {} should not before periodStartTime: {}", visit.getLocationTemporaryPublicId(), visit.getQrCodeScanTime(), periodStartAsInstant);
            return;
        }
        int exposureTime = this.getExposureTime(visit.getVenueType(), visit.getVenueCategory1(), visit.getVenueCategory2(), visit.isStaff(), visit.isBackward());
        int firstExposedSlot = Math.max(0, (int) scanTimeSlot - exposureTime + 1);
        int lastExposedSlot = Math.min(this.getPeriodMaxSlot(visit.getPeriodDuration()), (int) scanTimeSlot + exposureTime - 1);

        List<ExposedVisitEntity> exposedVisits = repository.findAllByLocationTemporaryPublicIdAndPeriodStart(visit.getLocationTemporaryPublicId(), periodStartFromCompressedPeriodStart(visit.getCompressedPeriodStartTime()));

        List<ExposedVisitEntity> toUpdate = new ArrayList<>();
        List<ExposedVisitEntity> toPersist = new ArrayList<>();

        log.info("updateExposureCount: LTId: {}, scanTimeSlot: {}, firstExposedSlot: {}, lastExposedSlot: {} ", visit.getLocationTemporaryPublicId(), scanTimeSlot, firstExposedSlot, lastExposedSlot);

        IntStream.rangeClosed(firstExposedSlot, lastExposedSlot)
                .forEach(slotIndex ->
                        exposedVisits.stream()
                                .filter(exposedVisit -> exposedVisit.getTimeSlot() == slotIndex)
                                .findFirst()
                                .ifPresentOrElse(
                                        exposedVisit -> toUpdate.add(this.updateExposedVisit(visit, exposedVisit)),
                                        () -> toPersist.add(this.newExposedVisit(visit, slotIndex))
                                )
                );

        List<ExposedVisitEntity> merged = Stream.concat(toUpdate.stream(), toPersist.stream()).collect(Collectors.toList());
        if (!merged.isEmpty()) {
            repository.saveAll(merged);
            log.info("Persisting {} new visits!", toPersist.size());
            log.info("Updating {} existing visits!", toUpdate.size());

            statService.logStats(visit);
        } else {
            log.info("LTId: {}, qrScanTime: {} - No visit to persist / update", visit.getLocationTemporaryPublicId(), visit.getQrCodeScanTime());
        }
    }

    /**
     * durationUnitInSeconds must be a value ensuring: 3600 % durationUnitInSeconds = 0
     */
    protected int getPeriodMaxSlot(int periodDuration) {
        // This check should go in venue consumer consumerConfig validation
        if (Duration.ofHours(1).toSeconds() % periodDuration == 0) {
            log.error("durationUnitInSeconds does not have a valid value: {}. 3600(secs) / durationUnitInSeconds has a reminder!", periodDuration);
        }
        if (periodDuration == 255) {
            return Integer.MAX_VALUE;
        }
        int nbSlotsInPeriod = (int) Duration.of(periodDuration, ChronoUnit.HOURS).dividedBy(Duration.of(consumerConfig.getDurationUnitInSeconds(), ChronoUnit.SECONDS));
        return nbSlotsInPeriod - 1; // 0 based index
    }

    protected long periodStartFromCompressedPeriodStart(long compressedPeriodStartTime) {
        return compressedPeriodStartTime * TimeUtils.NB_SECONDS_PER_HOUR;
    }

    protected Instant periodStartFromCompressedPeriodStartAsInstant(long compressedPeriodStartTime) {
        return TimeUtils.instantFromTimestamp(this.periodStartFromCompressedPeriodStart(compressedPeriodStartTime));
    }

    protected ExposedVisitEntity updateExposedVisit(Visit visit, ExposedVisitEntity exposedVisit) {
        if (visit.isBackward()) {
            exposedVisit.setBackwardVisits(exposedVisit.getBackwardVisits() + 1);
        } else {
            exposedVisit.setForwardVisits(exposedVisit.getForwardVisits() + 1);
        }
        return exposedVisit;
    }

    protected ExposedVisitEntity newExposedVisit(Visit visit, int slotIndex) {
        // TODO: visit.getPeriodStart returning an Instant
        long periodStart = periodStartFromCompressedPeriodStart(visit.getCompressedPeriodStartTime());
        return ExposedVisitEntity.builder()
                .locationTemporaryPublicId(visit.getLocationTemporaryPublicId())
                .venueType(visit.getVenueType())
                .venueCategory1(visit.getVenueCategory1())
                .venueCategory2(visit.getVenueCategory2())
                .periodStart(periodStart)
                .timeSlot(slotIndex)
                .backwardVisits(visit.isBackward() ? 1 : 0)
                .forwardVisits(visit.isBackward() ? 0 : 1)
                .build();
    }

    /**
     * @return The exposure time of a visit expressed as the number of EXPOSURE_TIME_UNIT.
     * e.g. if EXPOSURE_TIME_UNIT is 3600 sec (one hour), an exposure time equals to 3 means 3 hours
     * if EXPOSURE_TIME_UNIT is 1800 sec (30 minutes), an exposure time equals to 3 means 1,5 hour.
     */
    protected int getExposureTime(int venueType, int venueCategory1, int venueCategory2, boolean staff, boolean isBackward) {
        ExposureTimeRule rule = exposureTimeConfig.getConfigurationFor(venueType, venueCategory1, venueCategory2);
        int exposureTime;
        if (staff) {
            if (isBackward) {
                exposureTime = rule.getExposureTimeStaffBackward();
            } else {
                exposureTime = rule.getExposureTimeStaffForward();
            }
        } else {
            if (isBackward) {
                exposureTime = rule.getExposureTimeBackward();
            } else {
                exposureTime = rule.getExposureTimeForward();
            }
        }
        return exposureTime;
    }
}
