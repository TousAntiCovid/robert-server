package fr.gouv.stopc.robert.server.batch.configuration;

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

import fr.gouv.stopc.robert.server.batch.processor.RegistrationRiskLevelResetProcessor;
import fr.gouv.stopc.robert.server.batch.service.impl.BatchRegistrationServiceImpl;
import fr.gouv.stopc.robert.server.batch.utils.PropertyLoader;
import fr.gouv.stopc.robert.server.batch.writer.RegistrationItemWriter;
import fr.gouv.stopc.robert.server.common.service.IServerConfigurationService;
import fr.gouv.stopc.robertserver.database.model.Registration;
import fr.gouv.stopc.robertserver.database.service.IRegistrationService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class RegistrationRiskLevelResetStepConfiguration extends StepConfigurationBase {
    public static final String TOTAL_REGISTRATION_FOR_RISK_LEVEL_RESET_COUNT_KEY = "totalRegistrationForRiskLevelResetCount";
    
    private final IRegistrationService registrationService;
    
    public RegistrationRiskLevelResetStepConfiguration(PropertyLoader propertyLoader,
                                                     StepBuilderFactory stepBuilderFactory, IServerConfigurationService serverConfigurationService,
                                                     IRegistrationService registrationService, BatchRegistrationServiceImpl batchRegistrationService) {
        super(propertyLoader, stepBuilderFactory, serverConfigurationService, null);
        this.registrationService = registrationService;
    }

    @Bean
    public Step registrationRiskLevelResetStep(Step registrationRiskLevelResetWorkerStep) {
        return this.stepBuilderFactory.get("registrationRiskLevelResetStep")
                .listener(this.registrationRiskLevelResetStepListener())
                .partitioner("registrationRiskLevelResetPartitioner", this.partitioner())
                .partitionHandler(this.partitionHandler(registrationRiskLevelResetWorkerStep, this.asyncTaskExecutor()))
                .build();
    }

    @Bean
    public Step registrationRiskLevelResetWorkerStep(MongoItemReader<Registration> mongoRegistrationItemReader,
            ItemProcessor<Registration, Registration> registrationRiskLevelResetProcessor,
            ItemWriter<Registration> registrationItemWriterForRiskLevelReset) {
        return this.stepBuilderFactory.get("registrationRiskLevelResetWorkerStep")
                .<Registration, Registration>chunk(CHUNK_SIZE)
                .reader(mongoRegistrationItemReader)
                .processor(registrationRiskLevelResetProcessor)
                .writer(registrationItemWriterForRiskLevelReset)
                .build();
    }

    @Bean
    public ItemProcessor<Registration, Registration> registrationRiskLevelResetProcessor() {
        return new RegistrationRiskLevelResetProcessor(this.propertyLoader);
    }
    
    @Bean
    public ItemWriter<Registration> registrationItemWriterForRiskLevelReset() {
        return new RegistrationItemWriter(this.registrationService, TOTAL_REGISTRATION_FOR_RISK_LEVEL_RESET_COUNT_KEY);
    }
    
    public StepExecutionListener registrationRiskLevelResetStepListener() {
        return new StepExecutionListener() {
            @Override
            public void beforeStep(StepExecution stepExecution) {
                log.debug("START : Reset risk level of registrations when retention time > {}.", propertyLoader.getRiskLevelRetentionPeriod());

                long totalItemCount = registrationService.countNbUsersAtRiskAndNotified().longValue();
                stepExecution.getJobExecution().getExecutionContext().putLong(TOTAL_REGISTRATION_FOR_RISK_LEVEL_RESET_COUNT_KEY, totalItemCount);
            }

            @Override
            public ExitStatus afterStep(StepExecution stepExecution) {
                log.info("END : Reset risk level of registrations.");
                return stepExecution.getExitStatus();
            }
        };
    }
}
