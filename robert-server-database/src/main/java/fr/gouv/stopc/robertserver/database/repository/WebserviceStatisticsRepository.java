package fr.gouv.stopc.robertserver.database.repository;

import fr.gouv.stopc.robertserver.database.model.WebserviceStatistics;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
public interface WebserviceStatisticsRepository
        extends MongoRepository<WebserviceStatistics, Instant>, WebserviceStatisticsCustomRepository {

}
