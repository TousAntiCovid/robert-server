package fr.gouv.stopc.robert.server.batch.configuration;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.item.data.MongoItemReader;
import org.springframework.batch.item.data.MongoItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;

import fr.gouv.stopc.robert.server.batch.processor.RegistrationIdMappingProcessor;
import fr.gouv.stopc.robert.server.batch.utils.PropertyLoader;
import fr.gouv.stopc.robert.server.common.service.IServerConfigurationService;
import fr.gouv.stopc.robertserver.database.model.ItemIdMapping;
import fr.gouv.stopc.robertserver.database.model.Registration;
import fr.gouv.stopc.robertserver.database.service.ItemIdMappingService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class ScoredRegistrationIdMappingStepConfiguration extends StepConfigurationBase {

    public ScoredRegistrationIdMappingStepConfiguration(PropertyLoader propertyLoader,
            StepBuilderFactory stepBuilderFactory, IServerConfigurationService serverConfigurationService,
            ItemIdMappingService itemIdMappingService) {
        super(propertyLoader, stepBuilderFactory, serverConfigurationService, itemIdMappingService);
    }

    @Bean
    public Step populateIdMappingWithScoredRegistrationStep(
            MongoItemReader<Registration> scoredRegistrationReader,
            MongoItemWriter<ItemIdMapping> mongoRegistrationIdMappingItemWriter) {
        return this.stepBuilderFactory.get("populateIdMappingWithScoredRegistrationStep")
                .<Registration, ItemIdMapping>chunk(POPULATE_STEP_CHUNK_SIZE)
                .reader(scoredRegistrationReader)
                .processor(new RegistrationIdMappingProcessor())
                .writer(mongoRegistrationIdMappingItemWriter)
                .listener(this.scoredRegistrationIdMappingStepListener())
                .build();
    }
    
    @Bean
    public MongoItemReader<Registration> scoredRegistrationReader(MongoTemplate mongoTemplate) {
        return this.registrationReader(mongoTemplate, "{exposedEpochs: {$ne: []}}");
    }
    
    private MongoItemReader<Registration> registrationReader(MongoTemplate mongoTemplate, String query) {
        MongoItemReader<Registration> reader = new MongoItemReader<>();

        reader.setTemplate(mongoTemplate);
        reader.setPageSize(CHUNK_SIZE);
        reader.setSort(this.getSorts());
        reader.setTargetType(Registration.class);
        reader.setQuery(query);

        return reader;
    }
    
    public StepExecutionListener scoredRegistrationIdMappingStepListener() {
        return new StepExecutionListener() {
            @Override
            public void beforeStep(StepExecution stepExecution) {
                resetItemIdMappingCollection();
                log.info("START : Scored registration id mapping.");
            }

            @Override
            public ExitStatus afterStep(StepExecution stepExecution) {
                log.info("END : Scored registration id mapping.");
                return null;
            }
            
            protected void resetItemIdMappingCollection() {
                log.info("START : Reset the itemIdMapping collection.");
                itemIdMappingService.deleteAll();
                log.info("END : Reset the itemIdMapping collection.");
            }
        };
    }
}
