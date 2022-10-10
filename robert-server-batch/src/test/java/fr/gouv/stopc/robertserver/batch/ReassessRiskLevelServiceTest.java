package fr.gouv.stopc.robertserver.batch;

import fr.gouv.stopc.robert.server.common.service.RobertClock;
import fr.gouv.stopc.robertserver.batch.test.IntegrationTest;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.List;

import static fr.gouv.stopc.robertserver.batch.test.LogbackManager.assertThatInfoLogs;
import static fr.gouv.stopc.robertserver.batch.test.MessageMatcher.assertThatRegistrationForIdA;
import static fr.gouv.stopc.robertserver.batch.test.MongodbManager.givenRegistrationExistsForIdA;
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
        givenRegistrationExistsForIdA(
                "user___1", r -> r
                        .exposedEpochs(List.of())
                        .atRisk(false)
                        .isNotified(false)
        );

        // When
        runRobertBatchJob();

        // Then
        assertThatRegistrationForIdA("user___1").hasFieldOrPropertyWithValue("atRisk", false);
    }

    @Test
    void risk_level_should_not_be_reset_when_not_at_risk_and_notified() {
        // Given
        givenRegistrationExistsForIdA(
                "user___1", r -> r
                        .exposedEpochs(List.of())
                        .atRisk(false)
                        .isNotified(true)
        );

        // When
        runRobertBatchJob();

        // Then
        assertThatRegistrationForIdA("user___1").hasFieldOrPropertyWithValue("atRisk", false);
    }

    @ParameterizedTest
    @ValueSource(ints = { 1, 2, 3, 4, 5, 6, 7 })
    void risk_level_should_not_be_reset_when_at_risk_and_notified_but_last_contact_date_is_under_7_days_ago(
            int lastContactDate) {

        // Given
        final var someDaysAgo = robertClock.at(
                Instant.now()
                        .truncatedTo(DAYS)
                        .minus(lastContactDate, DAYS)
        );
        givenRegistrationExistsForIdA(
                "user___1", r -> r
                        .exposedEpochs(List.of())
                        .atRisk(true)
                        .isNotified(true)
                        .lastContactTimestamp(someDaysAgo.asNtpTimestamp())
        );

        // When
        runRobertBatchJob();

        // Then
        assertThatRegistrationForIdA("user___1").hasFieldOrPropertyWithValue("atRisk", true);
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
        givenRegistrationExistsForIdA(
                "user___1", r -> r
                        .exposedEpochs(List.of())
                        .atRisk(true)
                        .isNotified(true)
                        .lastContactTimestamp(nowMinusDays.asNtpTimestamp())
        );

        // When
        runRobertBatchJob();

        // Then
        assertThatRegistrationForIdA("user___1").hasFieldOrPropertyWithValue("atRisk", false);
        assertThatRegistrationForIdA("user___1").hasFieldOrPropertyWithValue("isNotified", true);
    }

    @Test
    void risk_level_should_be_reset_when_at_risk_and_not_notified_and_epoch_minimum_is_reached() {
        // Given
        givenRegistrationExistsForIdA(
                "user___1", r -> r
                        .exposedEpochs(List.of())
                        .atRisk(true)
                        .isNotified(false)
        );

        // When
        runRobertBatchJob();

        // Then
        assertThatRegistrationForIdA("user___1").hasFieldOrPropertyWithValue("atRisk", false);
        assertThatRegistrationForIdA("user___1").hasFieldOrPropertyWithValue("isNotified", false);
    }
}
