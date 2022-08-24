package fr.gouv.stopc.robert.server.batch.service;

import com.google.protobuf.ByteString;
import fr.gouv.stopc.robert.server.batch.IntegrationTest;
import fr.gouv.stopc.robert.server.batch.manager.GrpcMockManager;
import fr.gouv.stopc.robert.server.batch.manager.HelloMessageFactory;
import fr.gouv.stopc.robert.server.batch.manager.MetricsManager;
import fr.gouv.stopc.robert.server.batch.manager.MongodbManager;
import fr.gouv.stopc.robert.server.common.service.RobertClock;
import fr.gouv.stopc.robertserver.database.model.EpochExposition;
import fr.gouv.stopc.robertserver.database.model.Registration;
import fr.gouv.stopc.robertserver.database.service.ContactService;
import fr.gouv.stopc.robertserver.database.service.IRegistrationService;
import lombok.RequiredArgsConstructor;
import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestExecutionListeners;

import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static fr.gouv.stopc.robert.server.batch.manager.GrpcMockManager.*;
import static fr.gouv.stopc.robert.server.batch.manager.HelloMessageFactory.*;
import static fr.gouv.stopc.robert.server.batch.manager.MetricsManager.assertThatLogsMatchingRegex;
import static fr.gouv.stopc.robert.server.batch.manager.MetricsManager.assertThatTimerMetricIncrement;
import static fr.gouv.stopc.robert.server.batch.manager.MongodbManager.givenContactExistForUser;
import static fr.gouv.stopc.robert.server.batch.manager.MongodbManager.givenRegistrationExistsForUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.context.TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS;

