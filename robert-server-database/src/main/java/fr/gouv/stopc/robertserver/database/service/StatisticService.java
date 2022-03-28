package fr.gouv.stopc.robertserver.database.service;

import fr.gouv.stopc.robertserver.database.model.Registration;
import fr.gouv.stopc.robertserver.database.model.WebserviceStatistic;
import fr.gouv.stopc.robertserver.database.repository.StatisticRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

import static java.time.Instant.now;
import static java.time.temporal.ChronoUnit.DAYS;

@Service
@RequiredArgsConstructor
public class StatisticService {

    private final StatisticRepository repository;

    private final IRegistrationService registrationService;

    public Optional<WebserviceStatistic> incrementOrCreateForDate(Instant date) {

        final var retrievedStatistic = repository.findById(date);
        if (retrievedStatistic.isEmpty()) {
            repository.save(
                    WebserviceStatistic.builder()
                            .date(date)
                            .notifiedTotal(1)
                            .build()
            );
        }
        return repository.findById(date);
    }

    public long countNbNotifiedTotalBetween(Instant from, Instant to) {
        return repository.countNbNotifiedBetween(from, to).stream()
                .mapToLong(WebserviceStatistic::getNotifiedTotal)
                .sum();
    }

    public void updateWebserviceStatistics(final Registration registration) {

        if (!registration.isNotifiedForCurrentRisk() && registration.isAtRisk()) {
            incrementOrCreateForDate(now().truncatedTo(DAYS))
                    .ifPresent((WebserviceStatistic::incrementNotifiedTotal));
            registration.setNotifiedForCurrentRisk(true);
            registrationService.saveRegistration(registration);
        }
    }
}
