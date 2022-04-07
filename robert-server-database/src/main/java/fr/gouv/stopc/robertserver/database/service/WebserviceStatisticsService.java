package fr.gouv.stopc.robertserver.database.service;

import fr.gouv.stopc.robertserver.database.model.Registration;
import fr.gouv.stopc.robertserver.database.model.WebserviceStatistics;
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

    public long countNbNotifiedUsersBetween(Instant from, Instant to) {
        final var webserviceStatistics = repository.getWebserviceStatisticsByDateBetween(from, to);
        return webserviceStatistics.stream()
                .mapToLong(WebserviceStatistics::getNbNotifiedUsers)
                .sum();
    }

    public void updateWebserviceStatistics(final Registration registration) {

        if (!registration.isNotifiedForCurrentRisk() && registration.isAtRisk()) {
            repository.increment(now().truncatedTo(DAYS));
        }
    }
}
