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

import fr.gouv.stopc.robert.server.batch.processor.PurgeOldEpochExpositionsProcessor;
import fr.gouv.stopc.robert.server.batch.service.impl.BatchRegistrationServiceImpl;
import fr.gouv.stopc.robert.server.batch.utils.PropertyLoader;
import fr.gouv.stopc.robert.server.batch.writer.RegistrationItemWriter;
import fr.gouv.stopc.robert.server.common.service.IServerConfigurationService;
import fr.gouv.stopc.robert.server.common.utils.TimeUtils;
import fr.gouv.stopc.robertserver.database.model.Registration;
import fr.gouv.stopc.robertserver.database.service.IRegistrationService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class PurgeOldEpochExpositionsStepConfiguration extends StepConfigurationBase {
    public static final String TOTAL_REGISTRATION_FOR_PURGE_COUNT_KEY = "totalRegistrationForPurgeCount";
    
    private final IRegistrationService registrationService;
    private final BatchRegistrationServiceImpl batchRegistrationService;
    
    public PurgeOldEpochExpositionsStepConfiguration(PropertyLoader propertyLoader,
                                                     StepBuilderFactory stepBuilderFactory, IServerConfigurationService serverConfigurationService,
                                                     IRegistrationService registrationService, BatchRegistrationServiceImpl batchRegistrationService) {
        super(propertyLoader, stepBuilderFactory, serverConfigurationService, null);
        this.registrationService = registrationService;
        this.batchRegistrationService = batchRegistrationService;
    }

    @Bean
    public Step purgeOldEpochExpositionsStep(Step purgeOldExpochExpositionsWorkerStep) {
        return this.stepBuilderFactory.get(PURGE_OLD_EXPOSITIONS_STEP_NAME)
                .listener(this.purgeStepListener())
                .partitioner(PURGE_OLD_EXPOSITIONS_PARTITIONER_STEP_NAME, this.partitioner())
                .partitionHandler(this.partitionHandler(purgeOldExpochExpositionsWorkerStep, this.asyncTaskExecutor()))
                .build();
    }

    @Bean
    public Step purgeOldExpochExpositionsWorkerStep(MongoItemReader<Registration> mongoRegistrationItemReader,
            ItemWriter<Registration> registrationItemWriterForPurge) {
        return this.stepBuilderFactory.get(PURGE_OLD_EXPOSITIONS_WORKER_STEP_NAME)
                .<Registration, Registration>chunk(CHUNK_SIZE)
                .reader(mongoRegistrationItemReader)
                .processor(this.purgeOldExpositionsProcessor())
                .writer(registrationItemWriterForPurge)
                .build();
    }
    
    public ItemProcessor<Registration, Registration> purgeOldExpositionsProcessor() {
        return new PurgeOldEpochExpositionsProcessor(
                this.serverConfigurationService,
                this.propertyLoader,
                this.batchRegistrationService);
    }

    @Bean
    public ItemWriter<Registration> registrationItemWriterForPurge() {
        return new RegistrationItemWriter(this.registrationService, TOTAL_REGISTRATION_FOR_PURGE_COUNT_KEY);
    }
    
    public StepExecutionListener purgeStepListener() {
        return new StepExecutionListener() {
            @Override
            public void beforeStep(StepExecution stepExecution) {
                log.debug("START : Purge Old Epoch Expositions.");

                long totalItemCount = registrationService.countNbUsersWithOldEpochExpositions(computeMinOldEpochId()).longValue();
                stepExecution.getJobExecution().getExecutionContext().putLong(TOTAL_REGISTRATION_FOR_PURGE_COUNT_KEY, totalItemCount);
            }

            @Override
            public ExitStatus afterStep(StepExecution stepExecution) {
                log.info("END : Purge Old Epoch Expositions.");
                return stepExecution.getExitStatus();
            }
            
            private int computeMinOldEpochId(){
                int currentEpochId = TimeUtils.getCurrentEpochFrom(serverConfigurationService.getServiceTimeStart());
                int contagiousPeriod = propertyLoader.getContagiousPeriod();
                int minEpochId = currentEpochId - contagiousPeriod * 96;

                log.debug("Min EpochId for the purge of old epoch expositions : {}", minEpochId);

                return minEpochId;
            }
        };
    }
}
