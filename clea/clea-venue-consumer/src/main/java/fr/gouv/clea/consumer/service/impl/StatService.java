package fr.gouv.clea.consumer.service.impl;

import fr.gouv.clea.consumer.model.StatLocation;
import fr.gouv.clea.consumer.model.StatLocationKey;
import fr.gouv.clea.consumer.model.Visit;
import fr.gouv.clea.consumer.repository.IStatLocationRepository;
import fr.gouv.clea.consumer.service.IStatService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Component
@Slf4j
public class StatService implements IStatService {

    private final IStatLocationRepository repository;

    private final int durationUnitInSeconds;

    @Autowired
    public StatService(
            IStatLocationRepository repository,
            @Value("${clea.conf.durationUnitInSeconds}") int durationUnitInSeconds
    ) {
        this.repository = repository;
        this.durationUnitInSeconds = durationUnitInSeconds;
    }

    @Override
    public void logStats(Visit visit) {
        Instant periodStartAsInstant = VisitExpositionAggregatorService.periodStartFromCompressedPeriodStartAsInstant(visit.getCompressedPeriodStartTime());
        long scanTimeSlot = Duration.between(periodStartAsInstant, visit.getQrCodeScanTime()).toSeconds() / durationUnitInSeconds;
        Instant period = periodStartAsInstant.plus(scanTimeSlot * durationUnitInSeconds, ChronoUnit.SECONDS);

        StatLocationKey statLocationKey = StatLocationKey.builder()
                .period(period)
                .venueType(visit.getVenueType())
                .venueCategory1(visit.getVenueCategory1())
                .venueCategory2(visit.getVenueCategory2())
                .build();

        Optional<StatLocation> optional = repository.findById(statLocationKey);
        StatLocation statLocation;

        if (optional.isEmpty()) {
            statLocation = StatLocation.builder()
                    .statLocationKey(statLocationKey)
                    .backwardVisits(visit.isBackward() ? 1 : 0)
                    .forwardVisits(visit.isBackward() ? 0 : 1)
                    .build();
        } else {
            statLocation = optional.get();
            if (visit.isBackward()) {
                statLocation.setBackwardVisits(statLocation.getBackwardVisits() + 1);
            } else {
                statLocation.setForwardVisits(statLocation.getForwardVisits() + 1);
            }
            repository.save(statLocation);
        }

        repository.save(statLocation);
        log.info("saved stat period: {}, venueType: {} venueCategory1: {}, venueCategory2: {}, backwardVisits: {}, forwardVisits: {}",
                statLocation.getStatLocationKey().getPeriod(),
                statLocation.getStatLocationKey().getVenueType(),
                statLocation.getStatLocationKey().getVenueCategory1(),
                statLocation.getStatLocationKey().getVenueCategory2(),
                statLocation.getBackwardVisits(),
                statLocation.getForwardVisits()
        );
    }
}
