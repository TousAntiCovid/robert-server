package fr.gouv.stopc.robert.server.batch.service;

import fr.gouv.stopc.robertserver.database.repository.BatchStatisticsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class BatchStatisticsService {

    private final BatchStatisticsRepository repository;

    public void incrementUsersAboveRiskThresholdButRetentionPeriodExpired(Instant date) {
        repository.incrementUsersAboveRiskThresholdButRetentionPeriodExpired(date);
    }

}
