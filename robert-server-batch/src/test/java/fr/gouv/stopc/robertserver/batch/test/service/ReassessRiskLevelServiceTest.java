package fr.gouv.stopc.robertserver.batch.test.service;

import fr.gouv.stopc.robert.server.common.service.RobertClock;
import fr.gouv.stopc.robertserver.batch.test.IntegrationTest;
import fr.gouv.stopc.robertserver.database.model.Registration;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.List;

import static fr.gouv.stopc.robertserver.batch.test.LogbackManager.assertThatInfoLogs;
import static fr.gouv.stopc.robertserver.batch.test.MongodbManager.assertThatRegistrationForUser;
import static fr.gouv.stopc.robertserver.batch.test.MongodbManager.givenRegistrationExistsForUser;
import static java.time.temporal.ChronoUnit.DAYS;

@IntegrationTest
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class ReassessRiskLevelServiceTest {

    private final JobLauncherTestUtils jobLauncher;

    private final RobertClock robertClock;

    public final int RISK_LEVEL_RETENTION_PERIOD_IN_DAYS = 7;

    @SneakyThrows
    private void runRobertBatchJob() {
        jobLauncher.launchJob();
    }

    @Test
    void can_log_start_and_end_of_purge_process() {
        // When
        runRobertBatchJob();

        assertThatInfoLogs()
                .contains(
                        "START : Reset risk level of registrations when retention time > "
                                + RISK_LEVEL_RETENTION_PERIOD_IN_DAYS + ".",
                        "END : Reset risk level of registrations."
                );
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
        runRobertBatchJob();

        // Then
        assertThatRegistrationForUser("user___1")
                .as("Object has not been updated")
                .isEqualTo(registration);
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
        runRobertBatchJob();

        // Then
        assertThatRegistrationForUser("user___1")
                .as("Object has not been updated")
                .isEqualTo(registration);
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
        runRobertBatchJob();

        // Then
        assertThatRegistrationForUser("user___1")
                .as("Object has not been updated")
                .isEqualTo(registration);
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
        runRobertBatchJob();

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
        runRobertBatchJob();

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
    }
}
