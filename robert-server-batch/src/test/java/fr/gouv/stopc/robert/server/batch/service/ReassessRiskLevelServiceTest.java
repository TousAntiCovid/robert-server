package fr.gouv.stopc.robert.server.batch.service;

import fr.gouv.stopc.robert.server.batch.IntegrationTest;
import fr.gouv.stopc.robert.server.batch.configuration.PropertyLoader;
import fr.gouv.stopc.robert.server.batch.manager.MetricsManager;
import fr.gouv.stopc.robert.server.batch.manager.MongodbManager;
import fr.gouv.stopc.robert.server.common.service.RobertClock;
import fr.gouv.stopc.robertserver.database.model.Registration;
import lombok.RequiredArgsConstructor;
import nl.altindag.log.LogCaptor;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestExecutionListeners;

import java.time.Instant;
import java.util.List;

import static fr.gouv.stopc.robert.server.batch.manager.MetricsManager.assertThatCounterMetricIncrement;
import static fr.gouv.stopc.robert.server.batch.manager.MetricsManager.assertThatTimerMetricIncrement;
import static fr.gouv.stopc.robert.server.batch.manager.MongodbManager.assertThatRegistrationForUser;
import static fr.gouv.stopc.robert.server.batch.manager.MongodbManager.givenRegistrationExistsForUser;
import static java.time.temporal.ChronoUnit.DAYS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.context.TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS;

@IntegrationTest
@TestExecutionListeners(listeners = {
        MetricsManager.class,
        MongodbManager.class
}, mergeMode = MERGE_WITH_DEFAULTS)
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class ReassessRiskLevelServiceTest {

    private final ReassessRiskLevelService reassessRiskLevelService;

    private final RobertClock robertClock;

    private final PropertyLoader propertyLoader;

    @Test
    void can_log_start_and_end_of_purge_process() {
        try (final var logCaptor = LogCaptor.forClass(ReassessRiskLevelService.class)) {
            reassessRiskLevelService.performs();

            assertThat(logCaptor.getInfoLogs())
                    .contains(
                            "START : Reset risk level of registrations when retention time > "
                                    + propertyLoader.getRiskLevelRetentionPeriodInDays() + ".",
                            "END : Reset risk level of registrations."
                    );
        }
    }

    @Test
    void metric_is_incremented_when_process_performed() {
        reassessRiskLevelService.performs();
        assertThatTimerMetricIncrement("robert.batch", "operation", "REGISTRATION_RISK_RESET_STEP").isEqualTo(1L);
    }

    @Test
    void risk_level_should_not_be_reset_when_not_at_risk_and_not_notified() {
        // Given
        var registration = givenRegistrationExistsForUser(
                "user___1", r -> r
                        .exposedEpochs(List.of())
                        .atRisk(false)
                        .isNotified(false)
        );

        // When
        reassessRiskLevelService.performs();

        // Then
        assertThatRegistrationForUser("user___1")
                .as("Object has not been updated")
                .isEqualTo(registration);

        assertThatCounterMetricIncrement("robert.batch.risk.reset", "notified", "false").isEqualTo(0L);
        assertThatCounterMetricIncrement("robert.batch.risk.reset", "notified", "true").isEqualTo(0L);
    }

    @Test
    void risk_level_should_not_be_reset_when_not_at_risk_and_notified() {
        // Given
        var registration = givenRegistrationExistsForUser(
                "user___1", r -> r
                        .exposedEpochs(List.of())
                        .atRisk(false)
                        .isNotified(true)
        );

        // When
        reassessRiskLevelService.performs();

        // Then
        assertThatRegistrationForUser("user___1")
                .as("Object has not been updated")
                .isEqualTo(registration);
        assertThatCounterMetricIncrement("robert.batch.risk.reset", "notified", "false").isEqualTo(0L);
        assertThatCounterMetricIncrement("robert.batch.risk.reset", "notified", "true").isEqualTo(0L);
    }

    @ParameterizedTest
    @ValueSource(ints = { 1, 2, 3, 4, 5, 6, 7 })
    void risk_level_should_not_be_reset_when_at_risk_and_notified_but_last_contact_date_is_under_7_days_ago(
            int lastContactDate) {

        // Given
        final var nowMinusDays = robertClock.at(
                Instant.now()
                        .truncatedTo(DAYS)
                        .minus(lastContactDate, DAYS)
        );
        var registration = givenRegistrationExistsForUser(
                "user___1", r -> r
                        .exposedEpochs(List.of())
                        .atRisk(true)
                        .isNotified(true)
                        .lastContactTimestamp(nowMinusDays.asNtpTimestamp())
        );

        // When
        reassessRiskLevelService.performs();

        // Then
        assertThatRegistrationForUser("user___1")
                .as("Object has not been updated")
                .isEqualTo(registration);
        assertThatCounterMetricIncrement("robert.batch.risk.reset", "notified", "false").isEqualTo(0L);
        assertThatCounterMetricIncrement("robert.batch.risk.reset", "notified", "true").isEqualTo(0L);
    }

    @ParameterizedTest
    @ValueSource(ints = { 8, 9, 10, 12, 13, 14 })
    void risk_level_should_be_reset_when_at_risk_and_notified_and_last_contact_date_is_above_7_days_ago(
            int lastContactDate) {
        // Given
        final var nowMinusDays = robertClock.at(
                Instant.now()
                        .truncatedTo(DAYS)
                        .minus(lastContactDate, DAYS)
        );
        givenRegistrationExistsForUser(
                "user___1", r -> r
                        .exposedEpochs(List.of())
                        .atRisk(true)
                        .isNotified(true)
                        .lastContactTimestamp(nowMinusDays.asNtpTimestamp())
        );

        // When
        reassessRiskLevelService.performs();

        // Then
        assertThatRegistrationForUser("user___1")
                .extracting(Registration::isAtRisk)
                .asInstanceOf(InstanceOfAssertFactories.BOOLEAN)
                .as("Registration is not at risk")
                .isFalse();

        assertThatRegistrationForUser("user___1")
                .extracting(Registration::isNotified)
                .asInstanceOf(InstanceOfAssertFactories.BOOLEAN)
                .as("Registration is notified for current risk")
                .isTrue();

        assertThatCounterMetricIncrement("robert.batch.risk.reset", "notified", "true")
                .as("Increment between before test method and now").isEqualTo(1L);
        assertThatCounterMetricIncrement("robert.batch.risk.reset", "notified", "false").isEqualTo(0L);
    }

    @Test
    void risk_level_should_be_reset_when_at_risk_and_not_notified_and_epoch_minimum_is_reached() {
        // Given
        givenRegistrationExistsForUser(
                "user___1", r -> r
                        .exposedEpochs(List.of())
                        .atRisk(true)
                        .isNotified(false)
        );

        // When
        reassessRiskLevelService.performs();

        // Then
        assertThatRegistrationForUser("user___1")
                .extracting(Registration::isAtRisk)
                .asInstanceOf(InstanceOfAssertFactories.BOOLEAN)
                .as("Registration is not at risk")
                .isFalse();

        assertThatRegistrationForUser("user___1")
                .extracting(Registration::isNotified)
                .asInstanceOf(InstanceOfAssertFactories.BOOLEAN)
                .as("Registration is notified for current risk")
                .isFalse();

        assertThatCounterMetricIncrement("robert.batch.risk.reset", "notified", "false")
                .as("Increment between before test method and now").isEqualTo(1L);
        assertThatCounterMetricIncrement("robert.batch.risk.reset", "notified", "true")
                .as("Increment between before test method and now").isEqualTo(0L);
    }
}
