package fr.gouv.stopc.robertserver.database.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import fr.gouv.stopc.robertserver.database.model.ItemIdMapping;

@Repository
public interface ItemIdMappingRepository extends MongoRepository<ItemIdMapping, Long> {

    /**
     * Return the ItemIdMapping list with id between startId and endId in parameter
     *
     * @param startId minimum include id
     * @param endId maximum include id
     *
     * @return ItemIdMapping list
     */
    @Query(value="{_id: { $gte: ?0, $lte: ?1} }", fields="{itemId : 1, _id : 0}")
    List<ItemIdMapping> getItemIdMappingsBetweenIds(long startId, long endId);
}
