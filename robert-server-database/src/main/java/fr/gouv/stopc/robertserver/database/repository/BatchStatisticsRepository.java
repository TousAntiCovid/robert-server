package fr.gouv.stopc.robertserver.database.repository;

import fr.gouv.stopc.robertserver.database.model.BatchStatistics;
import org.springframework.data.domain.Range;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface BatchStatisticsRepository
        extends MongoRepository<BatchStatistics, Instant>, BatchStatisticsCustomRepository {

    List<BatchStatistics> findByJobStartInstantBetween(Range<Instant> range);

}
