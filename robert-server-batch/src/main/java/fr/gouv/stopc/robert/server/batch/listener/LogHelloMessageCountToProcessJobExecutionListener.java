package fr.gouv.stopc.robert.server.batch.listener;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Component;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.group;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.newAggregation;

@Component
@Slf4j
@RequiredArgsConstructor
public class LogHelloMessageCountToProcessJobExecutionListener implements JobExecutionListener {

    private final MongoOperations mongoOperations;

    @Override
    public void beforeJob(JobExecution jobExecution) {
        log.info("{} hello messages waiting for process",  getHelloMessageCount());
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        log.info("{} hello messages remaining after process",  getHelloMessageCount());
    }

    private long getHelloMessageCount() {
        final var unwind = Aggregation.unwind("$messageDetails");
        final var count = Aggregation.count().as("total");
        final var aggregation = Aggregation.newAggregation(unwind, count);
        final var result = mongoOperations.aggregate(aggregation,"CONTACTS_TO_PROCESS", HelloMessageCount.class);
        final var helloMessageCount = result.getUniqueMappedResult() == null ? 0 : result.getUniqueMappedResult().getTotal();
        return helloMessageCount;
    }

    @Data
    public static class HelloMessageCount {
        private long total;
    }
}
