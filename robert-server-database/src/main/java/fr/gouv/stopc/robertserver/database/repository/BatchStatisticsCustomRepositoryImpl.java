package fr.gouv.stopc.robertserver.database.repository;

import fr.gouv.stopc.robertserver.database.model.BatchStatistics;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.time.Instant;

@RequiredArgsConstructor
public class BatchStatisticsCustomRepositoryImpl implements BatchStatisticsCustomRepository {

    private final MongoOperations mongoOperations;

    @Override
    public void incrementUsersAboveRiskThresholdButRetentionPeriodExpired(Instant date) {
        Query query = new Query().addCriteria(Criteria.where("_id").is(date));
        Update update = new Update().inc("usersAboveRiskThresholdButRetentionPeriodExpired", 1);
        FindAndModifyOptions options = FindAndModifyOptions.options()
                .upsert(true)
                .returnNew(true);
        mongoOperations.findAndModify(query, update, options, BatchStatistics.class);
    }

    @Override
    public void saveNbNotifiedUsersScoredAgainInStatistics(Instant date, Long nbNotifiedUsersScoredAgain) {
        Query query = new Query().addCriteria(Criteria.where("_id").is(date));
        Update update = new Update().set("nbNotifiedUsersScoredAgain", nbNotifiedUsersScoredAgain);
        FindAndModifyOptions options = FindAndModifyOptions.options()
                .upsert(true)
                .returnNew(true);
        mongoOperations.findAndModify(query, update, options, BatchStatistics.class);
    }

    @Override
    public void saveNbExposedButNotAtRiskUsersInStatistics(Instant date, Long nbExposedButNotAtRiskUsers) {
        Query query = new Query().addCriteria(Criteria.where("_id").is(date));
        Update update = new Update().set("nbExposedButNotAtRiskUsers", nbExposedButNotAtRiskUsers);
        FindAndModifyOptions options = FindAndModifyOptions.options()
                .upsert(true)
                .returnNew(true);
        mongoOperations.findAndModify(query, update, options, BatchStatistics.class);
    }

}
