package fr.gouv.stopc.robertserver.database.repository;

import fr.gouv.stopc.robertserver.database.model.Statistic;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface StatisticRepository extends MongoRepository<Statistic, Instant> {

    @Aggregation(pipeline = {
            "{$match : { _id: { $gte: ?0, $lte: ?1 }}}"
    })
    List<Statistic> countNbNotifiedTotalBetween(Instant from, Instant to);

}
