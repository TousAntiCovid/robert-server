package fr.gouv.clea.consumer.service.impl;

import fr.gouv.clea.consumer.configuration.VenueConsumerConfiguration;
import fr.gouv.clea.consumer.model.StatLocation;
import fr.gouv.clea.consumer.model.StatLocationKey;
import fr.gouv.clea.consumer.model.Visit;
import fr.gouv.clea.consumer.repository.IStatLocationRepository;
import fr.gouv.clea.consumer.service.IStatService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Component
@Slf4j
public class StatService implements IStatService {

    private final IStatLocationRepository repository;

    private final VenueConsumerConfiguration config;

    @Autowired
    public StatService(
            IStatLocationRepository repository,
            VenueConsumerConfiguration configuration) {
        this.repository = repository;
        this.config = configuration;
    }

    @Override
    public void logStats(Visit visit) {
        StatLocationKey statLocationKey = StatLocationKey.builder()
                .period(this.getStatPeriod(visit))
                .venueType(visit.getVenueType())
                .venueCategory1(visit.getVenueCategory1())
                .venueCategory2(visit.getVenueCategory2())
                .build();

        Optional<StatLocation> optional = repository.findById(statLocationKey);

        StatLocation statLocation;
        if (optional.isEmpty()) {
            statLocation = newStatLocation(statLocationKey, visit);
        } else {
            statLocation = updateStatLocation(optional.get(), visit);
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

    protected StatLocation newStatLocation(StatLocationKey statLocationKey, Visit visit) {
        return StatLocation.builder()
                .statLocationKey(statLocationKey)
                .backwardVisits(visit.isBackward() ? 1 : 0)
                .forwardVisits(visit.isBackward() ? 0 : 1)
                .build();
    }

    protected StatLocation updateStatLocation(StatLocation statLocation, Visit visit) {
        if (visit.isBackward()) {
            statLocation.setBackwardVisits(statLocation.getBackwardVisits() + 1);
        } else {
            statLocation.setForwardVisits(statLocation.getForwardVisits() + 1);
        }
        return statLocation;
    }

    protected Instant getStatPeriod(Visit visit) {
        long secondsToRemove = visit.getQrCodeScanTime().getEpochSecond() % config.getStatSlotDurationInSeconds();
        return visit.getQrCodeScanTime().minus(secondsToRemove, ChronoUnit.SECONDS);
    }
}
