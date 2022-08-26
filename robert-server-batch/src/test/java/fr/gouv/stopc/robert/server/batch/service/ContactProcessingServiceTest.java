package fr.gouv.stopc.robert.server.batch.service;

import com.google.protobuf.ByteString;
import fr.gouv.stopc.robert.server.batch.IntegrationTest;
import fr.gouv.stopc.robert.server.batch.manager.GrpcMockManager;
import fr.gouv.stopc.robert.server.batch.manager.HelloMessageFactory;
import fr.gouv.stopc.robert.server.batch.manager.MetricsManager;
import fr.gouv.stopc.robert.server.batch.manager.MongodbManager;
import fr.gouv.stopc.robert.server.common.service.RobertClock;
import fr.gouv.stopc.robert.server.common.utils.TimeUtils;
import fr.gouv.stopc.robertserver.database.model.EpochExposition;
import fr.gouv.stopc.robertserver.database.model.HelloMessageDetail;
import fr.gouv.stopc.robertserver.database.model.Registration;
import fr.gouv.stopc.robertserver.database.service.ContactService;
import fr.gouv.stopc.robertserver.database.service.IRegistrationService;
import lombok.RequiredArgsConstructor;
import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestExecutionListeners;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static fr.gouv.stopc.robert.server.batch.manager.GrpcMockManager.*;
import static fr.gouv.stopc.robert.server.batch.manager.HelloMessageFactory.generateHelloMessagesStartingAndDuring;
import static fr.gouv.stopc.robert.server.batch.manager.HelloMessageFactory.randRssi;
import static fr.gouv.stopc.robert.server.batch.manager.MetricsManager.assertThatLogsMatchingRegex;
import static fr.gouv.stopc.robert.server.batch.manager.MetricsManager.assertThatTimerMetricIncrement;
import static fr.gouv.stopc.robert.server.batch.manager.MongodbManager.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.context.TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS;

