package fr.gouv.stopc.robertserver.database.repository;

public interface KpiCustomRepository {

    void incrementKpi(String name);

    void upsert(String name, Long value);

}
