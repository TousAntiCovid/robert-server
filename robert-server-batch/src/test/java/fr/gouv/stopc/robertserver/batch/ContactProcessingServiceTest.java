package fr.gouv.stopc.robertserver.batch;

import fr.gouv.stopc.robert.server.common.service.RobertClock;
import fr.gouv.stopc.robert.server.common.utils.TimeUtils;
import fr.gouv.stopc.robertserver.batch.test.IntegrationTest;
import fr.gouv.stopc.robertserver.database.model.EpochExposition;
import fr.gouv.stopc.robertserver.database.model.HelloMessageDetail;
import fr.gouv.stopc.robertserver.database.model.Registration;
import fr.gouv.stopc.robertserver.database.service.IRegistrationService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static fr.gouv.stopc.robertserver.batch.test.HelloMessageFactory.generateHelloMessagesStartingAndDuring;
import static fr.gouv.stopc.robertserver.batch.test.HelloMessageFactory.randRssi;
import static fr.gouv.stopc.robertserver.batch.test.LogbackManager.*;
import static fr.gouv.stopc.robertserver.batch.test.MongodbManager.*;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class ContactProcessingServiceTest {

    private final IRegistrationService registrationService;

    private final RobertClock clock;

    private final JobLauncherTestUtils jobLauncher;

    @SneakyThrows
    private void runRobertBatchJob() {
        jobLauncher.launchJob();
    }

    @Test
    void process_two_helloMessages_and_concat_risk_with_existing_registration_exposed_epochs_risks() {
        var twoDaysAgo = clock.now().minus(2, ChronoUnit.DAYS);
        var fiveDaysAgo = clock.now().minus(5, ChronoUnit.DAYS);

        givenRegistrationExistsForUser(
                "user___1", r -> r
                        .exposedEpochs(
                                List.of(
                                        EpochExposition.builder()
                                                .epochId(twoDaysAgo.asEpochId())
                                                .expositionScores(Collections.singletonList(3.0))
                                                .build(),
                                        EpochExposition.builder()
                                                .epochId(fiveDaysAgo.asEpochId())
                                                .expositionScores(Collections.singletonList(4.3))
                                                .build()
                                )
                        )
        );

        givenPendingContact(
                "user___1", helloMessagesBuilder -> helloMessagesBuilder.addAt(twoDaysAgo, twoDaysAgo.plus(20, SECONDS))
        );

        // When
        runRobertBatchJob();

        // Then
        assertThatContactsToProcess().isEmpty();
        final var expectedRegistration = registrationService.findById("user___1".getBytes())
                .orElseThrow();
        assertThat(expectedRegistration.getExposedEpochs())
                .hasSize(2)
                .filteredOn(e -> e.getEpochId() == twoDaysAgo.asEpochId())
                .asList()
                .flatExtracting("expositionScores")
                .hasSize(2)
                .contains(3.0, 0.0);
        assertThat(expectedRegistration.getExposedEpochs())
                .hasSize(2)
                .filteredOn(e -> e.getEpochId() == fiveDaysAgo.asEpochId())
                .asList()
                .flatExtracting("expositionScores")
                .hasSize(1)
                .contains(4.3);
    }

    @Test
    void process_contact_succeeds_when_has_at_least_one_hello_message_valid() {
        var now = clock.now();

        givenRegistrationExistsForUser("user___1");

        var messageDetails = generateHelloMessagesStartingAndDuring(
                now,
                Duration.of(30, ChronoUnit.SECONDS)
        );

        // Add a helloMessage with timeCollected exceeded max time stamp tolerance
        final int HELLO_MESSAGE_TIME_STAMP_TOLERANCE = 180;
        final var exceededTime = now.plus(HELLO_MESSAGE_TIME_STAMP_TOLERANCE + 1, ChronoUnit.SECONDS);
        messageDetails.add(
                HelloMessageDetail.builder()
                        .rssiCalibrated(randRssi())
                        .timeCollectedOnDevice(
                                exceededTime.asNtpTimestamp()
                        )
                        .timeFromHelloMessage(
                                now.as16LessSignificantBits()
                        )
                        .mac(("mac___exceeded_time").getBytes())
                        .build()
        );

        givenContactExistForUser(
                "user___1", now, c -> c.messageDetails(messageDetails)
        );

        // When
        runRobertBatchJob();

        // Then : Check that there is one bad contact in logs and that the final
        // registration
        // contains one exposed epoch for the computation of the good ones
        assertThatWarnLogs()
                .containsOnlyOnce(
                        String.format(
                                "Time tolerance was exceeded: |%d (HELLO) vs %d (receiving device)| > 180; discarding HELLO message",
                                now.as16LessSignificantBits(), exceededTime.as16LessSignificantBits()
                        )
                );

        assertThatRegistrationForUser("user___1")
                .extracting(Registration::getExposedEpochs)
                .asList()
                .hasSize(1);

        assertThatContactsToProcess().isEmpty();
    }

    @Test
    void process_contact_when_the_contact_is_valid_succeeds() {
        var now = clock.now();

        givenRegistrationExistsForUser("user___1");

        givenPendingContact(
                "user___1", helloMessagesBuilder -> helloMessagesBuilder.addAt(now, now.plus(120, SECONDS))
        );

        // When
        runRobertBatchJob();

        // Then
        assertThatRegistrationForUser("user___1")
                .extracting(Registration::getExposedEpochs)
                .asList()
                .hasSize(1);
        assertThatContactsToProcess().isEmpty();
    }

    @Test
    void process_contact_with_a_bad_encrypted_country_code_fails() {
        // Given
        var now = clock.now();
        var badEcc = new byte[] { (byte) 0xff };

        givenRegistrationExistsForUser("user___1");

        givenPendingContact(
                "user___1", badEcc, helloMessagesBuilder -> helloMessagesBuilder.addAt(now, now.plus(30, SECONDS))
        );

        // When
        runRobertBatchJob();

        // Then
        assertThatInfoLogs()
                .contains("Country code [-1] is not managed by this server ([33])");
        assertThatContactsToProcess().isEmpty();
    }

    @Test
    void process_contact_with_no_messages_fails() {
        // Given
        var now = clock.now();
        givenRegistrationExistsForUser("user___1");

        givenContactExistForUser(
                "user___1", now, c -> c.messageDetails(List.of())
        );

        // When
        runRobertBatchJob();

        // Then
        assertThatInfoLogs()
                .contains("No messages in contact, discarding contact");
        assertThatContactsToProcess().isEmpty();
    }

    @Test
    void process_contact_with_non_existent_registration_fails() {
        // Given
        var now = clock.now();

        var registration = givenRegistrationExistsForUser("user___1");

        givenPendingContact(
                "user___1", helloMessagesBuilder -> helloMessagesBuilder.addAt(now, now.plus(30, SECONDS))
        );

        this.registrationService.delete(registration);

        runRobertBatchJob();

        final var idA = Arrays.toString(registration.getPermanentIdentifier());

        assertThatInfoLogs()
                .contains("No identity exists for id_A " + idA + " extracted from ebid, discarding contact");
        assertThatContactsToProcess().isEmpty();
    }

    @Test
    void process_contact_logs_when_hello_message_timestamp_is_exceeded() {
        // Given
        var now = clock.now();

        givenRegistrationExistsForUser("user___1");

        // Add an helloMessage with timeCollected exceeded max time stamp tolerance
        var messageDetails = new ArrayList<HelloMessageDetail>();
        final int HELLO_MESSAGE_TIME_STAMP_TOLERANCE = 180;
        final var exceededTime = now.plus(HELLO_MESSAGE_TIME_STAMP_TOLERANCE + 1, ChronoUnit.SECONDS);
        messageDetails.add(
                HelloMessageDetail.builder()
                        .rssiCalibrated(randRssi())
                        .timeCollectedOnDevice(
                                exceededTime.asNtpTimestamp()
                        )
                        .timeFromHelloMessage(
                                now.as16LessSignificantBits()
                        )
                        .mac(("mac___exceeded_time").getBytes())
                        .build()
        );

        givenContactExistForUser(
                "user___1", now, c -> c.messageDetails(messageDetails)
        );

        runRobertBatchJob();

        assertThatWarnLogs()
                .containsOnlyOnce(
                        String.format(
                                "Time tolerance was exceeded: |%d (HELLO) vs %d (receiving device)| > 180; discarding HELLO message",
                                now.as16LessSignificantBits(), exceededTime.as16LessSignificantBits()
                        )
                );

        assertThatInfoLogs()
                .contains("Contact did not contain any valid messages; discarding contact");

        assertThatContactsToProcess().isEmpty();
    }

    @Test
    void process_contact_logs_when_the_epochs_are_different() {
        // Given
        var now = clock.now();

        givenRegistrationExistsForUser("user___1");

        // Add HelloMessage with divergence between message and registration
        final var exceededTime = now.plus(TimeUtils.EPOCH_DURATION_SECS * 2L, ChronoUnit.SECONDS);
        var messageDetails = new ArrayList<HelloMessageDetail>();
        messageDetails.add(
                HelloMessageDetail.builder()
                        .rssiCalibrated(randRssi())
                        .timeCollectedOnDevice(
                                exceededTime.asNtpTimestamp()
                        )
                        .timeFromHelloMessage(
                                exceededTime.as16LessSignificantBits()
                        )
                        .mac(("mac___exceeded_time").getBytes())
                        .build()
        );

        givenContactExistForUser(
                "user___1", now, c -> c.messageDetails(messageDetails)
        );

        // When
        runRobertBatchJob();

        // Then

        assertThatWarnLogs().containsOnlyOnce(
                String.format(
                        "Epochid from message %d  vs epochid from ebid  %d > 1 (tolerance); discarding HELLO message",
                        now.asEpochId() + 2, now.asEpochId()
                )
        );
        // Note : DIVERGENT_EPOCHIDS il y a des espaces en trop dans les logs de cette
        // version applicative
        assertThatInfoLogs()
                .contains("Contact did not contain any valid messages; discarding contact");
        assertThatContactsToProcess().isEmpty();

        assertThatRegistrationForUser("user___1")
                .extracting(Registration::getExposedEpochs)
                .asList()
                .isEmpty();
    }

    @Test
    void process_contact_succeeds_and_when_some_of_the_epochs_are_different_logs_and_discard() {
        // Given
        var now = clock.now();

        givenRegistrationExistsForUser("user___1");
        var messageDetails = generateHelloMessagesStartingAndDuring(
                now,
                Duration.of(30, ChronoUnit.SECONDS)
        );

        // Add HelloMessage with time divergence with contact epoch
        final var exceededTime = now.plus(TimeUtils.EPOCH_DURATION_SECS * 2L, ChronoUnit.SECONDS);
        messageDetails.add(
                HelloMessageDetail.builder()
                        .rssiCalibrated(randRssi())
                        .timeCollectedOnDevice(
                                exceededTime.asNtpTimestamp()
                        )
                        .timeFromHelloMessage(
                                exceededTime.as16LessSignificantBits()
                        )
                        .mac(("mac___exceeded_time").getBytes())
                        .build()
        );

        givenContactExistForUser(
                "user___1", now, c -> c.messageDetails(messageDetails)
        );

        // When
        runRobertBatchJob();

        // Then
        assertThatWarnLogs().containsOnlyOnce(
                String.format(
                        "Epochid from message %d  vs epochid from ebid  %d > 1 (tolerance); discarding HELLO message",
                        now.asEpochId() + 2, now.asEpochId()
                )
        );
        assertThatContactsToProcess().isEmpty();

        assertThatRegistrationForUser("user___1")
                .extracting(Registration::getExposedEpochs)
                .asList()
                .hasSize(1);
    }
}
