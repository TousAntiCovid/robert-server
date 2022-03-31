package fr.gouv.stopc.robertserver.database.repository;

import fr.gouv.stopc.robertserver.database.model.WebserviceStatistic;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.time.Instant;

@RequiredArgsConstructor
public class WebserviceStatisticsCustomRepositoryImpl implements WebserviceStatisticsCustomRepository {

    private final MongoOperations mongoOperations;

    @Override
    public void increment(Instant date) {
        Query query = new Query().addCriteria(Criteria.where("_id").is(date));
        Update update = new Update().inc("notifiedTotal", 1);
        FindAndModifyOptions options = FindAndModifyOptions.options()
                .upsert(true)
                .returnNew(true);
        mongoOperations.findAndModify(query, update, options, WebserviceStatistic.class);
    }
}
