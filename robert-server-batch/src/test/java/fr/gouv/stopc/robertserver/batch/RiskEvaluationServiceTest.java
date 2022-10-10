package fr.gouv.stopc.robertserver.batch;

import fr.gouv.stopc.robert.server.common.service.RobertClock;
import fr.gouv.stopc.robertserver.batch.test.IntegrationTest;
import fr.gouv.stopc.robertserver.database.model.EpochExposition;
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
import static fr.gouv.stopc.robertserver.batch.test.LogbackManager.assertThatInfoLogs;
import static fr.gouv.stopc.robertserver.batch.test.MessageMatcher.*;
import static fr.gouv.stopc.robertserver.batch.test.MongodbManager.*;
import static java.time.temporal.ChronoUnit.DAYS;
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
    void no_risk_detected_when_exposed_epochs_score_equals_to_zero() {
        final var twoDaysAgo = clock.now().minus(2, ChronoUnit.DAYS);
        final var fiveDaysAgo = clock.now().minus(5, ChronoUnit.DAYS);

        givenRegistrationExistsForIdA(
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
        assertThatRegistrationForIdA("user___1")
                .hasFieldOrPropertyWithValue("atRisk", false);
    }

    @Test
    void no_risk_detected_when_expositions_giving_score_not_at_risk() {
        final var now = clock.now();
        final var twoDaysAgo = now.minus(2, ChronoUnit.DAYS);

        givenRegistrationExistsForIdA(
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
        assertThatRegistrationForIdA("user___1")
                .hasFieldOrPropertyWithValue("atRisk", false)
                .hasFieldOrPropertyWithValue("latestRiskEpoch", 0)
                .hasFieldOrPropertyWithValue("lastContactTimestamp", 0L);
    }

    @Test
    void risk_detected_when_exposed_epoch_scores_greater_than_risk_threshold() {
        // Given Registration With Existing Score Above Threshold
        final var twoDaysAgo = clock.now().minus(2, ChronoUnit.DAYS);

        givenRegistrationExistsForIdA(
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
        assertThatRegistrationForIdA("user___1").hasFieldOrPropertyWithValue("atRisk", true);

        assertThatInfoLogs()
                .containsOnlyOnce(
                        "Risk detected. Aggregated risk since 0: 0.13237874351408596 greater than threshold 0.1"
                );
    }

    @Test
    void no_risk_detected_when_expose_epoch_scores_less_than_risk_threshold() {
        // Given Registration With Existing Score Below Threshold
        final var twoDaysAgo = clock.now().minus(2, ChronoUnit.DAYS);

        givenRegistrationExistsForIdA(
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
        assertThatRegistrationForIdA("user___1")
                .hasFieldOrPropertyWithValue("atRisk", false);
    }

    @Test
    void risk_detected_when_single_epoch_exposition_giving_score_at_risk() {
        final var now = clock.now();
        final var twoDaysAgo = now.minus(2, ChronoUnit.DAYS);

        givenRegistrationExistsForIdA(
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
        assertThatRegistrationForIdA("user___1")
                .hasFieldOrPropertyWithValue("atRisk", true);

        assertThatLatestRiskEpochForIdA("user___1")
                .isCloseTo(now.truncatedTo(ROBERT_EPOCH).asInstant(), within(1, ROBERT_EPOCH));

        assertThatLastContactTimestampForIdA("user___1")
                .isCloseTo(twoDaysAgo.asInstant(), within(1, DAYS));
    }

    @Test
    void last_exposition_at_risk_must_be_updated_when_epoch_exposition_giving_score_at_risk() {
        final var now = clock.now();
        final var yesterday = now.minus(1, ChronoUnit.DAYS);
        // Given
        givenRegistrationExistsForIdA(
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
        assertThatRegistrationForIdA("user___1")
                .hasFieldOrPropertyWithValue("atRisk", true);

        assertThatLatestRiskEpochForIdA("user___1")
                .isCloseTo(now.asInstant(), within(1, ROBERT_EPOCH));

        assertThatLastContactTimestampForIdA("user___1")
                .isCloseTo(now.asInstant(), within(1, DAYS));

    }

    @Test
    void last_contact_date_is_updated_when_already_at_risk_and_new_contact_at_risk_with_date_greater_than_current_last_contact_date() {
        final var now = clock.now();
        final var threeDaysAgo = now.minus(3, ChronoUnit.DAYS);
        final var fiveDaysAgo = now.minus(5, ChronoUnit.DAYS);

        givenRegistrationExistsForIdA(
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
        assertThatRegistrationForIdA("user___1")
                .hasFieldOrPropertyWithValue("atRisk", true);

        assertThatLatestRiskEpochForIdA("user___1")
                .isCloseTo(now.asInstant(), within(1, ROBERT_EPOCH));

        assertThatLastContactTimestampForIdA("user___1")
                .isCloseTo(threeDaysAgo.asInstant(), within(1, DAYS));
    }

    @Test
    void last_contact_date_is_not_updated_when_already_at_risk_and_new_contact_at_risk_with_date_less_than_current_last_contact_date() {
        final var now = clock.now();
        final var threeDaysAgo = now.minus(3, ChronoUnit.DAYS);
        final var fiveDaysAgo = now.minus(5, ChronoUnit.DAYS);

        givenRegistrationExistsForIdA(
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
        assertThatRegistrationForIdA("user___1")
                .hasFieldOrPropertyWithValue("atRisk", true);

        assertThatLatestRiskEpochForIdA("user___1")
                .isCloseTo(now.asInstant(), within(1, ROBERT_EPOCH));

        assertThatLastContactTimestampForIdA("user___1")
                .isCloseTo(fiveDaysAgo.asInstant(), within(1, DAYS));
    }

    @Test
    void last_contact_date_must_not_be_in_the_futur_when_already_at_risk_and_new_contact_at_risk() {
        final var now = clock.now();
        final var inFourDays = now.plus(4, ChronoUnit.DAYS);

        givenRegistrationExistsForIdA(
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
        assertThatRegistrationForIdA("user___1")
                .hasFieldOrPropertyWithValue("atRisk", true);

        assertThatLatestRiskEpochForIdA("user___1")
                .isCloseTo(now.asInstant(), within(1, ROBERT_EPOCH));

        assertThatLastContactTimestampForIdA("user___1")
                .isCloseTo(now.asInstant(), within(1, DAYS));
    }

    @Test
    void notified_remains_false_when_risk_not_detected() {
        givenRegistrationExistsForIdA(
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
        assertThatRegistrationForIdA("user___1")
                .hasFieldOrPropertyWithValue("isNotified", false)
                .hasFieldOrPropertyWithValue("atRisk", false);
    }

    @Test
    void notified_remains_false_when_risk_detected() {
        givenRegistrationExistsForIdA(
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
        assertThatRegistrationForIdA("user___1")
                .hasFieldOrPropertyWithValue("isNotified", false)
                .hasFieldOrPropertyWithValue("atRisk", true);
    }

    @Test
    void notified_remains_true_when_risk_detected() {
        givenRegistrationExistsForIdA(
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
        assertThatRegistrationForIdA("user___1")
                .hasFieldOrPropertyWithValue("isNotified", true)
                .hasFieldOrPropertyWithValue("atRisk", true);
    }

    @Test
    void notified_remains_true_when_risk_not_detected() {
        givenRegistrationExistsForIdA(
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
        assertThatRegistrationForIdA("user___1")
                .hasFieldOrPropertyWithValue("isNotified", true)
                .hasFieldOrPropertyWithValue("atRisk", false);
    }

    @Test
    void no_risk_detected_if_no_score() {
        givenRegistrationExistsForIdA(
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
        assertThatRegistrationForIdA("user___1")
                .hasFieldOrPropertyWithValue("atRisk", false);
    }

}
