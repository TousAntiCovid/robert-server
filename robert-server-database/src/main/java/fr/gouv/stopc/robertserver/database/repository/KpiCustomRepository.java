package fr.gouv.stopc.robertserver.database.repository;

public interface KpiCustomRepository {

    void incrementNotifiedUsers();

    void incrementReportsCount();

    void incrementUsersAboveRiskThresholdButRetentionPeriodExpired();
}
