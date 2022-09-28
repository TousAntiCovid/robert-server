package fr.gouv.stopc.robertserver.batch;

import fr.gouv.stopc.robert.server.common.service.RobertClock;
import fr.gouv.stopc.robertserver.batch.test.IntegrationTest;
import fr.gouv.stopc.robertserver.database.model.EpochExposition;
import fr.gouv.stopc.robertserver.database.model.Registration;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static fr.gouv.stopc.robert.server.common.service.RobertClock.ROBERT_EPOCH;
import static fr.gouv.stopc.robertserver.batch.test.LogbackManager.*;
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
        var badEcc = new byte[] { (byte) 0xff };

        // Given
        givenRegistrationExistsForUser("user___1");
        givenPendingContact(
                "user___1", badEcc, helloMessagesBuilder -> helloMessagesBuilder.addAt(now, now.plus(120, SECONDS))
        );

        // When
        runRobertBatchJob();

        // Then
        assertThatContactsToProcess().isEmpty();

        assertThatRegistrationForUser("user___1")
                .hasFieldOrPropertyWithValue("atRisk", false);

        assertThatRegistrationForUser("user___1")
                .extracting(Registration::getExposedEpochs)
                .asList()
                .isEmpty();

        assertThatInfoLogs()
                .contains("Country code [-1] is not managed by this server ([33])");
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
                .hasFieldOrPropertyWithValue("atRisk", false);
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
                .hasFieldOrPropertyWithValue("atRisk", true);

        assertThatInfoLogs()
                .containsOnlyOnce(
                        "Risk detected. Aggregated risk since 0: 0.13237874351408596 greater than threshold 0.1"
                );
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
                .hasFieldOrPropertyWithValue("atRisk", false);
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
                .hasFieldOrPropertyWithValue("atRisk", true);

        assertThatLatestRiskEpochForUser("user___1")
                .isCloseTo(now.asInstant(), within(1, ROBERT_EPOCH));

        assertThatLastContactTimestampForUser("user___1")
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
                .hasFieldOrPropertyWithValue("atRisk", false)
                .hasFieldOrPropertyWithValue("latestRiskEpoch", 0)
                .hasFieldOrPropertyWithValue("lastContactTimestamp", 0L);
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
                .hasFieldOrPropertyWithValue("atRisk", true);

        assertThatLatestRiskEpochForUser("user___1")
                .isCloseTo(now.asInstant(), within(1, ROBERT_EPOCH));

        assertThatLastContactTimestampForUser("user___1")
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
                        .lastContactTimestamp(fiveDaysAgo.asNtpTimestamp())
                        .outdatedRisk(true)
        );

        // When
        runRobertBatchJob();

        // Then
        assertThatRegistrationForUser("user___1")
                .hasFieldOrPropertyWithValue("atRisk", true);

        assertThatLatestRiskEpochForUser("user___1")
                .isCloseTo(now.asInstant(), within(1, ROBERT_EPOCH));

        assertThatLastContactTimestampForUser("user___1")
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
                        .lastContactTimestamp(threeDaysAgo.asNtpTimestamp())
                        .outdatedRisk(true)
        );

        // When
        runRobertBatchJob();

        // Then
        assertThatRegistrationForUser("user___1")
                .hasFieldOrPropertyWithValue("atRisk", true);

        assertThatLatestRiskEpochForUser("user___1")
                .isCloseTo(now.asInstant(), within(1, ROBERT_EPOCH));

        assertThatLastContactTimestampForUser("user___1")
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
                .hasFieldOrPropertyWithValue("atRisk", true);

        assertThatLatestRiskEpochForUser("user___1")
                .isCloseTo(now.asInstant(), within(1, ROBERT_EPOCH));

        assertThatLastContactTimestampForUser("user___1")
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
                .hasFieldOrPropertyWithValue("isNotified", false)
                .hasFieldOrPropertyWithValue("atRisk", false);
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
                .hasFieldOrPropertyWithValue("isNotified", false)
                .hasFieldOrPropertyWithValue("atRisk", true);
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
                .hasFieldOrPropertyWithValue("isNotified", true)
                .hasFieldOrPropertyWithValue("atRisk", true);
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
                .hasFieldOrPropertyWithValue("isNotified", true)
                .hasFieldOrPropertyWithValue("atRisk", false);
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
                .hasFieldOrPropertyWithValue("atRisk", false);
    }

}
