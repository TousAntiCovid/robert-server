package fr.gouv.stopc.robertserver.database.service;

import fr.gouv.stopc.robertserver.database.model.Registration;
import fr.gouv.stopc.robertserver.database.model.WebserviceStatistic;
import fr.gouv.stopc.robertserver.database.repository.WebserviceStatisticsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

import static java.time.Instant.now;
import static java.time.temporal.ChronoUnit.DAYS;

@Service
@RequiredArgsConstructor
public class WebserviceStatisticsService {

    private final WebserviceStatisticsRepository repository;

    public long countNbNotifiedTotalBetween(Instant from, Instant to) {
        final var webserviceStatistics = repository.getWebserviceStatisticsBetween(from, to);
        return webserviceStatistics.stream()
                .mapToLong(WebserviceStatistic::getNotifiedTotal)
                .sum();
    }

    public void updateWebserviceStatistics(final Registration registration) {

        if (!registration.isNotifiedForCurrentRisk() && registration.isAtRisk()) {
            repository.increment(now().truncatedTo(DAYS));
        }
    }
}
