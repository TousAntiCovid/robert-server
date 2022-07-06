package fr.gouv.stopc.robertserver.database.repository;

import fr.gouv.stopc.robertserver.database.model.WebserviceStatistics;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.time.Instant;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;

@RequiredArgsConstructor
public class WebserviceStatisticsCustomRepositoryImpl implements WebserviceStatisticsCustomRepository {

    private final MongoOperations mongoOperations;

    @Override
    public void incrementNotifiedUsers(Instant date) {
        final var query = new Query().addCriteria(Criteria.where("date").is(date));
        final var update = new Update().inc("notifiedUsers", 1);
        final var options = FindAndModifyOptions.options()
                .upsert(true)
                .returnNew(true);
        mongoOperations.findAndModify(query, update, options, WebserviceStatistics.class);
    }

    @Override
    public void incrementReportsCount(Instant date) {
        final var query = new Query().addCriteria(Criteria.where("date").is(date));
        final var update = new Update().inc("reportsCount", 1);
        final var options = FindAndModifyOptions.options()
                .upsert(true)
                .returnNew(true);
        mongoOperations.findAndModify(query, update, options, WebserviceStatistics.class);
    }

}
