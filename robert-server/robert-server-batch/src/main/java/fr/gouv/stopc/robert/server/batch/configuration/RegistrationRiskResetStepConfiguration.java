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
import fr.gouv.stopc.robert.server.batch.utils.StepNameUtils;
import fr.gouv.stopc.robert.server.batch.writer.RegistrationItemWriter;
import fr.gouv.stopc.robert.server.common.service.IServerConfigurationService;
import fr.gouv.stopc.robertserver.database.model.Registration;
import fr.gouv.stopc.robertserver.database.service.IRegistrationService;
import fr.gouv.stopc.robertserver.database.service.ItemIdMappingService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class RegistrationRiskResetStepConfiguration extends StepConfigurationBase {
    public static final String TOTAL_REGISTRATION_FOR_RISK_LEVEL_RESET_COUNT_KEY = "totalRegistrationForRiskLevelResetCount";
    
    private final IRegistrationService registrationService;
    
    public RegistrationRiskResetStepConfiguration(PropertyLoader propertyLoader, StepBuilderFactory stepBuilderFactory,
            IServerConfigurationService serverConfigurationService, IRegistrationService registrationService,
            BatchRegistrationServiceImpl batchRegistrationService, ItemIdMappingService itemIdMappingService) {
        super(propertyLoader, stepBuilderFactory, serverConfigurationService, itemIdMappingService);
        this.registrationService = registrationService;
    }

    @Bean
    public Step registrationRiskResetStep(Step registrationRiskResetWorkerStep) {
        return this.stepBuilderFactory.get(StepNameUtils.REGISTRATION_RISK_RESET_STEP_NAME)
                .listener(this.registrationRiskResetStepListener())
                .partitioner(StepNameUtils.REGISTRATION_RISK_RESET_PARTITIONER_STEP_NAME, this.partitioner())
                .partitionHandler(this.partitionHandler(registrationRiskResetWorkerStep, this.asyncTaskExecutor()))
                .build();
    }

    @Bean
    public Step registrationRiskResetWorkerStep(MongoItemReader<Registration> mongoRegistrationItemReader,
            ItemProcessor<Registration, Registration> registrationRiskResetProcessor,
            ItemWriter<Registration> registrationItemWriterForRiskReset) {
        return this.stepBuilderFactory.get(StepNameUtils.REGISTRATION_RISK_RESET_WORKER_STEP_NAME)
                .<Registration, Registration>chunk(CHUNK_SIZE)
                .reader(mongoRegistrationItemReader)
                .processor(registrationRiskResetProcessor)
                .writer(registrationItemWriterForRiskReset)
                .build();
    }

    @Bean
    public ItemProcessor<Registration, Registration> registrationRiskResetProcessor() {
        return new RegistrationRiskLevelResetProcessor(this.propertyLoader);
    }
    
    @Bean
    public ItemWriter<Registration> registrationItemWriterForRiskReset() {
        return new RegistrationItemWriter(this.registrationService, TOTAL_REGISTRATION_FOR_RISK_LEVEL_RESET_COUNT_KEY);
    }
    
    public StepExecutionListener registrationRiskResetStepListener() {
        return new StepExecutionListener() {
            @Override
            public void beforeStep(StepExecution stepExecution) {
                log.debug("START : Reset risk level of registrations when retention time > {}.", propertyLoader.getRiskLevelRetentionPeriodInDays());

                long totalItemCount = registrationService.countNbUsersAtRisk().longValue();
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
