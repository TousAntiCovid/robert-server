package fr.gouv.stopc.robert.server.batch.reader;

import org.springframework.batch.item.data.MongoItemReader;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.util.Map;

public class MongoItemReaderFactory<T> {

    private final Class<T> type;

    public MongoItemReaderFactory(Class<T> type) {
        this.type = type;
    }

    public MongoItemReader<T> getMongoItemReader(MongoTemplate mongoTemplate,
            Query query,
            int pageSize) {
        MongoItemReader<T> reader = new MongoItemReader<>();
        reader.setTemplate(mongoTemplate);
        reader.setTargetType(type);
        reader.setQuery(query);
        reader.setPageSize(pageSize);

        return reader;
    }

    public MongoItemReader<T> getMongoItemReader(MongoTemplate mongoTemplate,
            String queryAsString,
            Map<String, Sort.Direction> sorts,
            int pageSize) {
        MongoItemReader<T> reader = new MongoItemReader<>();
        reader.setTemplate(mongoTemplate);
        reader.setTargetType(type);
        reader.setQuery(queryAsString);
        if (sorts != null) {
            reader.setSort(sorts);
        }
        reader.setPageSize(pageSize);

        return reader;
    }

}
