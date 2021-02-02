package fr.gouv.stopc.robert.server.batch.utils;

import java.util.Map;

import org.springframework.batch.item.data.MongoItemReader;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

public class MongoItemReaderFactory<T> {

    private final Class<T> type;

    public MongoItemReaderFactory(Class<T> type) {
        this.type = type;
    }


    public MongoItemReader<T> buildMongoItemReader(MongoTemplate mongoTemplate,
                                                   Query query,
                                                   Map<String, Sort.Direction> sorts,
                                                   int pageSize){

        MongoItemReader<T> reader = new MongoItemReader<>();
        reader.setTemplate(mongoTemplate);
        reader.setTargetType(type);
        reader.setQuery(query);
        if (sorts != null) {
            reader.setSort(sorts);
        }
        reader.setPageSize(pageSize);

    return reader;
    }

    public MongoItemReader<T> buildMongoItemReader(MongoTemplate mongoTemplate,
                                                   String queryAsString,
                                                   Map<String, Sort.Direction> sorts,
                                                   int pageSize){

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
