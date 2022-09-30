package fr.gouv.stopc.robertserver.batch;

import fr.gouv.stopc.robert.server.common.service.RobertClock;
import fr.gouv.stopc.robertserver.batch.test.IntegrationTest;
import fr.gouv.stopc.robertserver.database.model.EpochExposition;
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
    void epoch_risk_0_when_not_enough_messages() {
        var now = clock.now();

        givenRegistrationExistsForIdA("user___1");

        givenGivenPendingContact()
                .idA("user___1")
                .withValidHelloMessageAt(now, 1)
                .build();

        // When
        runRobertBatchJob();

        // Then
        assertThatEpochExpositionsForIdA("user___1")
                .containsOnlyOnce(
                        new EpochExposition(now.asEpochId(), List.of(0.0))
                );

        assertThatContactsToProcess().isEmpty();
    }

    @Test
    void add_new_risk_to_existing_exposed_epochs_risks() {
        final var now = clock.now();
        final var twoDaysAgo = now.minus(2, ChronoUnit.DAYS);
        final var fiveDaysAgo = now.minus(5, ChronoUnit.DAYS);

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

        // Should raise an exposedEpoch risk
        givenGivenPendingContact()
                .idA("user___1")
                .withValidHelloMessageAt(twoDaysAgo, 70)
                .build();

        // When
        runRobertBatchJob();

        // Then
        assertThatEpochExpositionsForIdA("user___1")
                .containsOnlyOnce(
                        new EpochExposition(fiveDaysAgo.asEpochId(), List.of(4.3)),
                        new EpochExposition(twoDaysAgo.asEpochId(), List.of(3.0, 1.0))
                );
    }

    @Test
    void remove_invalid_hello_messages() {
        final var now = clock.now();

        givenRegistrationExistsForIdA("user___1");

        givenGivenPendingContact()
                .idA("user___1")
                .withHelloMessageWithBadTimeCollectedOnDevice(now)
                .withHelloMessageWithDivergentTimeCollected(now)
                .withValidHelloMessageAt(now, 70)
                .build();

        // When
        runRobertBatchJob();

        // Then : Check that the registration contains one exposed epoch for the
        // computation of the good ones
        assertThatLogsMatchingRegex(assertThatWarnLogs(), TIME_TOLERANCE_REGEX, 1);
        assertThatLogsMatchingRegex(assertThatWarnLogs(), DIVERGENT_EPOCHIDS, 1);

        assertThatEpochExpositionsForIdA("user___1")
                .containsOnlyOnce(
                        new EpochExposition(now.asEpochId(), List.of(1.0))
                );

        assertThatContactsToProcess().isEmpty();
    }

    @Test
    void logs_and_discard_messages_when_bad_encrypted_country_code_rejection() {
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
        assertThatEpochExpositionsForIdA("user___1").isEmpty();
        assertThatContactsToProcess().isEmpty();
    }

    @Test
    void logs_and_discard_when_contact_has_no_messages() {
        // Given
        givenRegistrationExistsForIdA("user___1");

        givenGivenPendingContact()
                .idA("user___1")
                .build();

        // When
        runRobertBatchJob();

        // Then
        assertThatInfoLogs()
                .containsOnlyOnce("No messages in contact, discarding contact");
        assertThatEpochExpositionsForIdA("user___1").isEmpty();
        assertThatContactsToProcess().isEmpty();
    }

    @Test
    void logs_and_discard_when_registration_does_not_exist() {
        // Given
        final var now = clock.now();
        final var registration = givenRegistrationExistsForIdA("user___1");

        givenGivenPendingContact()
                .idA("user___1")
                .withValidHelloMessageAt(now, 3)
                .build();

        this.registrationService.delete(registration);

        // When
        runRobertBatchJob();

        // Then
        assertThatInfoLogs()
                .containsOnlyOnce(
                        String.format(
                                "No identity exists for id_A %s extracted from ebid, discarding contact",
                                Arrays.toString(registration.getPermanentIdentifier())
                        )
                );
        assertThatContactsToProcess().isEmpty();
    }

    @Test
    void logs_and_discard_when_all_messages_have_been_rejected() {
        // Given
        givenRegistrationExistsForIdA("user___1");

        givenGivenPendingContact()
                .idA("user___1")
                .withHelloMessageWithBadTimeCollectedOnDevice(clock.now())
                .build();

        // When
        runRobertBatchJob();

        // Then
        assertThatInfoLogs()
                .containsOnlyOnce("Contact did not contain any valid messages; discarding contact");
        assertThatEpochExpositionsForIdA("user___1").isEmpty();
        assertThatContactsToProcess().isEmpty();
    }

    @Test
    void logs_and_discard_when_hello_message_timestamp_is_exceeded() {
        // Given
        givenRegistrationExistsForIdA("user___1");

        givenGivenPendingContact()
                .idA("user___1")
                .withHelloMessageWithBadTimeCollectedOnDevice(clock.now())
                .build();

        runRobertBatchJob();

        assertThatLogsMatchingRegex(assertThatWarnLogs(), TIME_TOLERANCE_REGEX, 1);
        assertThatEpochExpositionsForIdA("user___1").isEmpty();
        assertThatContactsToProcess().isEmpty();
    }

    @Test
    void logs_and_discard_when_the_epochs_are_different() {
        // Given
        givenRegistrationExistsForIdA("user___1");

        // Add HelloMessage with divergence between message and registration
        givenGivenPendingContact()
                .idA("user___1")
                .withHelloMessageWithDivergentTimeCollected(clock.now())
                .build();

        // When
        runRobertBatchJob();

        // Then
        assertThatLogsMatchingRegex(assertThatWarnLogs(), DIVERGENT_EPOCHIDS, 1);
        // Note : DIVERGENT_EPOCHIDS il y a des espaces en trop dans les logs de cette
        // version applicative
        assertThatEpochExpositionsForIdA("user___1").isEmpty();
        assertThatContactsToProcess().isEmpty();
    }
}
