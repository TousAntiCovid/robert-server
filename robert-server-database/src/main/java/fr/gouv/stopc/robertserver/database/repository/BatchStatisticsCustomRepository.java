package fr.gouv.stopc.robertserver.database.repository;

import java.time.Instant;

public interface BatchStatisticsCustomRepository {

    void increment(Instant date);
}