@IntegrationTest
@TestExecutionListeners(listeners = {
        GrpcMockManager.class,
        MongodbManager.class,
        MetricsManager.class,
        HelloMessageFactory.class
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
    void process_two_contacts_and_concat_with_existing_exposed_epochs() {
        var twoDaysAgo = clock.now().minus(2, ChronoUnit.DAYS);
        var fiveDaysAgo = clock.now().minus(5, ChronoUnit.DAYS);

        givenRegistrationExistsForUser(
                "user___1", r -> r
                        .exposedEpochs(
                                List.of(
                                        EpochExposition.builder()
                                                .epochId(twoDaysAgo.asEpochId())
                                                .expositionScores(Arrays.asList(3.0))
                                                .build(),
                                        EpochExposition.builder()
                                                .epochId(fiveDaysAgo.asEpochId())
                                                .expositionScores(Arrays.asList(4.3))
                                                .build()
                                )
                        )
        );

        givenContactExistForUser(
                "user___1", c -> c.messageDetails(generateHelloMessagesStartingAndDuringSeconds(twoDaysAgo, 20))
        );

        givenCryptoServerIdA(ByteString.copyFrom("user___1".getBytes()));
        givenCryptoServerEpochId(twoDaysAgo.asEpochId());

        // When
        contactProcessingService.performs();

        // Then
        assertThat(this.contactService.findAll()).isEmpty();
        Optional<Registration> expectedRegistration = registrationService.findById("user___1".getBytes());
        assertThat(expectedRegistration).isPresent();
        assertThat(expectedRegistration.get().getExposedEpochs())
                .isNotEmpty()
                .hasSize(2)
                .filteredOn(e -> e.getEpochId() == twoDaysAgo.asEpochId())
                .isNotEmpty()
                .asList()
                .flatExtracting("expositionScores")
                .hasSize(2);
    }

    @Test
    void process_contact_succeeds_when_has_at_least_one_hello_message_valid() {
        var now = clock.now();

        givenRegistrationExistsForUser("user___1");

        var messageDetails = generateHelloMessagesStartingAndDuringSeconds(now, 30);
        messageDetails.addAll(generateHelloMessageWithTimeCollectedOnDeviceExceeded(now));

        givenContactExistForUser(
                "user___1", c -> c.messageDetails(messageDetails)
        );

        givenCryptoServerIdA(ByteString.copyFrom("user___1".getBytes()));
        givenCryptoServerEpochId(now.asEpochId());

        try (final var logCaptor = LogCaptor.forClass(ContactProcessingService.class)) {
            // When
            contactProcessingService.performs();

            // Then : Check that there is one bad contact in logs and that the final
            // registration
            // contains one exposed epoch for the computation of the good ones
            assertThatLogsMatchingRegex(logCaptor.getWarnLogs(), TIME_TOLERANCE_REGEX, 1);

            Optional<Registration> expectedRegistration = registrationService
                    .findById("user___1".getBytes());

            assertThat(expectedRegistration).isPresent();
            assertThat(expectedRegistration.get().getExposedEpochs())
                    .isNotEmpty()
                    .hasSize(1);
            assertThat(this.contactService.findAll()).isEmpty();
        }
    }

    @Test
    void process_contact_when_the_contact_is_valid_succeeds() {
        var now = clock.now();

        givenRegistrationExistsForUser("user___1");

        givenContactExistForUser(
                "user___1", c -> c.messageDetails(generateHelloMessagesStartingAndDuringSeconds(now, 120))
        );

        givenCryptoServerIdA(ByteString.copyFrom("user___1".getBytes()));
        givenCryptoServerEpochId(now.asEpochId());

        // When
        contactProcessingService.performs();

        // Then
        Optional<Registration> expectedRegistration = registrationService
                .findById("user___1".getBytes());
        assertThat(expectedRegistration).isPresent();
        assertThat(expectedRegistration.get().getExposedEpochs())
                .isNotEmpty()
                .hasSize(1);
        assertThat(this.contactService.findAll()).isEmpty();
    }

    @Test
    void process_contact_with_a_bad_encrypted_country_code_fails() {
        // Given
        var now = clock.now();

        givenRegistrationExistsForUser("user___1");

        givenContactExistForUser(
                "user___1", c -> c.messageDetails(generateHelloMessagesStartingAndDuringSeconds(now, 30))
        );

        givenCryptoServerIdA(ByteString.copyFrom("user___1".getBytes()));
        givenCryptoServerCountryCode(ByteString.copyFrom(new byte[] { (byte) 0xff }));

        try (final var logCaptor = LogCaptor.forClass(ContactProcessingService.class)) {
            // When
            contactProcessingService.performs();

            // Then
            assertThat(logCaptor.getInfoLogs())
                    .contains("Country code [-1] is not managed by this server ([33])");
            assertThat(this.contactService.findAll()).isEmpty();
        }
    }

    @Test
    void process_contact_with_no_messages_fails() {
        // Given
        givenRegistrationExistsForUser("user___1");

        givenContactExistForUser(
                "user___1", c -> c.messageDetails(List.of())
        );

        givenCryptoServerIdA(ByteString.copyFrom("user___1".getBytes()));

        try (final var logCaptor = LogCaptor.forClass(ContactProcessingService.class)) {
            // When
            contactProcessingService.performs();

            // Then
            assertThat(logCaptor.getInfoLogs())
                    .contains("No messages in contact, discarding contact");
            assertThat(this.contactService.findAll()).isEmpty();
        }
    }

    @Test
    void process_contact_with_non_existent_registration_fails() {
        // Given
        var now = clock.now();

        var registration = givenRegistrationExistsForUser("user___1");

        givenContactExistForUser(
                "user___1", c -> c.messageDetails(generateHelloMessagesStartingAndDuringSeconds(now, 30))
        );

        givenCryptoServerIdA(ByteString.copyFrom("user___1".getBytes()));

        this.registrationService.delete(registration);

        try (final var logCaptor = LogCaptor.forClass(ContactProcessingService.class)) {
            contactProcessingService.performs();

            final var idA = Arrays.toString(registration.getPermanentIdentifier());

            assertThat(logCaptor.getInfoLogs())
                    .contains("No identity exists for id_A " + idA + " extracted from ebid, discarding contact");
            assertThat(this.contactService.findAll()).isEmpty();
        }
    }

    @Test
    void process_contact_logs_when_hello_message_timestamp_is_exceeded() {
        // Given
        var now = clock.now();

        var registration = givenRegistrationExistsForUser("user___1");

        givenContactExistForUser(
                "user___1", c -> c.messageDetails(generateHelloMessageWithTimeCollectedOnDeviceExceeded(now))
        );

        givenCryptoServerIdA(ByteString.copyFrom("user___1".getBytes()));

        try (final var logCaptor = LogCaptor.forClass(ContactProcessingService.class)) {
            contactProcessingService.performs();

            assertThatLogsMatchingRegex(logCaptor.getWarnLogs(), TIME_TOLERANCE_REGEX, 1);

            assertThat(logCaptor.getInfoLogs())
                    .contains("Contact did not contain any valid messages; discarding contact");

            assertThat(this.contactService.findAll()).isEmpty();
        }
    }

    @Test
    void process_contact_logs_when_the_epochs_are_different() {
        // Given
        var now = clock.now();

        var registration = givenRegistrationExistsForUser("user___1");

        givenContactExistForUser(
                "user___1", c -> c.messageDetails(generateHelloMessageWithDivergenceBetweenMessageAndRegistration(now))
        );

        givenCryptoServerIdA(ByteString.copyFrom("user___1".getBytes()));

        try (final var logCaptor = LogCaptor.forClass(ContactProcessingService.class)) {
            // When
            contactProcessingService.performs();

            // Then
            assertThatLogsMatchingRegex(logCaptor.getWarnLogs(), DIVERGENT_EPOCHIDS, 1);
            assertThat(logCaptor.getInfoLogs())
                    .contains("Contact did not contain any valid messages; discarding contact");
            assertThat(this.contactService.findAll()).isEmpty();
            Optional<Registration> expectedRegistration = registrationService
                    .findById(registration.getPermanentIdentifier());
            assertThat(expectedRegistration).isPresent();
            assertThat(expectedRegistration.get().getExposedEpochs()).isEmpty();
        }
    }

    @Test
    void process_contact_succeeds_and_when_some_of_the_epochs_are_different_logs_and_discard() throws Exception {
        // Given
        var now = clock.now();

        var registration = givenRegistrationExistsForUser("user___1");
        var messageDetails = generateHelloMessagesStartingAndDuringSeconds(now, 30);
        messageDetails.addAll(generateHelloMessageWithDivergenceBetweenMessageAndRegistration(now));

        givenContactExistForUser(
                "user___1", c -> c.messageDetails(messageDetails)
        );

        givenCryptoServerIdA(ByteString.copyFrom("user___1".getBytes()));

        try (final var logCaptor = LogCaptor.forClass(ContactProcessingService.class)) {
            // When
            contactProcessingService.performs();

            // Then
            assertThatLogsMatchingRegex(logCaptor.getWarnLogs(), DIVERGENT_EPOCHIDS, 1);
            assertThat(this.contactService.findAll()).isEmpty();
            Optional<Registration> expectedRegistration = registrationService
                    .findById(registration.getPermanentIdentifier());
            assertThat(expectedRegistration).isPresent();
            assertThat(expectedRegistration.get().getExposedEpochs())
                    .isNotEmpty()
                    .hasSize(1);
        }
    }
}
