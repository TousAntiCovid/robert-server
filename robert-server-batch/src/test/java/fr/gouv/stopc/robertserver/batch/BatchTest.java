package fr.gouv.stopc.robertserver.batch;

import fr.gouv.stopc.robertserver.batch.test.IntegrationTest;
import fr.gouv.stopc.robertserver.common.RobertClock;
import fr.gouv.stopc.robertserver.database.model.EpochExposition;
import fr.gouv.stopc.robertserver.database.model.Kpi;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static fr.gouv.stopc.robertserver.batch.test.LogbackManager.assertThatInfoLogs;
import static fr.gouv.stopc.robertserver.batch.test.MessageMatcher.assertThatEpochExpositionsForIdA;
import static fr.gouv.stopc.robertserver.batch.test.MongodbManager.*;
import static java.time.temporal.ChronoUnit.DAYS;
import static org.assertj.core.api.Assertions.tuple;

@IntegrationTest
class BatchTest {

    @Autowired
    private JobLauncherTestUtils jobLauncher;

    @Autowired
    private RobertClock clock;

    @SneakyThrows
    private void runRobertBatchJob() {
        jobLauncher.launchJob();
    }

    @Test
    void can_run() {
        runRobertBatchJob();

        assertThatInfoLogs()
                .contains(
                        "0 hello messages waiting for process",
                        "0 hello messages remaining after process"
                );
    }

    @Test
    void can_process_contact() {
        // Given
        final var yesterday = clock.now().minus(1, DAYS);

        givenRegistrationExistsForIdA("user___1");
        givenPendingContact()
                .idA("user___1")
                .withValidHelloMessageAt(yesterday)
                .build();

        // When
        runRobertBatchJob();

        // Then
        assertThatInfoLogs()
                .contains(
                        "1 hello messages waiting for process",
                        "0 hello messages remaining after process"
                );

        assertThatEpochExpositionsForIdA("user___1")
                .containsOnlyOnce(
                        new EpochExposition(yesterday.asEpochId(), List.of(0.0))
                );

        assertThatKpis().as("check kpis values")
                .extracting(Kpi::getName, Kpi::getValue)
                .containsExactlyInAnyOrder(
                        tuple("exposedButNotAtRiskUsers", 1L),
                        tuple("infectedUsersNotNotified", 0L),
                        tuple("notifiedUsersScoredAgain", 0L)
                );
    }

    @Test
    void can_process_contact_with_multiple_messages() {
        // Given
        final var yesterday = clock.now().minus(1, DAYS);

        givenRegistrationExistsForIdA("user___1");
        givenPendingContact()
                .idA("user___1")
                .withValidHelloMessageAt(yesterday, 2)
                .build();

        // When
        runRobertBatchJob();

        // Then
        assertThatInfoLogs()
                .contains(
                        "2 hello messages waiting for process",
                        "0 hello messages remaining after process"
                );

        assertThatEpochExpositionsForIdA("user___1")
                .containsOnlyOnce(
                        new EpochExposition(yesterday.asEpochId(), List.of(0.0))
                );

        assertThatKpis().as("check kpis values")
                .extracting(Kpi::getName, Kpi::getValue)
                .containsExactlyInAnyOrder(
                        tuple("exposedButNotAtRiskUsers", 1L),
                        tuple("infectedUsersNotNotified", 0L),
                        tuple("notifiedUsersScoredAgain", 0L)
                );
    }
}
