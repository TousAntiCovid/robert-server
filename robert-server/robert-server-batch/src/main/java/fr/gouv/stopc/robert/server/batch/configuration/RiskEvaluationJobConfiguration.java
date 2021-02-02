package fr.gouv.stopc.robert.server.batch.configuration;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.extern.slf4j.Slf4j;

/**
 * Only evaluate the risk of all registrations present in the database.
 * WARNING: old expositions MUST be purged before risk evaluation. If not,
 * old expositions scores will taken into account for risk evaluation. 
 */
@Slf4j
@Configuration
@ConditionalOnProperty(
        value="robert.scoring.batch-mode",
        havingValue = "FULL_REGISTRATION_SCAN_COMPUTE_RISK")
public class RiskEvaluationJobConfiguration {

    public static final String TOTAL_REGISTRATION_COUNT_KEY = "totalRegistrationCount";
    
    private final JobBuilderFactory jobBuilderFactory;
    
    public RiskEvaluationJobConfiguration(JobBuilderFactory jobBuilderFactory) {
        this.jobBuilderFactory = jobBuilderFactory;
    }
    
    @Bean
    public Job evaluateRiskJob(Step populateIdMappingWithScoredRegistrationStep, Step processRegistrationRiskStep) {
        log.info("Launching registration batch (No contact scoring, only risk computation)");
        return this.jobBuilderFactory.get("processRegistration")
                .start(populateIdMappingWithScoredRegistrationStep)
                .next(processRegistrationRiskStep)
                .build();
    }
}
