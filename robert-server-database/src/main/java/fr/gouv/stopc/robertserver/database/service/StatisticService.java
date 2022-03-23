package fr.gouv.stopc.robertserver.database.service;

import fr.gouv.stopc.robertserver.database.model.Statistic;
import fr.gouv.stopc.robertserver.database.repository.StatisticRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StatisticService {

    private final StatisticRepository repository;

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

    public long countNbNotifiedTotalBetween(Instant from, Instant to) {
        return repository.countNbNotifiedTotalBetween(from, to).stream().mapToLong(Statistic::getNotifiedTotal).sum();
    }
}
