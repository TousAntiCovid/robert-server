package fr.gouv.stopc.robertserver.database.service.impl;

import fr.gouv.stopc.robertserver.database.model.Statistic;
import fr.gouv.stopc.robertserver.database.repository.StatisticsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final StatisticsRepository repository;

    public Optional<Statistic> getByDate(Instant date) {
        // create if not exist
        Optional<Statistic> exist = repository.findById(date);
        if (exist.isEmpty()) {
            repository.save(
                    Statistic.builder()
                            .date(date)
                            .notifiedTotal(0)
                            .build()
            );
        }
        return repository.findById(date);
    }

    public void incrementNotifiedTotal(Statistic statistic) {
        statistic.incrementNotifiedTotal();
        repository.save(statistic);
    }

    public int countNbNotifiedTotalBetween(Instant from, Instant to) {
        return repository.countNbNotifiedTotalBetween(from, to).stream().mapToInt(Statistic::getNotifiedTotal).sum();
    }
}
