package fr.gouv.stopc.robert.server.batch.configuration;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.data.MongoItemReader;
import org.springframework.batch.item.data.MongoItemWriter;
import org.springframework.batch.item.data.builder.MongoItemWriterBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;

import fr.gouv.stopc.robert.server.batch.processor.ContactIdMappingProcessor;
import fr.gouv.stopc.robert.server.batch.utils.PropertyLoader;
import fr.gouv.stopc.robert.server.common.service.IServerConfigurationService;
import fr.gouv.stopc.robertserver.database.model.Contact;
import fr.gouv.stopc.robertserver.database.model.ItemIdMapping;
import fr.gouv.stopc.robertserver.database.service.ItemIdMappingService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class ContactIdMappingStepConfiguration extends StepConfigurationBase {

    public ContactIdMappingStepConfiguration(PropertyLoader propertyLoader, StepBuilderFactory stepBuilderFactory,
            IServerConfigurationService serverConfigurationService, ItemIdMappingService itemIdMappingService) {
        super(propertyLoader, stepBuilderFactory, serverConfigurationService, itemIdMappingService);
    }

    @Bean
    public Step populateContactIdMappingStep(
            MongoItemReader<Contact> mongoContactIdMappingItemReader,
            MongoItemWriter<ItemIdMapping> mongoContactIdMappingItemWriter) {

        return this.stepBuilderFactory.get("populateContactIdMapping")
                .<Contact, ItemIdMapping>chunk(POPULATE_STEP_CHUNK_SIZE)
                .reader(mongoContactIdMappingItemReader)
                .processor(this.contactIdMappingProcessor())
                .writer(mongoContactIdMappingItemWriter)
                .listener(this.contactIdMappingStepListener())
                .build();
    }
    
    public ItemProcessor<Contact, ItemIdMapping<String>> contactIdMappingProcessor() {
        return new ContactIdMappingProcessor();
    }

    @Bean
    public MongoItemReader<Contact> mongoContactIdMappingItemReader(MongoTemplate mongoTemplate) {
        MongoItemReader<Contact> reader = new MongoItemReader<>();

        reader.setTemplate(mongoTemplate);
        reader.setPageSize(CHUNK_SIZE);
        reader.setSort(this.getSorts());
        reader.setTargetType(Contact.class);
        reader.setQuery("{}");

        return reader;
    }

    @Bean
    public MongoItemWriter<ItemIdMapping> mongoContactIdMappingItemWriter(MongoTemplate mongoTemplate) {
        return new MongoItemWriterBuilder<ItemIdMapping>()
                .template(mongoTemplate)
                .collection("itemIdMapping")
                .build();
    }
    
    public StepExecutionListener contactIdMappingStepListener() {
        return new StepExecutionListener() {
            @Override
            public void beforeStep(StepExecution stepExecution) {
                resetItemIdMappingCollection();
                log.info("START : Contact id mapping.");
            }

            @Override
            public ExitStatus afterStep(StepExecution stepExecution) {
                log.info("END : Contact registration id mapping.");
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
