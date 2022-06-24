package fr.gouv.stopc.robert.server.batch.task;

import fr.gouv.stopc.robert.server.batch.manager.MetricsManager;
import fr.gouv.stopc.robert.server.batch.scheduled.service.ReassessRiskLevelService;
import fr.gouv.stopc.robert.server.batch.service.MetricsService;
import fr.gouv.stopc.robert.server.common.service.RobertClock;
import fr.gouv.stopc.robertserver.database.model.Registration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;

import java.time.Instant;

import static fr.gouv.stopc.robert.server.batch.manager.MetricsManager.assertThatCounterMetricIncrement;
import static java.time.temporal.ChronoUnit.DAYS;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.test.context.TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS;

@TestExecutionListeners(listeners = {
        MetricsManager.class
}, mergeMode = MERGE_WITH_DEFAULTS)
@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles({ "legacy", "test" })
class ReassessRiskLevelServiceTest {

    @Autowired
    ReassessRiskLevelService reassessRiskLevel;

    @Autowired
    RobertClock robertClock;

    @Autowired
    MetricsService metricsService;

    @Test
    void registration_should_not_be_updated_when_not_at_risk_and_not_notified() {
        // Given
        var registration = Registration.builder().atRisk(false).isNotified(false).build();
        // When
        var processedRegistration = reassessRiskLevel.updateWhenAtRiskAndRetentionPeriodHasExpired(registration);
        // Then
        assertThat(processedRegistration).as("Registration is not null").isNull();
    }

    @Test
    void risk_level_should_not_be_reset_when_not_at_risk_and_notified() {
        // Given
        final var registration = Registration.builder().atRisk(false).isNotified(true).build();
        // When
        final var processedRegistration = reassessRiskLevel.updateWhenAtRiskAndRetentionPeriodHasExpired(registration);
        // Then
        assertThat(processedRegistration).as("Registration is not null").isNull();
    }

    @Test
    void risk_level_should_not_be_reset_when_at_risk_and_notified_but_last_contact_date_is_under_7_days_ago() {

        // Given
        final var nowMinus6DaysNtpTimestamp = robertClock.at(
                Instant.now()
                        .truncatedTo(DAYS)
                        .minus(7, DAYS)
        ).asNtpTimestamp();

        final var registration = Registration.builder()
                .atRisk(true)
                .isNotified(true)
                .latestRiskEpoch(4912)
                .lastContactTimestamp(nowMinus6DaysNtpTimestamp)
                .build();

        // When
        final var processedRegistration = reassessRiskLevel.updateWhenAtRiskAndRetentionPeriodHasExpired(registration);
        // Then
        assertThat(processedRegistration).isNull();
    }

    @Test
    void risk_level_should_be_reset_when_at_risk_and_notified_and_last_contact_date_is_above_7_days_ago() {
        // Given
        final var nowMinus8DaysNtpTimestamp = robertClock.at(
                Instant.now()
                        .truncatedTo(DAYS)
                        .minus(8, DAYS)
        ).asNtpTimestamp();

        final var registration = Registration.builder()
                .atRisk(true)
                .isNotified(true)
                .latestRiskEpoch(4808)
                .lastContactTimestamp(nowMinus8DaysNtpTimestamp)
                .build();

        // When
        final var processedRegistration = reassessRiskLevel.updateWhenAtRiskAndRetentionPeriodHasExpired(registration);

        // Then
        assertThat(processedRegistration).as("Registration is null").isNotNull();
        assertThat(processedRegistration.isAtRisk()).as("Registration is not at risk").isFalse();
        assertThat(processedRegistration.isNotified()).as("Registration is notified for current risk").isTrue();
    }

    @Test
    void risk_level_should_be_reset_when_at_risk_and_not_notified_and_epoch_minimum_is_reached() {
        // Given
        final var registration = Registration.builder()
                .atRisk(true)
                .isNotified(false)
                .latestRiskEpoch(4808)
                .build();
        // When
        final var processedRegistration = reassessRiskLevel.updateWhenAtRiskAndRetentionPeriodHasExpired(registration);
        // Then
        assertThat(processedRegistration).as("Registration is null").isNotNull();
        assertThat(processedRegistration.isAtRisk()).as("Registration is not at risk").isFalse();
        assertThat(processedRegistration.isNotified()).as("Registration is not notified for current risk").isFalse();
    }

    @Test
    void metric_should_be_updated_when_not_notified_and_risk_level_is_reset() {
        // Given
        final var registration = Registration.builder()
                .atRisk(true)
                .isNotified(false)
                .latestRiskEpoch(4808)
                .build();
        // When
        final var processedRegistration = reassessRiskLevel.updateWhenAtRiskAndRetentionPeriodHasExpired(registration);
        // Then
        assertThat(processedRegistration).as("Registration is null").isNotNull();
        assertThat(processedRegistration.isAtRisk()).as("Registration is not at risk").isFalse();
        assertThat(processedRegistration.isNotified()).as("Registration is not notified for current risk").isFalse();
        assertThatCounterMetricIncrement("robert.batch.registration.risk.reset.but.not.notified")
                .as("Increment between before test method and now").isEqualTo(1L);
    }

    @Test
    void metric_should_not_be_updated_when_notified_and_risk_level_is_reset() {

        final var nowMinus8DaysNtpTimestamp = robertClock.now()
                .truncatedTo(DAYS)
                .minus(8, DAYS)
                .asNtpTimestamp();

        final var registration = Registration.builder()
                .atRisk(true)
                .isNotified(true)
                .latestRiskEpoch(4808)
                .lastContactTimestamp(nowMinus8DaysNtpTimestamp)
                .build();

        // When
        final var processedRegistration = reassessRiskLevel.updateWhenAtRiskAndRetentionPeriodHasExpired(registration);

        // Then
        assertThat(processedRegistration).as("Registration is null").isNotNull();
        assertThat(processedRegistration.isAtRisk()).as("Registration is not at risk").isFalse();
        assertThat(processedRegistration.isNotified()).as("Registration is notified for current risk").isTrue();
        assertThatCounterMetricIncrement("robert.batch.registration.risk.reset.but.not.notified").isEqualTo(0L);
    }
}
