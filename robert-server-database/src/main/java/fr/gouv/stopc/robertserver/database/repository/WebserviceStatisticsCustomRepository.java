package fr.gouv.stopc.robertserver.database.repository;

import java.time.Instant;

public interface WebserviceStatisticsCustomRepository {

    void increment(Instant date);
}
