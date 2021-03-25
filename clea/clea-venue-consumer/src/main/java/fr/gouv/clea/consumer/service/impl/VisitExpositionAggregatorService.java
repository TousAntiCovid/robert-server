package fr.gouv.clea.consumer.service.impl;

import fr.gouv.clea.consumer.model.ExposedVisitEntity;
import fr.gouv.clea.consumer.model.Visit;
import fr.gouv.clea.consumer.repository.IExposedVisitRepository;
import fr.gouv.clea.consumer.service.IVisitExpositionAggregatorService;
import fr.inria.clea.lsp.utils.TimeUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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

    private final static long EXPOSURE_TIME_UNIT = TimeUtils.NB_SECONDS_PER_HOUR;
    private final IExposedVisitRepository repository;

    @Autowired
    public VisitExpositionAggregatorService(IExposedVisitRepository repository) {
        this.repository = repository;
    }

    @Override
    public void updateExposureCount(Visit visit) {
        Instant periodStartInstant = periodStartInstant(visit);
        long scanTimeSlot = Duration.between(periodStartInstant, visit.getQrCodeScanTime()).toSeconds() / EXPOSURE_TIME_UNIT;
        int exposureTime = this.getExposureTime(visit.getVenueType(), visit.getVenueCategory1(), visit.getVenueCategory2(), visit.isStaff());
        int firstExposedSlot = Math.max(0, (int) scanTimeSlot - exposureTime);
        int lastExposedSlot = Math.min(visit.getPeriodDuration(), (int) scanTimeSlot + exposureTime);

        List<ExposedVisitEntity> exposedVisits = repository.findAllByLocationTemporaryPublicIdAndPeriodStart(visit.getLocationTemporaryPublicId().toString(), periodStartInstant.toEpochMilli());

        List<ExposedVisitEntity> toUpdate = new ArrayList<>();
        List<ExposedVisitEntity> toPersist = new ArrayList<>();

        IntStream.rangeClosed(Math.min(firstExposedSlot, lastExposedSlot), Math.max(firstExposedSlot, lastExposedSlot))
                .forEach(slotIndex ->
                        exposedVisits.stream()
                                .filter(exposedVisit -> exposedVisit.getTimeSlot() == slotIndex)
                                .findFirst()
                                .ifPresentOrElse(
                                        exposedVisit -> toUpdate.add(this.updateExposureCount(visit, exposedVisit)),
                                        () -> toPersist.add(this.newExposureCount(visit, slotIndex))
                                )
                );

        if (!toPersist.isEmpty()) {
            log.info("Persisting {} new visits!", toPersist.size());
            log.info("Updating {} exsisting visits!", toUpdate.size());
            repository.saveAll(Stream.concat(toUpdate.stream(), toPersist.stream()).collect(Collectors.toList()));
        }
    }

    protected Instant periodStartInstant(Visit visit) {
        return TimeUtils.instantFromTimestamp((long) visit.getCompressedPeriodStartTime() * TimeUtils.NB_SECONDS_PER_HOUR);
    }

    protected ExposedVisitEntity updateExposureCount(Visit visit, ExposedVisitEntity exposedVisit) {
        if (visit.isBackward()) {
            exposedVisit.setBackwardVisits(exposedVisit.getBackwardVisits() + 1);
        } else {
            exposedVisit.setForwardVisits(exposedVisit.getForwardVisits() + 1);
        }
        return exposedVisit;
    }

    protected ExposedVisitEntity newExposureCount(Visit visit, int slotIndex) {
        // TODO: visit.getPeriodStart returning an Instant
        long periodStart = this.periodStartInstant(visit).toEpochMilli();
        return ExposedVisitEntity.builder()
                .locationTemporaryPublicId(visit.getStringLocationTemporaryPublicId())
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
    protected int getExposureTime(int venueType, int venueCategory1, int venueCategory2, boolean staff) {
        return 3;
    }
}
