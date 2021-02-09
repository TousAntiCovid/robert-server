package fr.gouv.stopc.robertserver.database.service;

import java.util.List;

public interface ItemIdMappingService<T> {
    void deleteAll();

    List<T> getItemIdMappingsBetweenIds(long startId, long endId);
}
