package fr.gouv.stopc.robert.server.batch.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

@Service
public class MetricsService {

    private final Counter robertBatchResetRiskLevelOfNeverNotifiedUser;

    private final Counter robertBatchResetRiskLevelOfUsersAlreadyNotified;

    private final Counter riskLevelReset;

    private final Counter totalRegistrationRiskEvaluationValued;

    private final Counter totalContactsToProcessValued;

    public MetricsService(final MeterRegistry meterRegistry) {

        robertBatchResetRiskLevelOfNeverNotifiedUser = Counter
                .builder("robert.batch.risk.reset")
                .tag("notified", "false")
                .description("The number of risk levels reseted of users never notified")
                .register(meterRegistry);

        robertBatchResetRiskLevelOfUsersAlreadyNotified = Counter
                .builder("robert.batch.risk.reset")
                .tag("notified", "true")
                .description("The number of risk levels reseted of users already notified")
                .register(meterRegistry);

        riskLevelReset = Counter.builder("robert.batch.risk.reset.step.count")
                .description("The number of risk level rested count")
                .register(meterRegistry);

        totalRegistrationRiskEvaluationValued = Counter
                .builder("robert.batch.total.registration.risk.evaluation.to.process.step.count")
                .description("The number of registration évaluation valued")
                .register(meterRegistry);

        totalContactsToProcessValued = Counter.builder("robert.batch.total.contacts.to.process.step.count")
                .description("The number of registration évaluation valued")
                .register(meterRegistry);
    }

    public void setRobertBatchRiskLevelReset(double amount) {
        riskLevelReset.increment(amount);
    }

    public void setTotalRegistrationRiskEvaluationValued(double amount) {
        totalRegistrationRiskEvaluationValued.increment(amount);
    }

    public void setTotalContactsToProcessValued(double amount) {
        totalContactsToProcessValued.increment(amount);
    }

    public void incrementResettingRiskLevelOfNeverNotifiedUserCounter() {
        robertBatchResetRiskLevelOfNeverNotifiedUser.increment();
    }

    public void incrementResettingRiskLevelOfUsersAlreadyNotifiedCounter() {
        robertBatchResetRiskLevelOfUsersAlreadyNotified.increment();
    }
}
