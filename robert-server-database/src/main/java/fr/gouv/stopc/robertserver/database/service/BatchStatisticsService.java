package fr.gouv.stopc.robertserver.database.service;

import fr.gouv.stopc.robertserver.database.model.BatchStatistics;
import fr.gouv.stopc.robertserver.database.repository.BatchStatisticsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Range;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class BatchStatisticsService {

    private final BatchStatisticsRepository repository;

    public long countUsersAboveRiskThresholdButRetentionPeriodExpired(Range<Instant> range) {
        final var batchStatistics = repository.getBatchStatisticsByBatchExecutionBetween(range);
        return batchStatistics.stream()
                .mapToLong(BatchStatistics::getUsersAboveRiskThresholdButRetentionPeriodExpired)
                .sum();
    }

    public void incrementUsersAboveRiskThresholdButRetentionPeriodExpired(Instant date) {
        repository.incrementUsersAboveRiskThresholdButRetentionPeriodExpired(date);
    }

}
