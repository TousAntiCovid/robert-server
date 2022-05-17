package fr.gouv.stopc.e2e.kpis.repository;

import fr.gouv.stopc.e2e.kpis.model.BatchStatistics;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
public interface BatchStatisticsRepository extends MongoRepository<BatchStatistics, Instant> {

}
