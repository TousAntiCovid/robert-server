package fr.gouv.stopc.robertserver.batch;

import fr.gouv.stopc.robertserver.batch.test.CountryCode;
import fr.gouv.stopc.robertserver.batch.test.IntegrationTest;
import fr.gouv.stopc.robertserver.common.RobertClock;
import fr.gouv.stopc.robertserver.database.model.EpochExposition;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static fr.gouv.stopc.robert.server.common.utils.TimeUtils.EPOCH_DURATION_SECS;
import static fr.gouv.stopc.robertserver.batch.test.LogbackManager.assertThatInfoLogs;
import static fr.gouv.stopc.robertserver.batch.test.LogbackManager.assertThatWarnLogs;
import static fr.gouv.stopc.robertserver.batch.test.MessageMatcher.assertThatContactsToProcess;
import static fr.gouv.stopc.robertserver.batch.test.MessageMatcher.assertThatEpochExpositionsForIdA;
import static fr.gouv.stopc.robertserver.batch.test.MongodbManager.givenPendingContact;
import static fr.gouv.stopc.robertserver.batch.test.MongodbManager.givenRegistrationExistsForIdA;
import static java.time.temporal.ChronoUnit.SECONDS;

@IntegrationTest
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class ContactProcessingServiceTest {

    private final RobertClock clock;

    private final JobLauncherTestUtils jobLauncher;

    private final int HELLO_MESSAGE_TIME_STAMP_TOLERANCE = 180;

    @SneakyThrows
    private void runRobertBatchJob() {
        jobLauncher.launchJob();
    }

    @Test
    void epoch_risk_0_when_not_enough_messages() {
        var now = clock.now();

        givenRegistrationExistsForIdA("user___1");

        givenPendingContact()
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
        givenPendingContact()
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
        final var exceedTimeStampTolerance = now.plus(HELLO_MESSAGE_TIME_STAMP_TOLERANCE + 1, SECONDS);
        final var divergingTimeCollected = now
                .plus(EPOCH_DURATION_SECS * 2L, SECONDS);

        givenRegistrationExistsForIdA("user___1");

        givenPendingContact()
                .idA("user___1")
                // Bad HMD with timeCollectedOnDevice exceeded time tolerance
                .buildHelloMessage(exceedTimeStampTolerance, now, now)
                // Bad HMD with timeCollectedOnDevice which is diverging from Registration
                // EpochId
                .buildHelloMessage(divergingTimeCollected, divergingTimeCollected, now)
                // Good helloMessageDetails
                .withValidHelloMessageAt(now, 70)
                .build();

        // When
        runRobertBatchJob();

        // Then
        // Check that the registration contains one exposed epoch for the computation of
        // the good ones
        assertThatEpochExpositionsForIdA("user___1")
                .containsOnlyOnce(
                        new EpochExposition(now.asEpochId(), List.of(1.0))
                );

        assertThatWarnLogs()
                .containsOnlyOnce(
                        String.format(
                                "Time tolerance was exceeded: |%d (HELLO) vs %d (receiving device)| > 180; discarding HELLO message",
                                now.as16LessSignificantBits(), exceedTimeStampTolerance.as16LessSignificantBits()
                        )
                );
        assertThatWarnLogs()
                .containsOnlyOnce(
                        String.format(
                                "Epochid from message %d  vs epochid from ebid  %d > 1 (tolerance); discarding HELLO message",
                                divergingTimeCollected.asEpochId(), now.asEpochId()
                        )
                );

        assertThatContactsToProcess().isEmpty();
    }

    @Test
    void logs_and_discard_messages_when_bad_encrypted_country_code_rejection() {
        // Given
        final var now = clock.now();

        givenRegistrationExistsForIdA("user___1");

        givenPendingContact()
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

        givenPendingContact()
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

        givenPendingContact()
                .idA("user___1")
                .withValidHelloMessageAt(now, 3)
                .build();

        // When
        runRobertBatchJob();

        // Then
        assertThatInfoLogs()
                .containsOnlyOnce(
                        String.format(
                                "No identity exists for id_A %s extracted from ebid, discarding contact",
                                Arrays.toString("user___1".getBytes())
                        )
                );
        assertThatContactsToProcess().isEmpty();
    }

    @Test
    void logs_and_discard_when_all_messages_have_been_rejected() {
        // Given
        givenRegistrationExistsForIdA("user___1");
        final var now = clock.now();
        final var exceededTime = now.plus(HELLO_MESSAGE_TIME_STAMP_TOLERANCE + 1, SECONDS);

        givenPendingContact()
                .idA("user___1")
                .buildHelloMessage(exceededTime, now, now)
                .build();

        // When
        runRobertBatchJob();

        // Then
        assertThatInfoLogs()
                .containsOnlyOnce("Contact did not contain any valid messages; discarding contact");
        assertThatEpochExpositionsForIdA("user___1").isEmpty();
        assertThatContactsToProcess().isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = { "PT3M", "PT-3M", "PT1M", "PT-1M" })
    void no_logs_and_no_discard_when_hello_message_timestamp_tolerance_is_not_exceeded(final Duration receptionDelay) {
        // Given
        final var now = clock.now();
        final var exceededTime = now.plus(receptionDelay);
        givenRegistrationExistsForIdA("user___1");

        givenPendingContact()
                .idA("user___1")
                .buildHelloMessage(exceededTime, now, now) // param ici
                .build();

        // When
        runRobertBatchJob();

        // Then
        assertThatWarnLogs()
                .doesNotContain(
                        String.format(
                                "Time tolerance was exceeded: |%d (HELLO) vs %d (receiving device)| > 180; discarding HELLO message",
                                now.as16LessSignificantBits(), exceededTime.as16LessSignificantBits()
                        )
                );
        assertThatEpochExpositionsForIdA("user___1")
                .containsOnlyOnce(new EpochExposition(now.asEpochId(), List.of(0.0)));
        assertThatContactsToProcess().isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = { "PT3M1S", "PT-3M-1S", "PT15M", "PT-15M" })
    void logs_and_discard_when_hello_message_timestamp_tolerance_is_exceeded(final Duration receptionDelay) {
        // Given
        final var now = clock.now();
        final var exceededTime = now.plus(receptionDelay);
        givenRegistrationExistsForIdA("user___1");

        givenPendingContact()
                .idA("user___1")
                .buildHelloMessage(exceededTime, now, now)
                .build();

        // When
        runRobertBatchJob();

        // Then
        assertThatWarnLogs()
                .containsOnlyOnce(
                        String.format(
                                "Time tolerance was exceeded: |%d (HELLO) vs %d (receiving device)| > 180; discarding HELLO message",
                                now.as16LessSignificantBits(), exceededTime.as16LessSignificantBits()
                        )
                );
        assertThatEpochExpositionsForIdA("user___1").isEmpty();
        assertThatContactsToProcess().isEmpty();
    }

    @Test
    void logs_and_discard_when_the_epochs_are_different() {
        // Given
        final var now = clock.now();
        final var divergingTimeCollected = now.plus(EPOCH_DURATION_SECS * 2L, SECONDS);
        givenRegistrationExistsForIdA("user___1");

        givenPendingContact()
                .idA("user___1")
                // Add HelloMessage with divergence between message and registration
                .buildHelloMessage(divergingTimeCollected, divergingTimeCollected, now)
                .build();

        // When
        runRobertBatchJob();

        // Then
        assertThatWarnLogs().containsOnlyOnce(
                String.format(
                        "Epochid from message %d  vs epochid from ebid  %d > 1 (tolerance); discarding HELLO message",
                        divergingTimeCollected.asEpochId(), now.asEpochId()
                )
        );
        // Note : DIVERGENT_EPOCHIDS il y a des espaces en trop dans les logs de cette
        // version applicative
        assertThatEpochExpositionsForIdA("user___1").isEmpty();
        assertThatContactsToProcess().isEmpty();
    }
}
