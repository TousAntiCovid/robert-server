package fr.gouv.clea.consumer.service.impl;

import fr.gouv.clea.consumer.model.ExposedVisitEntity;
import fr.gouv.clea.consumer.model.Visit;
import fr.gouv.clea.consumer.repository.IExposedVisitRepository;
import fr.gouv.clea.consumer.service.IVisitExpositionAggregatorService;
import fr.inria.clea.lsp.utils.TimeUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@Component
@Slf4j
public class VisitExpositionAggregatorService implements IVisitExpositionAggregatorService {

    private final IExposedVisitRepository repository;

    private final int durationUnitInSeconds;

    @Autowired
    public VisitExpositionAggregatorService(
            IExposedVisitRepository repository,
            @Value("${clea.conf.durationUnitInSeconds}") int durationUnitInSeconds
    ) {
        this.repository = repository;
        this.durationUnitInSeconds = durationUnitInSeconds;
    }

    @Override
    public void updateExposureCount(Visit visit) {
        Instant periodStartAsInstant = this.periodStartFromCompressedPeriodStartAsInstant(visit.getCompressedPeriodStartTime());
        long scanTimeSlot = Duration.between(visit.getQrCodeScanTime(), periodStartAsInstant).abs().toSeconds() / durationUnitInSeconds;
        int exposureTime = this.getExposureTime(visit.getVenueType(), visit.getVenueCategory1(), visit.getVenueCategory2(), visit.isStaff());
        int firstExposedSlot = Math.max(0, (int) scanTimeSlot - exposureTime);
        int lastExposedSlot = Math.min(visit.getPeriodDuration(), (int) scanTimeSlot + exposureTime);

        System.out.println(scanTimeSlot);
        System.out.println(firstExposedSlot);
        System.out.println(lastExposedSlot);

        List<ExposedVisitEntity> exposedVisits = repository.findAllByLocationTemporaryPublicIdAndPeriodStart(visit.getLocationTemporaryPublicId(), this.periodStartFromCompressedPeriodStart(visit.getCompressedPeriodStartTime()));

        List<ExposedVisitEntity> toUpdate = new ArrayList<>();
        List<ExposedVisitEntity> toPersist = new ArrayList<>();

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
        }
    }

    private long periodStartFromCompressedPeriodStart(long compressedPeriodStartTime) {
        return compressedPeriodStartTime * TimeUtils.NB_SECONDS_PER_HOUR;
    }

    private Instant periodStartFromCompressedPeriodStartAsInstant(long compressedPeriodStartTime) {
        return TimeUtils.instantFromTimestamp(this.periodStartFromCompressedPeriodStart(compressedPeriodStartTime));
    }

    private ExposedVisitEntity updateExposedVisit(Visit visit, ExposedVisitEntity exposedVisit) {
        if (visit.isBackward()) {
            exposedVisit.setBackwardVisits(exposedVisit.getBackwardVisits() + 1);
        } else {
            exposedVisit.setForwardVisits(exposedVisit.getForwardVisits() + 1);
        }
        return exposedVisit;
    }

    private ExposedVisitEntity newExposedVisit(Visit visit, int slotIndex) {
        // TODO: visit.getPeriodStart returning an Instant
        long periodStart = this.periodStartFromCompressedPeriodStart(visit.getCompressedPeriodStartTime());
        return ExposedVisitEntity.builder()
                .locationTemporaryPublicId(visit.getLocationTemporaryPublicId())
                .venueType(visit.getVenueType())
                .venueCategory1(visit.getVenueCategory1())
                .venueCategory2(visit.getVenueCategory2())
                .periodStart(periodStart)
                .timeSlot(slotIndex)
                .backwardVisits(visit.isBackward() ? 1 : 0)
                .forwardVisits(visit.isBackward() ? 0 : 1)
                .qrCodeScanTime(visit.getQrCodeScanTime())
                .build();
    }

    /**
     * @return The exposure time of a visit expressed as the number of EXPOSURE_TIME_UNIT.
     * e.g. if EXPOSURE_TIME_UNIT is 3600 sec (one hour), an exposure time equals to 3 means 3 hours
     * if EXPOSURE_TIME_UNIT is 1800 sec (30 minutes), an exposure time equals to 3 means 1,5 hour.
     */
    private int getExposureTime(int venueType, int venueCategory1, int venueCategory2, boolean staff) {
        return 3;
    }
}
