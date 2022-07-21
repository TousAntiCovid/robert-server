package fr.gouv.stopc.robertserver.database.repository;

public interface WebserviceStatisticsCustomRepository {

    void incrementNotifiedUsers();

    void incrementReportsCount();

}
