package fr.gouv.stopc.robert.server.batch.configuration;

import fr.gouv.stopc.robert.server.batch.listener.ResetIdMappingTableListener;
import fr.gouv.stopc.robert.server.batch.processor.RegistrationIdMappingProcessor;
import fr.gouv.stopc.robert.server.batch.utils.PropertyLoader;
import fr.gouv.stopc.robert.server.common.service.IServerConfigurationService;
import fr.gouv.stopc.robertserver.database.model.ItemIdMapping;
import fr.gouv.stopc.robertserver.database.model.Registration;
import fr.gouv.stopc.robertserver.database.service.ItemIdMappingService;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.item.data.MongoItemReader;
import org.springframework.batch.item.data.MongoItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;

import static fr.gouv.stopc.robert.server.batch.utils.StepNameUtils.POPULATE_REGISTRATION_WITH_SCORE_STEP_NAME;

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
        return this.stepBuilderFactory.get(POPULATE_REGISTRATION_WITH_SCORE_STEP_NAME)
                .<Registration, ItemIdMapping>chunk(POPULATE_STEP_CHUNK_SIZE)
                .reader(scoredRegistrationReader)
                .processor(new RegistrationIdMappingProcessor())
                .writer(mongoRegistrationIdMappingItemWriter)
                .listener(this.scoredRegistrationIdMappingStepListener())
                .build();
    }

    @Bean
    public MongoItemReader<Registration> scoredRegistrationReader(MongoTemplate mongoTemplate) {

        String queryAsString = "{outdatedRisk: true}";
        return registrationMongoItemReaderFactory
                .getMongoItemReader(mongoTemplate, queryAsString, this.getSorts(), CHUNK_SIZE);
    }

    public StepExecutionListener scoredRegistrationIdMappingStepListener() {
        return new ResetIdMappingTableListener("Scored registration id mapping", this.itemIdMappingService);
    }
}
