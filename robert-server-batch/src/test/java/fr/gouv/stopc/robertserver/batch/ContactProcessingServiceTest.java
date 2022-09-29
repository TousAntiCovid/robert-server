package fr.gouv.stopc.robertserver.batch;

import fr.gouv.stopc.robert.server.common.service.RobertClock;
import fr.gouv.stopc.robertserver.batch.test.IntegrationTest;
import fr.gouv.stopc.robertserver.database.model.EpochExposition;
import fr.gouv.stopc.robertserver.database.model.Registration;
import fr.gouv.stopc.robertserver.database.service.IRegistrationService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static fr.gouv.stopc.robertserver.batch.test.LogbackManager.*;
import static fr.gouv.stopc.robertserver.batch.test.MongodbManager.*;
import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class ContactProcessingServiceTest {

    private final IRegistrationService registrationService;

    private final RobertClock clock;

    private final String TIME_TOLERANCE_REGEX = "(Time tolerance was exceeded: \\|[\\d]{1,} \\(HELLO\\) vs [\\d]{1,} \\(receiving device\\)\\| > 180; discarding HELLO message)";

    private final String DIVERGENT_EPOCHIDS = "Epochid from message [\\d]{1,}  vs epochid from ebid  [\\d]{1,} > 1 \\(tolerance\\); discarding HELLO message";

    private final JobLauncherTestUtils jobLauncher;

    @SneakyThrows
    private void runRobertBatchJob() {
        jobLauncher.launchJob();
    }

    @Test
    void process_two_helloMessages_and_concat_risk_with_existing_registration_exposed_epochs_risks() {
        var twoDaysAgo = clock.now().minus(2, ChronoUnit.DAYS);
        var fiveDaysAgo = clock.now().minus(5, ChronoUnit.DAYS);

        givenRegistrationExistsForIdA(
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

        givenGivenPendingContact()
                .idA("user___1")
                .withValidHelloMessageAt(twoDaysAgo, 2)
                .build();

        // When
        runRobertBatchJob();

        // Then
        assertThatContactsToProcess().isEmpty();
        // TODO : ===> faire un truc pour simplifier
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

        givenRegistrationExistsForIdA("user___1");

        givenGivenPendingContact()
                .idA("user___1")
                .withHelloMessageTimeCollectedExceededMaxTimeStampTolerance(clock.now())
                .withValidHelloMessageAt(now, 3)
                .build();

        // When
        runRobertBatchJob();

        // Then : Check that there is one bad contact in logs and that the final
        // registration
        // contains one exposed epoch for the computation of the good ones
        assertThatLogsMatchingRegex(assertThatWarnLogs(), TIME_TOLERANCE_REGEX, 1);

        assertThatRegistrationForUser("user___1")
                .extracting(Registration::getExposedEpochs)
                .asList()
                .hasSize(1);

        assertThatContactsToProcess().isEmpty();
    }

    @Test
    void process_contact_when_the_contact_is_valid_succeeds() {
        var now = clock.now();

        givenRegistrationExistsForIdA("user___1");

        givenGivenPendingContact()
                .idA("user___1")
                .withValidHelloMessageAt(now, 12)
                .build();

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
        final var now = clock.now();

        givenRegistrationExistsForIdA("user___1");

        givenGivenPendingContact()
                .idA("user___1")
                .countryCode(CountryCode.GERMANY)
                .withValidHelloMessageAt(now, 3)
                .build();

        // When
        runRobertBatchJob();

        // Then
        assertThatInfoLogs()
                .containsOnlyOnce(
                        String.format(
                                "Country code [%d] is not managed by this server ([33])",
                                CountryCode.GERMANY.getNumericCode()
                        )
                );
        assertThatContactsToProcess().isEmpty();
    }

    @Test
    void process_contact_with_no_messages_fails() {
        // Given
        var now = clock.now();
        givenRegistrationExistsForIdA("user___1");

        givenGivenPendingContact()
                .idA("user___1")
                .build();

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

        var registration = givenRegistrationExistsForIdA("user___1");

        givenGivenPendingContact()
                .idA("user___1")
                .withValidHelloMessageAt(now, 3)
                .build();

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
        givenRegistrationExistsForIdA("user___1");

        givenGivenPendingContact()
                .idA("user___1")
                .withHelloMessageTimeCollectedExceededMaxTimeStampTolerance(clock.now())
                .build();

        runRobertBatchJob();

        assertThatLogsMatchingRegex(assertThatWarnLogs(), TIME_TOLERANCE_REGEX, 1);

        assertThatInfoLogs()
                .contains("Contact did not contain any valid messages; discarding contact");

        assertThatContactsToProcess().isEmpty();
    }

    @Test
    void process_contact_logs_when_the_epochs_are_different() {
        // Given
        givenRegistrationExistsForIdA("user___1");

        // Add HelloMessage with divergence between message and registration
        givenGivenPendingContact()
                .idA("user___1")
                .withHelloMessageTimeCollectedIsDivergentFromRegistrationEpochId(clock.now())
                .build();

        // When
        runRobertBatchJob();

        // Then
        assertThatLogsMatchingRegex(assertThatWarnLogs(), DIVERGENT_EPOCHIDS, 1);
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

        givenRegistrationExistsForIdA("user___1");

        givenGivenPendingContact()
                .idA("user___1")
                .withHelloMessageTimeCollectedIsDivergentFromRegistrationEpochId(clock.now())
                .withValidHelloMessageAt(now, 3)
                .build();

        // When
        runRobertBatchJob();

        // Then
        assertThatLogsMatchingRegex(assertThatWarnLogs(), DIVERGENT_EPOCHIDS, 1);
        assertThatContactsToProcess().isEmpty();

        assertThatRegistrationForUser("user___1")
                .extracting(Registration::getExposedEpochs)
                .asList()
                .hasSize(1);
    }
}
