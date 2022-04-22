package fr.gouv.stopc.robert.server.batch.service;

import fr.gouv.stopc.robertserver.database.repository.BatchStatisticsRepository;
import fr.gouv.stopc.robertserver.database.repository.RegistrationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class BatchStatisticsService {

    private final BatchStatisticsRepository batchStatisticsRepository;

    private final RegistrationRepository registrationRepository;

    public void incrementUsersAboveRiskThresholdButRetentionPeriodExpired(Instant date) {
        batchStatisticsRepository.incrementUsersAboveRiskThresholdButRetentionPeriodExpired(date);
    }

    public void saveNbNotifiedUsersScoredAgainInStatistics(Instant date) {
        final var nbNotifiedUsersScoredAgain = registrationRepository.countNbNotifiedUsersScoredAgain();
        batchStatisticsRepository.saveNbNotifiedUsersScoredAgainInStatistics(date, nbNotifiedUsersScoredAgain);
    }

    public void saveNbExposedButNotAtRiskUsersInStatistics(Instant date) {
        final var nbExposedUsersButNotAtRisk = registrationRepository.countNbExposedUsersButNotAtRisk();
        batchStatisticsRepository.saveNbExposedButNotAtRiskUsersInStatistics(date, nbExposedUsersButNotAtRisk);
    }
}
