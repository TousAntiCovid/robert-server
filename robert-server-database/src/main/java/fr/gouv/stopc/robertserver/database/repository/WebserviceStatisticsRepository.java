package fr.gouv.stopc.robertserver.database.repository;

import fr.gouv.stopc.robertserver.database.model.WebserviceStatistics;
import org.springframework.data.domain.Range;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface WebserviceStatisticsRepository
        extends MongoRepository<WebserviceStatistics, Instant>, WebserviceStatisticsCustomRepository {

    Optional<WebserviceStatistics> findByDate(Instant date);

    List<WebserviceStatistics> getWebserviceStatisticsByDateBetween(Range<Instant> range);

}
