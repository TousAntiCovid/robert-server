package fr.gouv.stopc.robert.server.batch.service;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class HelloMessageService {

    private final MongoOperations mongoOperations;

    public long getHelloMessageCount() {
        final var unwind = Aggregation.unwind("$messageDetails");
        final var count = Aggregation.count().as("total");
        final var aggregation = Aggregation.newAggregation(unwind, count);
        final var result = mongoOperations.aggregate(aggregation, "CONTACTS_TO_PROCESS", HelloMessageCount.class);
        final var helloMessageCount = result.getUniqueMappedResult() == null ? 0
                : result.getUniqueMappedResult().getTotal();
        return helloMessageCount;
    }

    @Data
    public static class HelloMessageCount {

        private long total;
    }
}
