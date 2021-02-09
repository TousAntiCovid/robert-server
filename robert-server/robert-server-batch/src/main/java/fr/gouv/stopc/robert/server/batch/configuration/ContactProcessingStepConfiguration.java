package fr.gouv.stopc.robert.server.batch.configuration;

import static fr.gouv.stopc.robert.server.batch.utils.StepNameUtils.*;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.data.MongoItemReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import fr.gouv.stopc.robert.crypto.grpc.server.client.service.ICryptoServerGrpcClient;
import fr.gouv.stopc.robert.server.batch.processor.ContactProcessor;
import fr.gouv.stopc.robert.server.batch.service.ScoringStrategyService;
import fr.gouv.stopc.robert.server.batch.utils.PropertyLoader;
import fr.gouv.stopc.robert.server.batch.writer.ContactItemWriter;
import fr.gouv.stopc.robert.server.common.service.IServerConfigurationService;
import fr.gouv.stopc.robertserver.database.model.Contact;
import fr.gouv.stopc.robertserver.database.service.ContactService;
import fr.gouv.stopc.robertserver.database.service.IRegistrationService;
import fr.gouv.stopc.robertserver.database.service.ItemIdMappingService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class ContactProcessingStepConfiguration extends StepConfigurationBase {

    private final IRegistrationService registrationService;
    private final ContactService contactService;
    private final ScoringStrategyService scoringStrategyService;
    private final ICryptoServerGrpcClient cryptoServerClient;
    
    public ContactProcessingStepConfiguration(PropertyLoader propertyLoader, StepBuilderFactory stepBuilderFactory,
            IServerConfigurationService serverConfigurationService, IRegistrationService registrationService,
            ContactService contactService, ICryptoServerGrpcClient cryptoServerClient,
            ScoringStrategyService scoringStrategyService,
            ItemIdMappingService itemIdMappingService) {
        super(propertyLoader, stepBuilderFactory, serverConfigurationService, itemIdMappingService);
        this.registrationService = registrationService;
        this.contactService = contactService;
        this.scoringStrategyService = scoringStrategyService;
        this.cryptoServerClient = cryptoServerClient;
    }

    @Bean
    public Step contactProcessingStep(MongoItemReader<Contact> mongoContactItemReader) {
        Step workerStep = this.contactWorkerStep(stepBuilderFactory, mongoContactItemReader);

        return this.stepBuilderFactory.get(SCORING_CONTACT_STEP_NAME)
                .listener(this.contactScoringStepListener())
                .partitioner(SCORING_CONTACT_PARTITIONER_STEP_NAME, partitioner())
                .partitionHandler(partitionHandler(workerStep, syncTaskExecutor()))
                .build();
    }

    public Step contactWorkerStep(StepBuilderFactory stepBuilderFactory, MongoItemReader<Contact> mongoContactItemReader) {
        return stepBuilderFactory.get(SCORING_CONTACT_WORKER_STEP_NAME)
                .<Contact, Contact>chunk(CHUNK_SIZE)
                .reader(mongoContactItemReader)
                .processor(this.contactsProcessor())
                .writer(this.mongoContactItemWriter())
                .build();
    }

    public ItemProcessor<Contact, Contact> contactsProcessor() {
        return new ContactProcessor(
                this.serverConfigurationService,
                this.registrationService,
                this.cryptoServerClient,
                this.scoringStrategyService,
                this.propertyLoader) {
        };
    }
    
    public ItemWriter<Contact> mongoContactItemWriter() {
        return new ContactItemWriter(this.contactService);
    }
    
    public StepExecutionListener contactScoringStepListener() {
        return new StepExecutionListener() {
            @Override
            public void beforeStep(StepExecution stepExecution) {
                log.info("START : Contact scoring.");

                long totalItemCount = contactService.count().longValue();
                stepExecution.getJobExecution().getExecutionContext().putLong(ScoringAndRiskEvaluationJobConfiguration.TOTAL_CONTACT_COUNT_KEY, totalItemCount);
            }

            @Override
            public ExitStatus afterStep(StepExecution stepExecution) {
                log.info("END : Contact scoring.");
                return stepExecution.getExitStatus();
            }
        };
    }
}
