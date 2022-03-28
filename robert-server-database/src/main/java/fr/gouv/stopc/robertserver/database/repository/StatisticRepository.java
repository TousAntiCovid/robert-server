package fr.gouv.stopc.robertserver.database.repository;

import fr.gouv.stopc.robertserver.database.model.WebserviceStatistic;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface StatisticRepository extends MongoRepository<WebserviceStatistic, Instant> {

    @Aggregation(pipeline = {
            "{$match : { _id: { $gte: ?0, $lte: ?1 }}}"
    })
    List<WebserviceStatistic> countNbNotifiedBetween(Instant from, Instant to);

}
