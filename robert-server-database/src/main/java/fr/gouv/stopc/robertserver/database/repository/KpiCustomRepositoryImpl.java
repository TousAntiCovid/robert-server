package fr.gouv.stopc.robertserver.database.repository;

import fr.gouv.stopc.robertserver.database.model.Kpi;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

@RequiredArgsConstructor
public class KpiCustomRepositoryImpl implements KpiCustomRepository {

    private final MongoOperations mongoOperations;

    @Override
    public void incrementNotifiedUsers() {
        final var query = new Query().addCriteria(Criteria.where("name").is("notifiedUsers"));
        final var update = new Update().inc("value", 1);
        final var options = FindAndModifyOptions.options()
                .upsert(true)
                .returnNew(true);

        mongoOperations.findAndModify(query, update, options, Kpi.class);
    }

    @Override
    public void incrementReportsCount() {
        final var query = new Query().addCriteria(Criteria.where("name").is("reportsCount"));
        final var update = new Update().inc("value", 1);
        final var options = FindAndModifyOptions.options()
                .upsert(true)
                .returnNew(true);

        mongoOperations.findAndModify(query, update, options, Kpi.class);
    }

    @Override
    public void incrementUsersAboveRiskThresholdButRetentionPeriodExpired() {
        final var query = new Query()
                .addCriteria(Criteria.where("name").is("usersAboveRiskThresholdButRetentionPeriodExpired"));
        final var update = new Update().inc("value", 1);
        final var options = FindAndModifyOptions.options()
                .upsert(true)
                .returnNew(true);

        mongoOperations.findAndModify(query, update, options, Kpi.class);
    }

}
