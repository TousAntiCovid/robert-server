package fr.gouv.stopc.robert.server.batch.configuration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.partition.PartitionHandler;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.core.partition.support.TaskExecutorPartitionHandler;
import org.springframework.batch.item.data.MongoItemReader;
import org.springframework.batch.item.data.MongoItemWriter;
import org.springframework.batch.item.data.builder.MongoItemWriterBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import fr.gouv.stopc.robert.server.batch.partitioner.RangePartitioner;
import fr.gouv.stopc.robert.server.batch.utils.MongoItemReaderFactory;
import fr.gouv.stopc.robert.server.batch.utils.PropertyLoader;
import fr.gouv.stopc.robert.server.common.service.IServerConfigurationService;
import fr.gouv.stopc.robertserver.database.model.Contact;
import fr.gouv.stopc.robertserver.database.model.ItemIdMapping;
import fr.gouv.stopc.robertserver.database.model.Registration;
import fr.gouv.stopc.robertserver.database.service.ItemIdMappingService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class StepConfigurationBase {
    public static final int CHUNK_SIZE = 10000;
    public static final int GRID_SIZE = 10;
    public static final int POPULATE_STEP_CHUNK_SIZE = 200000;
    
    protected final PropertyLoader propertyLoader;
    protected final StepBuilderFactory stepBuilderFactory;
    protected final IServerConfigurationService serverConfigurationService;
    protected final ItemIdMappingService itemIdMappingService;
    protected final MongoItemReaderFactory<Registration> registrationMongoItemReaderFactory;
    protected final MongoItemReaderFactory<Contact> contactMongoItemReaderFactory;

    public StepConfigurationBase(PropertyLoader propertyLoader, StepBuilderFactory stepBuilderFactory,
            IServerConfigurationService serverConfigurationService, ItemIdMappingService itemIdMappingService) {
        super();
        this.propertyLoader = propertyLoader;
        this.stepBuilderFactory = stepBuilderFactory;
        this.serverConfigurationService = serverConfigurationService;
        this.itemIdMappingService = itemIdMappingService;
        this.registrationMongoItemReaderFactory = new MongoItemReaderFactory<>(Registration.class);
        this.contactMongoItemReaderFactory = new MongoItemReaderFactory<>(Contact.class);
    }

    protected Map<String, Sort.Direction> getSorts() {
        Map<String, Sort.Direction> sorts = new HashMap<>();
        sorts.put("_id", Direction.DESC);

        return sorts;
    }
    
    protected Partitioner partitioner() {
        return new RangePartitioner();
    }

    public PartitionHandler partitionHandler(Step workerStep, TaskExecutor taskExecutor) {
        TaskExecutorPartitionHandler handler = new TaskExecutorPartitionHandler();
        handler.setGridSize(GRID_SIZE);
        handler.setTaskExecutor(taskExecutor);
        handler.setStep(workerStep);

        try {
            handler.afterPropertiesSet();
        } catch (Exception e) {
            log.error(e.getMessage());
        }

        return handler;
    }

    public TaskExecutor syncTaskExecutor() {
        return new SyncTaskExecutor();
    }

    public TaskExecutor asyncTaskExecutor() {
        return new SimpleAsyncTaskExecutor();
    }

    @Bean
    @StepScope
    public MongoItemReader<Registration> mongoRegistrationItemReader(MongoTemplate mongoTemplate,
                                                                     @Value("#{stepExecutionContext[name]}") final String name,
                                                                     @Value("#{stepExecutionContext[start]}") final int start,
                                                                     @Value("#{stepExecutionContext[end]}") final int end) {
        log.info("{} currently reading Registration(s) from itemId collections from id={} - to id= {} ", name, start, end);

        List<byte[]> itemIdentifiers = (List<byte[]>) itemIdMappingService.getItemIdMappingsBetweenIds(start, end);

        Query query = new Query();
        query.addCriteria(Criteria.where("_id").in(itemIdentifiers));
        return registrationMongoItemReaderFactory.getMongoItemReader(mongoTemplate, query, CHUNK_SIZE);
    }
    
    @Bean
    @StepScope
    public MongoItemReader<Contact> mongoContactItemReader(MongoTemplate mongoTemplate, @Value("#{stepExecutionContext[name]}") final String name, @Value("#{stepExecutionContext[start]}") final int start, @Value("#{stepExecutionContext[end]}") final int end) {
        log.info("{} currently reading Contact(s) from itemId collections from id={} - to id= {} ", name, start, end);
    
        List<String> itemIdentifiers = (List<String>) this.itemIdMappingService.getItemIdMappingsBetweenIds(start, end);
    
        Query query = new Query();
        query.addCriteria(Criteria.where("_id").in(itemIdentifiers));
        return contactMongoItemReaderFactory.getMongoItemReader(mongoTemplate, query, CHUNK_SIZE);
    }

    @Bean
    public MongoItemWriter<ItemIdMapping> mongoRegistrationIdMappingItemWriter(MongoTemplate mongoTemplate) {
        return new MongoItemWriterBuilder<ItemIdMapping>()
                .template(mongoTemplate)
                .collection("itemIdMapping")
                .build();
    }
}
