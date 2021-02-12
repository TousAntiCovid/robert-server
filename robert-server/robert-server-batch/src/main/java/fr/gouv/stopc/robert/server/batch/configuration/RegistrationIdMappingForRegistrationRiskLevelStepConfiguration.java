package fr.gouv.stopc.robert.server.batch.configuration;

import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.item.data.MongoItemReader;
import org.springframework.batch.item.data.MongoItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;

import fr.gouv.stopc.robert.server.batch.listener.ResetIdMappingTableListener;
import fr.gouv.stopc.robert.server.batch.processor.RegistrationIdMappingProcessor;
import fr.gouv.stopc.robert.server.batch.utils.PropertyLoader;
import fr.gouv.stopc.robert.server.common.service.IServerConfigurationService;
import fr.gouv.stopc.robertserver.database.model.ItemIdMapping;
import fr.gouv.stopc.robertserver.database.model.Registration;
import fr.gouv.stopc.robertserver.database.service.ItemIdMappingService;

@Configuration
public class RegistrationIdMappingForRegistrationRiskLevelStepConfiguration extends StepConfigurationBase {
    public RegistrationIdMappingForRegistrationRiskLevelStepConfiguration(PropertyLoader propertyLoader,
                                                               StepBuilderFactory stepBuilderFactory,
                                                               IServerConfigurationService serverConfigurationService,
                                                               ItemIdMappingService itemIdMappingService) {
        super(propertyLoader, stepBuilderFactory, serverConfigurationService, itemIdMappingService);
    }

    @Bean
    public Step populateIdMappingForRegistrationRiskResetStep(
            MongoItemReader<Registration> registrationIdMappingForRiskResetReader,
            MongoItemWriter<ItemIdMapping> mongoRegistrationIdMappingItemWriter) {

        return this.stepBuilderFactory.get("populateIdMappingWithRegistrationForRiskLevelReset")
                .<Registration, ItemIdMapping>chunk(POPULATE_STEP_CHUNK_SIZE)
                .reader(registrationIdMappingForRiskResetReader)
                .processor(new RegistrationIdMappingProcessor())
                .writer(mongoRegistrationIdMappingItemWriter)
                .listener(this.registrationIdMappingStepListener())
                .build();
    }
    
    @Bean
    public MongoItemReader<Registration> registrationIdMappingForRiskResetReader(MongoTemplate mongoTemplate) {
        String queryString = "{atRisk: true}";

        return registrationMongoItemReaderFactory.getMongoItemReader(mongoTemplate,
                queryString,
                this.getSorts(),
                CHUNK_SIZE);
    }

    public StepExecutionListener registrationIdMappingStepListener() {
        return new ResetIdMappingTableListener("Registration id mapping for risk level reset", this.itemIdMappingService);
    }
}
