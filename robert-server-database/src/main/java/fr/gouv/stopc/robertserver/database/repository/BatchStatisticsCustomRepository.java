package fr.gouv.stopc.robertserver.database.repository;

import java.time.Instant;

public interface BatchStatisticsCustomRepository {

    void incrementUsersAboveRiskThresholdButRetentionPeriodExpired(Instant date);

    void saveNbNotifiedUsersScoredAgainInStatistics(Instant date, Long nbNotifiedUsersScoredAgain);

    void saveNbExposedButNotAtRiskUsersInStatistics(Instant date, Long nbExposedUsersButNotAtRisk);
}
