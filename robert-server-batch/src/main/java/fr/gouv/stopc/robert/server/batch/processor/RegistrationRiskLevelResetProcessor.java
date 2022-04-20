package fr.gouv.stopc.robert.server.batch.processor;

import fr.gouv.stopc.robert.server.batch.service.BatchStatisticsService;
import fr.gouv.stopc.robert.server.batch.utils.PropertyLoader;
import fr.gouv.stopc.robert.server.common.service.RobertClock;
import fr.gouv.stopc.robertserver.database.model.Registration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ItemProcessor;

import java.time.Instant;
import java.util.Optional;

import static java.time.temporal.ChronoUnit.DAYS;

@Slf4j
@RequiredArgsConstructor
public class RegistrationRiskLevelResetProcessor implements ItemProcessor<Registration, Registration> {

    private final PropertyLoader propertyLoader;

    private final RobertClock robertClock;

    private final BatchStatisticsService batchStatisticsService;

    private Instant batchExecutionInstant;

    @BeforeStep
    void retrieveInterStepData(final StepExecution stepExecution) {
        batchExecutionInstant = stepExecution
                .getJobExecution()
                .getStartTime()
                .toInstant();
    }

    @Override
    public Registration process(final Registration registration) throws Exception {

        if (isAtRiskAndRetentionPeriodHasExpired(registration)) {
            return resetRiskAndSaveStatistics(registration, Optional.ofNullable(batchExecutionInstant));
        }
        return null;
    }

    private boolean riskRetentionThresholdHasExpired(final Registration registration) {
        final var today = robertClock.now().truncatedTo(DAYS);
        final var lastContactTime = robertClock.atNtpTimestamp(registration.getLastContactTimestamp());
        return lastContactTime.until(today).toDays() > propertyLoader.getRiskLevelRetentionPeriodInDays();
    }

    private boolean isAtRiskAndRetentionPeriodHasExpired(final Registration registration) {
        return registration.isAtRisk() && riskRetentionThresholdHasExpired(registration);
    }

    public Registration resetRiskAndSaveStatistics(final Registration registration,
            final Optional<Instant> batchExecutionInstant) {

        if (!registration.isNotified()) {
            log.info("Resetting risk level of a user never notified!");
        }
        registration.setAtRisk(false);
        registration.setNotifiedForCurrentRisk(false);
        batchExecutionInstant.ifPresentOrElse(
                batchStatisticsService::incrementUsersAboveRiskThresholdButRetentionPeriodExpired,
                () -> batchStatisticsService.incrementUsersAboveRiskThresholdButRetentionPeriodExpired(Instant.now())
        );
        return registration;
    }

}
