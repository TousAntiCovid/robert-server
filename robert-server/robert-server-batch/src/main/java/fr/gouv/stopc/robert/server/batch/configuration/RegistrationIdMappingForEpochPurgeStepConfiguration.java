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
import fr.gouv.stopc.robert.server.common.utils.TimeUtils;
import fr.gouv.stopc.robertserver.database.model.ItemIdMapping;
import fr.gouv.stopc.robertserver.database.model.Registration;
import fr.gouv.stopc.robertserver.database.service.ItemIdMappingService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class RegistrationIdMappingForEpochPurgeStepConfiguration extends StepConfigurationBase {
    public RegistrationIdMappingForEpochPurgeStepConfiguration(PropertyLoader propertyLoader,
                                                               StepBuilderFactory stepBuilderFactory,
                                                               IServerConfigurationService serverConfigurationService,
                                                               ItemIdMappingService itemIdMappingService) {
        super(propertyLoader, stepBuilderFactory, serverConfigurationService, itemIdMappingService);
    }

    @Bean
    public Step populateRegistrationIdMappingForEpochPurgeStep(
            MongoItemReader<Registration> mongoRegistrationIdMappingForPurgeItemReader,
            MongoItemWriter<ItemIdMapping> mongoRegistrationIdMappingItemWriter) {

        return this.stepBuilderFactory.get("populateRegistrationIdMappingForPurge")
                .<Registration, ItemIdMapping>chunk(POPULATE_STEP_CHUNK_SIZE)
                .reader(mongoRegistrationIdMappingForPurgeItemReader)
                .processor(new RegistrationIdMappingProcessor())
                .writer(mongoRegistrationIdMappingItemWriter)
                .listener(this.registrationIdMappingStepListener())
                .build();
    }
    
    @Bean
    public MongoItemReader<Registration> mongoRegistrationIdMappingForPurgeItemReader(MongoTemplate mongoTemplate) {
        int currentEpochId = TimeUtils.getCurrentEpochFrom(serverConfigurationService.getServiceTimeStart());
        int contagiousPeriod = this.propertyLoader.getContagiousPeriod();
        int minEpochId = currentEpochId - contagiousPeriod * 96;
        String query = "{exposedEpochs:{$elemMatch:{epochId:{$lte:"+minEpochId+"}}}}}";

        MongoItemReader<Registration> reader = new MongoItemReader<>();

        reader.setTemplate(mongoTemplate);
        reader.setPageSize(CHUNK_SIZE);
        reader.setSort(this.getSorts());
        reader.setTargetType(Registration.class);
        reader.setQuery(query);

        return reader;
    }
    
    public StepExecutionListener registrationIdMappingStepListener() {
        return new StepExecutionListener() {
            @Override
            public void beforeStep(StepExecution stepExecution) {
                resetItemIdMappingCollection();
                log.debug("START : Registration id mapping for old epoch expositions purge.");
            }

            @Override
            public ExitStatus afterStep(StepExecution stepExecution) {
                log.debug("END : Registration id mapping for old epoch expositions purge.");
                return stepExecution.getExitStatus();
            }
            
            protected void resetItemIdMappingCollection() {
                log.info("START : Reset the itemIdMapping collection.");
                itemIdMappingService.deleteAll();
                log.info("END : Reset the itemIdMapping collection.");
            }
        };
    }
}