@IntegrationTest
@TestExecutionListeners(listeners = {
        GrpcMockManager.class,
        MongodbManager.class,
        MetricsManager.class
}, mergeMode = MERGE_WITH_DEFAULTS)
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class ContactProcessingServiceTest {

    private final ContactProcessingService contactProcessingService;

    private final ContactService contactService;

    private final IRegistrationService registrationService;

    private final RobertClock clock;

    private final String TIME_TOLERANCE_REGEX = "(Time tolerance was exceeded: \\|[\\d]{1,} \\(HELLO\\) vs [\\d]{1,} \\(receiving device\\)\\| > 180; discarding HELLO message)";

    private final String DIVERGENT_EPOCHIDS = "Epochid from message [\\d]{1,} vs epochid from ebid [\\d]{1,} > 1 \\(tolerance\\); discarding HELLO message";

    @AfterEach
    public void afterAll() {
        this.contactService.deleteAll();
        this.registrationService.deleteAll();
    }

    @Test
    void metric_is_incremented_when_process_performed() {
        contactProcessingService.performs();
        assertThatTimerMetricIncrement("robert.batch", "operation", "CONTACT_SCORING_STEP").isEqualTo(1L);
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

        givenContactExistForUser(
                "user___1", twoDaysAgo, c -> c.messageDetails(
                        generateHelloMessagesStartingAndDuring(
                                twoDaysAgo,
                                Duration.of(20, ChronoUnit.SECONDS)
                        )
                )
        );

        // When
        contactProcessingService.performs();

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

        // Add an helloMessage with timeCollected exceeded max time stamp tolerance
        final int HELLO_MESSAGE_TIME_STAMP_TOLERANCE = 180;
        final var exceededTime = now.plus(HELLO_MESSAGE_TIME_STAMP_TOLERANCE + 1, ChronoUnit.SECONDS);
        messageDetails.add(
                HelloMessageDetail.builder()
                        .rssiCalibrated(HelloMessageFactory.randRssi())
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

        try (final var logCaptor = LogCaptor.forClass(ContactProcessingService.class)) {
            // When
            contactProcessingService.performs();

            // Then : Check that there is one bad contact in logs and that the final
            // registration
            // contains one exposed epoch for the computation of the good ones
            assertThatLogsMatchingRegex(logCaptor.getWarnLogs(), TIME_TOLERANCE_REGEX, 1);

            assertThatRegistrationForUser("user___1")
                    .extracting(Registration::getExposedEpochs)
                    .asList()
                    .hasSize(1);

            assertThatContactsToProcess().isEmpty();
        }
    }

    @Test
    void process_contact_when_the_contact_is_valid_succeeds() {
        var now = clock.now();

        givenRegistrationExistsForUser("user___1");

        givenContactExistForUser(
                "user___1", now, c -> c.messageDetails(
                        generateHelloMessagesStartingAndDuring(
                                now,
                                Duration.of(120, ChronoUnit.SECONDS)
                        )
                )
        );

        // When
        contactProcessingService.performs();

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

        givenRegistrationExistsForUser("user___1");

        givenContactExistForUser(
                "user___1", now, c -> c.messageDetails(
                        generateHelloMessagesStartingAndDuring(
                                now,
                                Duration.of(30, ChronoUnit.SECONDS)
                        )
                )
        );

        givenCryptoServerCountryCode(ByteString.copyFrom(new byte[] { (byte) 0xff }));

        try (final var logCaptor = LogCaptor.forClass(ContactProcessingService.class)) {
            // When
            contactProcessingService.performs();

            // Then
            assertThat(logCaptor.getInfoLogs())
                    .contains("Country code [-1] is not managed by this server ([33])");
            assertThatContactsToProcess().isEmpty();
        }
    }

    @Test
    void process_contact_with_no_messages_fails() {
        // Given
        var now = clock.now();
        givenRegistrationExistsForUser("user___1");

        givenContactExistForUser(
                "user___1", now, c -> c.messageDetails(List.of())
        );

        try (final var logCaptor = LogCaptor.forClass(ContactProcessingService.class)) {
            // When
            contactProcessingService.performs();

            // Then
            assertThat(logCaptor.getInfoLogs())
                    .contains("No messages in contact, discarding contact");
            assertThatContactsToProcess().isEmpty();
        }
    }

    @Test
    void process_contact_with_non_existent_registration_fails() {
        // Given
        var now = clock.now();

        var registration = givenRegistrationExistsForUser("user___1");

        givenContactExistForUser(
                "user___1", now, c -> c.messageDetails(
                        generateHelloMessagesStartingAndDuring(
                                now,
                                Duration.of(30, ChronoUnit.SECONDS)
                        )
                )
        );

        this.registrationService.delete(registration);

        try (final var logCaptor = LogCaptor.forClass(ContactProcessingService.class)) {
            contactProcessingService.performs();

            final var idA = Arrays.toString(registration.getPermanentIdentifier());

            assertThat(logCaptor.getInfoLogs())
                    .contains("No identity exists for id_A " + idA + " extracted from ebid, discarding contact");
            assertThatContactsToProcess().isEmpty();
        }
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
                        .rssiCalibrated(HelloMessageFactory.randRssi())
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

        try (final var logCaptor = LogCaptor.forClass(ContactProcessingService.class)) {
            contactProcessingService.performs();

            assertThatLogsMatchingRegex(logCaptor.getWarnLogs(), TIME_TOLERANCE_REGEX, 1);

            assertThat(logCaptor.getInfoLogs())
                    .contains("Contact did not contain any valid messages; discarding contact");

            assertThatContactsToProcess().isEmpty();
        }
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

        try (final var logCaptor = LogCaptor.forClass(ContactProcessingService.class)) {
            // When
            contactProcessingService.performs();

            // Then
            assertThatLogsMatchingRegex(logCaptor.getWarnLogs(), DIVERGENT_EPOCHIDS, 1);
            assertThat(logCaptor.getInfoLogs())
                    .contains("Contact did not contain any valid messages; discarding contact");
            assertThatContactsToProcess().isEmpty();

            assertThatRegistrationForUser("user___1")
                    .extracting(Registration::getExposedEpochs)
                    .asList()
                    .isEmpty();
        }
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

        try (final var logCaptor = LogCaptor.forClass(ContactProcessingService.class)) {
            // When
            contactProcessingService.performs();

            // Then
            assertThatLogsMatchingRegex(logCaptor.getWarnLogs(), DIVERGENT_EPOCHIDS, 1);
            assertThatContactsToProcess().isEmpty();

            assertThatRegistrationForUser("user___1")
                    .extracting(Registration::getExposedEpochs)
                    .asList()
                    .hasSize(1);
        }
    }
}
