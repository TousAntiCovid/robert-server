package fr.gouv.stopc.robertserver.database.service;

import fr.gouv.stopc.robertserver.database.model.BatchStatistics;
import fr.gouv.stopc.robertserver.database.repository.BatchStatisticsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class BatchStatisticsService {

    private final BatchStatisticsRepository repository;

    public long countNbUsersAboveThresholdButNotAtRiskBetween(Instant from, Instant to) {
        final var batchStatistics = repository.getBatchStatisticsByBatchExecutionBetween(from, to);
        return batchStatistics.stream()
                .mapToLong(BatchStatistics::getNbUsersAboveThresholdButNotAtRisk)
                .sum();
    }

    public void increment(Instant date) {
        repository.increment(date);
    }

}
