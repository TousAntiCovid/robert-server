package fr.gouv.stopc.robert.server.batch.service;

import com.google.protobuf.ByteString;
import fr.gouv.stopc.robert.server.batch.IntegrationTest;
import fr.gouv.stopc.robert.server.batch.manager.GrpcMockManager;
import fr.gouv.stopc.robert.server.batch.manager.MetricsManager;
import fr.gouv.stopc.robertserver.database.model.Registration;
import fr.gouv.stopc.robertserver.database.service.ContactService;
import fr.gouv.stopc.robertserver.database.service.impl.RegistrationService;
import lombok.RequiredArgsConstructor;
import nl.altindag.log.LogCaptor;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestExecutionListeners;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static fr.gouv.stopc.robert.server.batch.manager.GrpcMockManager.*;
import static fr.gouv.stopc.robert.server.batch.manager.MetricsManager.assertThatTimerMetricIncrement;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.context.TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS;

@IntegrationTest
@TestExecutionListeners(listeners = {
        GrpcMockManager.class,
        MetricsManager.class
}, mergeMode = MERGE_WITH_DEFAULTS)
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class ContactProcessingServiceTest {

    private final ContactProcessingService contactProcessingService;

    private final ContactService contactService;

    private final RegistrationService registrationService;

    private TestContext testContext;

    private final String TIME_TOLERANCE_REGEX = "(Time tolerance was exceeded: \\|[\\d]{5} \\(HELLO\\) vs [\\d]{5} \\(receiving device\\)\\| > 180; discarding HELLO message)";

    private final String DIVERGENT_EPOCHIDS = "Epochid from message [\\d]{5} vs epochid from ebid [\\d]{5} > 1 \\(tolerance\\); discarding HELLO message";

    @BeforeEach
    public void before(@Autowired TestContext testContext) {
        this.testContext = testContext;
        givenCryptoServerEpochId(this.testContext.currentEpochId);
    }

    @AfterEach
    public void afterAll() {
        this.contactService.deleteAll();
        this.registrationService.deleteAll();
    }

    @Test
    void metricIsIncrementedWhenProcessPerformed() {
        contactProcessingService.performs();
        assertThatTimerMetricIncrement("robert.batch", "operation", "CONTACT_SCORING_STEP").isEqualTo(1L);
    }

    @Test
    void process_two_contacts_with_aggregated_score_above_threshold_yields_risk_succeeds() throws Exception {
        // Given
        var registration = this.testContext.acceptableRegistrationWithExistingScoreBelowThreshold();
        var contact = this.testContext.acceptableContactWithoutHelloMessage(registration);
        contact = this.testContext
                .addHelloMessagesWithTotalScoreUpperThanThreshold(contact, registration.getPermanentIdentifier());

        assertThat(this.contactService.findAll()).isNotEmpty().hasSize(1);

        givenCryptoServerIdA(ByteString.copyFrom(registration.getPermanentIdentifier()));

        // When
        contactProcessingService.performs();

        // Then
        assertThat(this.contactService.findAll()).isEmpty();
        Optional<Registration> expectedRegistration = registrationService
                .findById(registration.getPermanentIdentifier());
        assertThat(expectedRegistration).isPresent();
        assertThat(expectedRegistration.get().getExposedEpochs())
                .isNotEmpty()
                .hasSize(2);
    }

    @Test
    void process_two_contacts_with_aggregated_score_below_threshold_does_not_yield_risk_succeeds() throws Exception {
        // Given
        var registration = this.testContext.acceptableRegistrationWithExistingScoreBelowThreshold();
        var contact = this.testContext.acceptableContactWithoutHelloMessage(registration);
        contact = this.testContext
                .addHelloMessagesWithTotalScoreUnderThreshold(contact, registration.getPermanentIdentifier());

        assertThat(this.contactService.findAll()).isNotEmpty();
        assertThat(this.contactService.findAll()).hasSize(1);

        givenCryptoServerIdA(ByteString.copyFrom(registration.getPermanentIdentifier()));

        // When
        contactProcessingService.performs();

        // Then
        assertThat(this.contactService.findAll()).isEmpty();
        Optional<Registration> expectedRegistration = registrationService
                .findById(registration.getPermanentIdentifier());
        assertThat(expectedRegistration).isPresent();
        assertThat(expectedRegistration.get().getExposedEpochs())
                .isNotEmpty()
                .hasSize(2);
    }

    @Test
    void process_contact_succeeds_when_has_at_least_one_hello_message_valid() throws Exception {
        var registration = this.testContext.acceptableRegistration();
        var contact = this.testContext.generateAcceptableContactForRegistration(registration);
        contact = this.testContext
                .addBadHelloMessageWithExcedeedTimeToleranceToContact(contact, registration.getPermanentIdentifier());

        givenCryptoServerIdA(ByteString.copyFrom(registration.getPermanentIdentifier()));

        try (final var logCaptor = LogCaptor.forClass(ContactProcessingService.class)) {
            // When
            contactProcessingService.performs();

            // Then : Check that there is one bad contact and that the final registration
            // contains one exposed epoch for the computation of the good ones
            assertThatLogsMatchingRegex(logCaptor.getWarnLogs(), TIME_TOLERANCE_REGEX, 1);
        }
        Optional<Registration> expectedRegistration = registrationService
                .findById(registration.getPermanentIdentifier());
        assertThat(expectedRegistration).isPresent();
        assertThat(expectedRegistration.get().getExposedEpochs())
                .isNotEmpty()
                .hasSize(1);
        assertThat(this.contactService.findAll()).isEmpty();
    }

    @Test
    void process_contact_when_the_contact_is_valid_succeeds() throws Exception {
        var registration = this.testContext.acceptableRegistrationWithExistingScoreBelowThreshold();
        var contact = this.testContext.generateAcceptableContactForRegistration(registration);
        contact = this.testContext.addValidHelloMessage(contact, registration.getPermanentIdentifier());

        givenCryptoServerIdA(ByteString.copyFrom(registration.getPermanentIdentifier()));

        // When
        contactProcessingService.performs();

        // Then
        Optional<Registration> expectedRegistration = registrationService
                .findById(registration.getPermanentIdentifier());
        assertThat(expectedRegistration).isPresent();
        assertThat(expectedRegistration.get().getExposedEpochs())
                .isNotEmpty()
                .hasSize(2);
        assertThat(this.contactService.findAll()).isEmpty();
    }

    @Test
    void process_contact_with_a_bad_encrypted_country_code_fails() throws Exception {
        // Given
        var registration = this.testContext.acceptableRegistration();
        var contact = this.testContext.contactWithBadCountryCode(registration.getPermanentIdentifier());

        givenCryptoServerIdA(ByteString.copyFrom(registration.getPermanentIdentifier()));
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
    void process_contact_with_no_messages_fails() throws Exception {
        // Given
        var registration = this.testContext.acceptableRegistration();
        var contact = this.testContext.acceptableContactWithoutHelloMessage(registration);

        givenCryptoServerIdA(ByteString.copyFrom(registration.getPermanentIdentifier()));

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
    void process_contact_with_non_existent_registration_fails() throws Exception {
        // Given
        var registration = this.testContext.acceptableRegistration();
        var contact = this.testContext.generateAcceptableContactForRegistration(registration);

        givenCryptoServerIdA(ByteString.copyFrom(registration.getPermanentIdentifier()));

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
    void process_contact_logs_when_hello_message_timestamp_is_exceeded() throws Exception {
        // Given
        var registration = this.testContext.acceptableRegistration();
        var contact = this.testContext.acceptableContactWithoutHelloMessage(registration);
        contact = this.testContext
                .addHelloMessageExcededTimestamptolerance(contact, registration.getPermanentIdentifier());

        givenCryptoServerIdA(ByteString.copyFrom(registration.getPermanentIdentifier()));

        try (final var logCaptor = LogCaptor.forClass(ContactProcessingService.class)) {
            contactProcessingService.performs();

            assertThatLogsMatchingRegex(logCaptor.getWarnLogs(), TIME_TOLERANCE_REGEX, 1);

            assertThat(logCaptor.getInfoLogs())
                    .contains("Contact did not contain any valid messages; discarding contact");

            assertThat(this.contactService.findAll()).isEmpty();
        }

        assertThat(this.contactService.findAll()).isEmpty();
    }

    @Test
    void process_contact_logs_when_the_epochs_are_different() throws Exception {
        // Given
        var registration = this.testContext.acceptableRegistration();
        var contact = this.testContext.acceptableContactWithoutHelloMessage(registration);
        contact = this.testContext.addHelloMessageWithBadEbidCoherence(contact, registration.getPermanentIdentifier());

        givenCryptoServerIdA(ByteString.copyFrom(registration.getPermanentIdentifier()));

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
        var registration = this.testContext.acceptableRegistration();
        var contact = this.testContext.acceptableContactWithoutHelloMessage(registration);
        contact = this.testContext.addValidHelloMessage(contact, registration.getPermanentIdentifier());
        contact = this.testContext.addHelloMessageWithBadEbidCoherence(contact, registration.getPermanentIdentifier());

        givenCryptoServerIdA(ByteString.copyFrom(registration.getPermanentIdentifier()));

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

    private void assertThatLogsMatchingRegex(List<String> logs, String regex, int times) {
        final Condition<String> rowMatchingRegex = new Condition<String>(value -> value.matches(regex), regex);
        assertThat(logs).as("Number of logs matching the regular expression").haveExactly(times, rowMatchingRegex);
    }
}
