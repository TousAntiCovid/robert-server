package fr.gouv.stopc.robertserver.batch.test.service;

import com.google.protobuf.ByteString;
import fr.gouv.stopc.robert.server.common.service.RobertClock;
import fr.gouv.stopc.robertserver.batch.test.IntegrationTest;
import fr.gouv.stopc.robertserver.database.model.EpochExposition;
import fr.gouv.stopc.robertserver.database.model.Registration;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static fr.gouv.stopc.robert.server.common.service.RobertClock.ROBERT_EPOCH;
import static fr.gouv.stopc.robertserver.batch.test.GrpcMockManager.givenCryptoServerCountryCode;
import static fr.gouv.stopc.robertserver.batch.test.LogbackManager.assertThatInfoLogs;
import static fr.gouv.stopc.robertserver.batch.test.LogbackManager.assertThatLogsMatchingRegex;
import static fr.gouv.stopc.robertserver.batch.test.MongodbManager.*;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.within;

@IntegrationTest
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class RiskEvaluationServiceTest {

    private final JobLauncherTestUtils jobLauncher;

    private final RobertClock clock;

    @SneakyThrows
    private void runRobertBatchJob() {
        jobLauncher.launchJob();
    }

    @Test
    void score_and_process_risks_with_a_bad_encrypted_country_code_should_not_update_registration() {
        var now = clock.now();

        // Given
        givenRegistrationExistsForUser("user___1");
        givenPendingContact(
                "user___1", helloMessagesBuilder -> helloMessagesBuilder.addAt(now, now.plus(120, SECONDS))
        );

        // Set bad country code
        givenCryptoServerCountryCode(ByteString.copyFrom(new byte[] { (byte) 0xff }));

        // When
        runRobertBatchJob();

        // Then
        assertThatContactsToProcess().isEmpty();

        assertThatRegistrationForUser("user___1")
                .extracting(Registration::isAtRisk)
                .asInstanceOf(InstanceOfAssertFactories.BOOLEAN)
                .as("Registration risk")
                .isFalse();

        assertThatRegistrationForUser("user___1")
                .extracting(Registration::getExposedEpochs)
                .asList()
                .isEmpty();
    }

    @Test
    void score_and_process_risk_when_scores_equals_zero_should_not_be_at_risk() {
        var twoDaysAgo = clock.now().minus(2, ChronoUnit.DAYS);
        var fiveDaysAgo = clock.now().minus(5, ChronoUnit.DAYS);

        givenRegistrationExistsForUser(
                "user___1", r -> r
                        .exposedEpochs(
                                List.of(
                                        EpochExposition.builder()
                                                .epochId(twoDaysAgo.asEpochId())
                                                .expositionScores(Collections.singletonList(0.0))
                                                .build(),
                                        EpochExposition.builder()
                                                .epochId(fiveDaysAgo.asEpochId())
                                                .expositionScores(Collections.singletonList(0.0))
                                                .build()
                                )
                        )
                        .outdatedRisk(true)
        );

        // When
        runRobertBatchJob();

        // Then
        assertThatRegistrationForUser("user___1")
                .extracting(Registration::isAtRisk)
                .asInstanceOf(InstanceOfAssertFactories.BOOLEAN)
                .as("Registration risk")
                .isFalse();
    }

    @Test
    void score_and_process_risks_when_recent_exposed_epoch_score_greater_than_risk_threshold_should_be_at_risk() {

        // Given Registration With Existing Score Above Threshold
        var twoDaysAgo = clock.now().minus(2, ChronoUnit.DAYS);

        givenRegistrationExistsForUser(
                "user___1", r -> r
                        .exposedEpochs(
                                List.of(
                                        EpochExposition.builder()
                                                .epochId(twoDaysAgo.asEpochId())
                                                .expositionScores(Collections.singletonList(20.0))
                                                .build()

                                )
                        )
                        .outdatedRisk(true)
        );

        // When
        runRobertBatchJob();

        // Then
        assertThatRegistrationForUser("user___1")
                .extracting(Registration::isAtRisk)
                .asInstanceOf(InstanceOfAssertFactories.BOOLEAN)
                .as("Registration risk")
                .isTrue();

        final var RISK_DETECTED = "Risk detected\\. Aggregated risk since [\\d]{0,}: [\\d]{0,}\\.[\\d]{0,} greater than threshold [\\d]{0,}\\.[\\d]{0,}";
        assertThatLogsMatchingRegex(assertThatInfoLogs(), RISK_DETECTED, 1);
    }

    @Test
    void score_and_process_risks_when_epoch_scores_less_than_risk_threshold_should_not_be_at_risk() {
        // Given Registration With Existing Score Below Threshold
        var twoDaysAgo = clock.now().minus(2, ChronoUnit.DAYS);

        givenRegistrationExistsForUser(
                "user___1", r -> r
                        .exposedEpochs(
                                List.of(
                                        EpochExposition.builder()
                                                .epochId(twoDaysAgo.asEpochId())
                                                .expositionScores(Collections.singletonList(3.0))
                                                .build(),
                                        EpochExposition.builder()
                                                .epochId(twoDaysAgo.asEpochId())
                                                .expositionScores(Collections.singletonList(4.3))
                                                .build()

                                )
                        )
                        .outdatedRisk(true)
        );

        // When
        runRobertBatchJob();

        // Then
        assertThatRegistrationForUser("user___1")
                .extracting(Registration::isAtRisk)
                .asInstanceOf(InstanceOfAssertFactories.BOOLEAN)
                .as("Registration risk")
                .isFalse();
    }

    @Test
    void last_exposition_at_risk_must_be_updated_when_epoch_exposition_giving_score_at_risk() {
        var now = clock.now();
        var yesterday = now.minus(1, ChronoUnit.DAYS);
        // Given
        givenRegistrationExistsForUser(
                "user___1", r -> r
                        .exposedEpochs(
                                List.of(
                                        EpochExposition.builder()
                                                .epochId(now.asEpochId())
                                                .expositionScores(Collections.singletonList(1.0))
                                                .build(),
                                        EpochExposition.builder()
                                                .epochId(yesterday.asEpochId())
                                                .expositionScores(Collections.singletonList(20.0))
                                                .build()

                                )
                        )
                        .outdatedRisk(true)
        );

        // When
        runRobertBatchJob();

        // Then
        assertThatRegistrationForUser("user___1")
                .extracting(Registration::isAtRisk)
                .asInstanceOf(InstanceOfAssertFactories.BOOLEAN)
                .as("Registration risk")
                .isTrue();

        assertThatRegistrationForUser("user___1")
                .extracting(Registration::getLatestRiskEpochAsInstant)
                .asInstanceOf(InstanceOfAssertFactories.INSTANT)
                .as("Last risk update")
                .isCloseTo(now.asInstant(), within(1, ROBERT_EPOCH));

        assertThatRegistrationForUser("user___1")
                .extracting(Registration::getLastContactTimestampAsInstant)
                .asInstanceOf(InstanceOfAssertFactories.INSTANT)
                .as("Last contact timestamp")
                .isCloseTo(now.asInstant(), within(1, DAYS));

    }

    @Test
    void risk_not_detected_when_expositions_giving_score_not_at_risk() {
        var now = clock.now();
        var twoDaysAgo = now.minus(2, ChronoUnit.DAYS);

        givenRegistrationExistsForUser(
                "user___1", r -> r
                        .exposedEpochs(
                                List.of(
                                        EpochExposition.builder()
                                                .epochId(twoDaysAgo.asEpochId())
                                                .expositionScores(Arrays.asList(1.0, 12.5))
                                                .build()

                                )
                        )
                        .outdatedRisk(true)
        );

        // When
        runRobertBatchJob();

        // Then
        assertThatRegistrationForUser("user___1")
                .extracting(Registration::isAtRisk)
                .asInstanceOf(InstanceOfAssertFactories.BOOLEAN)
                .as("Registration risk")
                .isFalse();

        assertThatRegistrationForUser("user___1")
                .extracting(Registration::getLatestRiskEpoch)
                .asInstanceOf(InstanceOfAssertFactories.INTEGER)
                .as("Last risk update")
                .isZero();

        assertThatRegistrationForUser("user___1")
                .extracting(Registration::getLastContactTimestamp)
                .asInstanceOf(InstanceOfAssertFactories.LONG)
                .as("Last contact timestamp")
                .isZero();
    }

    @Test
    void risk_detected_when_single_epoch_exposition_giving_score_at_risk() {
        var now = clock.now();
        var twoDaysAgo = now.minus(2, ChronoUnit.DAYS);

        givenRegistrationExistsForUser(
                "user___1", r -> r
                        .exposedEpochs(
                                List.of(
                                        EpochExposition.builder()
                                                .epochId(twoDaysAgo.asEpochId())
                                                .expositionScores(Arrays.asList(10.0, 2.0, 1.0, 4.3))
                                                .build()

                                )
                        )
                        .outdatedRisk(true)
        );

        // When
        runRobertBatchJob();

        // Then
        assertThatRegistrationForUser("user___1")
                .extracting(Registration::isAtRisk)
                .asInstanceOf(InstanceOfAssertFactories.BOOLEAN)
                .as("Registration risk")
                .isTrue();

        assertThatRegistrationForUser("user___1")
                .extracting(Registration::getLatestRiskEpochAsInstant)
                .asInstanceOf(InstanceOfAssertFactories.INSTANT)
                .as("Last risk update")
                .isCloseTo(now.asInstant(), within(1, ROBERT_EPOCH));

        assertThatRegistrationForUser("user___1")
                .extracting(Registration::getLastContactTimestampAsInstant)
                .asInstanceOf(InstanceOfAssertFactories.INSTANT)
                .as("Last contact timestamp")
                .isCloseTo(twoDaysAgo.asInstant(), within(1, DAYS));
    }

    @Test
    void last_contact_date_is_updated_when_already_at_risk_and_new_contact_at_risk_with_date_greater_than_current_last_contact_date() {
        var now = clock.now();
        var threeDaysAgo = now.minus(3, ChronoUnit.DAYS);
        var fiveDaysAgo = now.minus(5, ChronoUnit.DAYS);

        givenRegistrationExistsForUser(
                "user___1", r -> r
                        .exposedEpochs(
                                List.of(
                                        EpochExposition.builder()
                                                .epochId(threeDaysAgo.asEpochId())
                                                .expositionScores(Arrays.asList(10.0, 2.0, 1.0, 4.3))
                                                .build()

                                )
                        )
                        .atRisk(true)
                        .lastContactTimestamp(fiveDaysAgo.asDayTruncatedTimestamp())
                        .outdatedRisk(true)
        );

        // When
        runRobertBatchJob();

        // Then
        assertThatRegistrationForUser("user___1")
                .extracting(Registration::isAtRisk)
                .asInstanceOf(InstanceOfAssertFactories.BOOLEAN)
                .as("Registration risk")
                .isTrue();

        assertThatRegistrationForUser("user___1")
                .extracting(Registration::getLatestRiskEpochAsInstant)
                .asInstanceOf(InstanceOfAssertFactories.INSTANT)
                .as("Last risk update")
                .isCloseTo(now.asInstant(), within(1, ROBERT_EPOCH));

        assertThatRegistrationForUser("user___1")
                .extracting(Registration::getLastContactTimestampAsInstant)
                .asInstanceOf(InstanceOfAssertFactories.INSTANT)
                .as("Last contact timestamp")
                .isCloseTo(threeDaysAgo.asInstant(), within(1, DAYS));
    }

    @Test
    void last_contact_date_is_not_updated_when_already_at_risk_and_new_contact_at_risk_with_date_less_than_current_last_contact_date() {
        var now = clock.now();
        var threeDaysAgo = now.minus(3, ChronoUnit.DAYS);
        var fiveDaysAgo = now.minus(5, ChronoUnit.DAYS);

        givenRegistrationExistsForUser(
                "user___1", r -> r
                        .exposedEpochs(
                                List.of(
                                        EpochExposition.builder()
                                                .epochId(fiveDaysAgo.asEpochId())
                                                .expositionScores(Arrays.asList(10.0, 2.0, 1.0, 4.3))
                                                .build()

                                )
                        )
                        .atRisk(true)
                        .lastContactTimestamp(threeDaysAgo.asDayTruncatedTimestamp())
                        .outdatedRisk(true)
        );

        // When
        runRobertBatchJob();

        // Then
        assertThatRegistrationForUser("user___1")
                .extracting(Registration::isAtRisk)
                .asInstanceOf(InstanceOfAssertFactories.BOOLEAN)
                .as("Registration risk")
                .isTrue();

        assertThatRegistrationForUser("user___1")
                .extracting(Registration::getLatestRiskEpochAsInstant)
                .asInstanceOf(InstanceOfAssertFactories.INSTANT)
                .as("Last risk update")
                .isCloseTo(now.asInstant(), within(1, ROBERT_EPOCH));

        assertThatRegistrationForUser("user___1")
                .extracting(Registration::getLastContactTimestampAsInstant)
                .asInstanceOf(InstanceOfAssertFactories.INSTANT)
                .as("Last contact timestamp")
                .isCloseTo(fiveDaysAgo.asInstant(), within(1, DAYS));
    }

    @Test
    void last_contact_date_must_not_be_in_the_futur_when_already_at_risk_and_new_contact_at_risk() {
        var now = clock.now();
        var inFourDays = now.plus(4, ChronoUnit.DAYS);

        givenRegistrationExistsForUser(
                "user___1", r -> r
                        .exposedEpochs(
                                List.of(
                                        EpochExposition.builder()
                                                .epochId(inFourDays.asEpochId())
                                                .expositionScores(Arrays.asList(10.0, 5.0))
                                                .build()

                                )
                        )
                        .atRisk(true)
                        .outdatedRisk(true)
        );

        // When
        runRobertBatchJob();

        // Then
        assertThatRegistrationForUser("user___1")
                .extracting(Registration::isAtRisk)
                .asInstanceOf(InstanceOfAssertFactories.BOOLEAN)
                .as("Registration risk")
                .isTrue();

        assertThatRegistrationForUser("user___1")
                .extracting(Registration::getLatestRiskEpochAsInstant)
                .asInstanceOf(InstanceOfAssertFactories.INSTANT)
                .as("Last risk update")
                .isCloseTo(now.asInstant(), within(1, ROBERT_EPOCH));

        assertThatRegistrationForUser("user___1")
                .extracting(Registration::getLastContactTimestampAsInstant)
                .asInstanceOf(InstanceOfAssertFactories.INSTANT)
                .as("Last contact timestamp")
                .isCloseTo(now.asInstant(), within(1, DAYS));
    }

    @Test
    void notified_remains_false_when_risk_not_detected() {
        givenRegistrationExistsForUser(
                "user___1", r -> r
                        .exposedEpochs(
                                List.of(
                                        EpochExposition.builder()
                                                .epochId(clock.now().asEpochId())
                                                .expositionScores(Collections.singletonList(1.0))
                                                .build()
                                )
                        )
                        .isNotified(false)
                        .atRisk(false)
                        .outdatedRisk(true)
        );

        // When
        runRobertBatchJob();

        // Then
        assertThatRegistrationForUser("user___1")
                .extracting(Registration::isAtRisk)
                .asInstanceOf(InstanceOfAssertFactories.BOOLEAN)
                .as("Registration risk")
                .isFalse();

        assertThatRegistrationForUser("user___1")
                .extracting(Registration::isNotified)
                .asInstanceOf(InstanceOfAssertFactories.BOOLEAN)
                .as("Notified")
                .isFalse();
    }

    @Test
    void notified_remains_false_when_risk_detected() {
        givenRegistrationExistsForUser(
                "user___1", r -> r
                        .exposedEpochs(
                                List.of(
                                        EpochExposition.builder()
                                                .epochId(clock.now().asEpochId())
                                                .expositionScores(Arrays.asList(10.0, 2.0, 1.0, 4.3))
                                                .build()
                                )
                        )
                        .isNotified(false)
                        .atRisk(false)
                        .outdatedRisk(true)
        );

        // When
        runRobertBatchJob();

        // Then
        assertThatRegistrationForUser("user___1")
                .extracting(Registration::isAtRisk)
                .asInstanceOf(InstanceOfAssertFactories.BOOLEAN)
                .as("Registration risk")
                .isTrue();

        assertThatRegistrationForUser("user___1")
                .extracting(Registration::isNotified)
                .asInstanceOf(InstanceOfAssertFactories.BOOLEAN)
                .as("Notified")
                .isFalse();
    }

    @Test
    void notified_remains_true_when_risk_detected() {
        givenRegistrationExistsForUser(
                "user___1", r -> r
                        .exposedEpochs(
                                List.of(
                                        EpochExposition.builder()
                                                .epochId(clock.now().asEpochId())
                                                .expositionScores(Arrays.asList(10.0, 2.0, 1.0, 4.3))
                                                .build()
                                )
                        )
                        .isNotified(true)
                        .atRisk(false)
                        .outdatedRisk(true)
        );

        // When
        runRobertBatchJob();

        // Then
        assertThatRegistrationForUser("user___1")
                .extracting(Registration::isAtRisk)
                .asInstanceOf(InstanceOfAssertFactories.BOOLEAN)
                .as("Registration risk")
                .isTrue();

        assertThatRegistrationForUser("user___1")
                .extracting(Registration::isNotified)
                .asInstanceOf(InstanceOfAssertFactories.BOOLEAN)
                .as("Notified")
                .isTrue();
    }

    @Test
    void notified_remains_true_when_risk_not_detected() {
        givenRegistrationExistsForUser(
                "user___1", r -> r
                        .exposedEpochs(
                                List.of(
                                        EpochExposition.builder()
                                                .epochId(clock.now().asEpochId())
                                                .expositionScores(Collections.singletonList(1.0))
                                                .build()
                                )
                        )
                        .isNotified(true)
                        .atRisk(false)
                        .outdatedRisk(true)
        );

        // When
        runRobertBatchJob();

        // Then
        assertThatRegistrationForUser("user___1")
                .extracting(Registration::isAtRisk)
                .asInstanceOf(InstanceOfAssertFactories.BOOLEAN)
                .as("Registration risk")
                .isFalse();

        assertThatRegistrationForUser("user___1")
                .extracting(Registration::isNotified)
                .asInstanceOf(InstanceOfAssertFactories.BOOLEAN)
                .as("Notified")
                .isTrue();
    }

    @Test
    void no_risk_detected_if_no_score() {
        givenRegistrationExistsForUser(
                "user___1", r -> r
                        .exposedEpochs(
                                List.of()
                        )
                        .atRisk(false)
                        .outdatedRisk(true)
        );

        // When
        runRobertBatchJob();

        // Then
        assertThatRegistrationForUser("user___1")
                .extracting(Registration::isAtRisk)
                .asInstanceOf(InstanceOfAssertFactories.BOOLEAN)
                .as("Registration risk")
                .isFalse();
    }

}
