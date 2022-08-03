package fr.gouv.stopc.robert.server.batch.configuration.job;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Usual Robert Server batch run. I first purge old epoch expositions (to not
 * take them into account for risk evaluation), then I process all contacts to
 * compute the scores, endly I evaluate the risk for all registrations in the
 * database having a score.
 */
@Slf4j
@Configuration
@ConditionalOnProperty(value = "robert.scoring.batch-mode", havingValue = "SCORE_CONTACTS_AND_COMPUTE_RISK")
public class ScoringAndRiskEvaluationJobConfiguration {

    public static final String TOTAL_CONTACT_COUNT_KEY = "totalContactCount";

    private final JobBuilderFactory jobBuilderFactory;

    public ScoringAndRiskEvaluationJobConfiguration(JobBuilderFactory jobBuilderFactory) {
        this.jobBuilderFactory = jobBuilderFactory;
    }

    @Bean
    public Job scoreAndProcessRisks(Step contactProcessingStep, Step purgeOldEpochExpositionsStep,
            Step populateContactIdMappingStep, Step populateRegistrationIdMappingForEpochPurgeStep,
            Step populateIdMappingWithScoredRegistrationStep, Step processRegistrationRiskStep,
            Step populateIdMappingForRegistrationRiskResetStep, Step registrationRiskResetStep,
            JobExecutionListener logHelloMessageCountToProcessJobExecutionListener,
            Step saveKpisStep) {

        log.info("Building contact batch (Old expositions purge, Contact scoring, Risk computation)");
        return this.jobBuilderFactory.get("SCORE_CONTACTS_AND_COMPUTE_RISK")
                .listener(logHelloMessageCountToProcessJobExecutionListener)
                .start(populateRegistrationIdMappingForEpochPurgeStep)
                .next(purgeOldEpochExpositionsStep)
                .next(populateIdMappingForRegistrationRiskResetStep)
                .next(registrationRiskResetStep)
                .next(populateContactIdMappingStep)
                .next(contactProcessingStep)
                .next(populateIdMappingWithScoredRegistrationStep)
                .next(processRegistrationRiskStep)
                .next(saveKpisStep)
                .build();
    }
}
