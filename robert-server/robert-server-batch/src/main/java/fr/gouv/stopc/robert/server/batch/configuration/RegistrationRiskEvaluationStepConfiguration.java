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

import fr.gouv.stopc.robert.server.batch.processor.RiskEvaluationProcessor;
import fr.gouv.stopc.robert.server.batch.service.ScoringStrategyService;
import fr.gouv.stopc.robert.server.batch.utils.PropertyLoader;
import fr.gouv.stopc.robert.server.batch.writer.RegistrationItemWriter;
import fr.gouv.stopc.robert.server.common.service.IServerConfigurationService;
import fr.gouv.stopc.robertserver.database.model.Registration;
import fr.gouv.stopc.robertserver.database.service.IRegistrationService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class RegistrationRiskEvaluationStepConfiguration extends StepConfigurationBase {

    private final ScoringStrategyService scoringStrategyService;
    private final IRegistrationService registrationService;

    public RegistrationRiskEvaluationStepConfiguration(PropertyLoader propertyLoader,
            StepBuilderFactory stepBuilderFactory, IServerConfigurationService serverConfigurationService,
            ScoringStrategyService scoringStrategyService, IRegistrationService registrationService) {
        super(propertyLoader, stepBuilderFactory, serverConfigurationService, null);
        this.scoringStrategyService = scoringStrategyService;
        this.registrationService = registrationService;
    }

    @Bean
    public Step processRegistrationRiskStep(Step processRegistrationRiskWorkerStep) {

        return this.stepBuilderFactory.get("processRegistrationRiskStep")
                .partitioner("processRegistrationRiskPartitioner", this.partitioner())
                .partitionHandler(this.partitionHandler(processRegistrationRiskWorkerStep, this.asyncTaskExecutor()))
                .listener(this.riskEvaluationStepListener())
                .build();
    }

    @Bean
    public Step processRegistrationRiskWorkerStep(MongoItemReader<Registration> mongoRegistrationItemReader,
            ItemProcessor<Registration, Registration> riskEvaluationProcessor,
            ItemWriter<Registration> registrationItemWriter) {
        return this.stepBuilderFactory.get("processRegistrationRiskWorkerStep")
                .<Registration, Registration>chunk(CHUNK_SIZE)
                .reader(mongoRegistrationItemReader)
                .processor(riskEvaluationProcessor)
                .writer(registrationItemWriter)
                .build();
    }

    @Bean
    public ItemProcessor<Registration, Registration> riskEvaluationProcessor() {
        return new RiskEvaluationProcessor(
                this.serverConfigurationService,
                this.scoringStrategyService,
                this.propertyLoader);
    }
    
    @Bean
    public ItemWriter<Registration> registrationItemWriter() {
        return new RegistrationItemWriter(this.registrationService, RiskEvaluationJobConfiguration.TOTAL_REGISTRATION_COUNT_KEY);
    }
    
    public StepExecutionListener riskEvaluationStepListener() {
        return new StepExecutionListener() {
            @Override
            public void beforeStep(StepExecution stepExecution) {
                log.info("START : Risk Evaluation");

                long totalItemCount = registrationService.count().longValue();
                stepExecution.getJobExecution().getExecutionContext().putLong(RiskEvaluationJobConfiguration.TOTAL_REGISTRATION_COUNT_KEY, totalItemCount);
            }

            @Override
            public ExitStatus afterStep(StepExecution stepExecution) {
                log.info("END : Risk Evaluation.");
                return stepExecution.getExitStatus();
            }
        };
    }
    
}
