package fr.gouv.stopc.robertserver.batch;

import fr.gouv.stopc.robert.server.common.service.RobertClock;
import fr.gouv.stopc.robertserver.batch.test.IntegrationTest;
import fr.gouv.stopc.robertserver.database.model.EpochExposition;
import fr.gouv.stopc.robertserver.database.model.Registration;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static fr.gouv.stopc.robertserver.batch.test.LogbackManager.assertThatInfoLogs;
import static fr.gouv.stopc.robertserver.batch.test.MongodbManager.*;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.MINUTES;

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
        final var yesterday = clock.now().minus(1, DAYS);

        givenRegistrationExistsForUser("user___1");
        givenGivenPendingContact()
                .idA("user___1")
                .withValidHelloMessages(
                        helloMessagesBuilder -> helloMessagesBuilder.addAt(yesterday)
                )
                .build();

        runRobertBatchJob();

        assertThatInfoLogs()
                .contains(
                        "1 hello messages waiting for process",
                        "0 hello messages remaining after process"
                );

        assertThatRegistrationForUser("user___1")
                .extracting(Registration::getExposedEpochs)
                .asList()
                .contains(
                        new EpochExposition(yesterday.asEpochId(), List.of(0.0))
                );
    }

    @Test
    void can_process_contact_with_multiple_hellomessages() {
        final var yesterday = clock.now().minus(1, DAYS);

        givenRegistrationExistsForUser("user___1");
        givenGivenPendingContact()
                .idA("user___1")
                .withValidHelloMessages(
                        helloMessagesBuilder -> helloMessagesBuilder.addAt(yesterday, yesterday.plus(5, MINUTES))
                )
                .build();

        runRobertBatchJob();

        assertThatInfoLogs()
                .contains(
                        "2 hello messages waiting for process",
                        "0 hello messages remaining after process"
                );

        assertThatRegistrationForUser("user___1")
                .extracting(Registration::getExposedEpochs)
                .asList()
                .contains(
                        new EpochExposition(yesterday.asEpochId(), List.of(0.0))
                );
    }
}
