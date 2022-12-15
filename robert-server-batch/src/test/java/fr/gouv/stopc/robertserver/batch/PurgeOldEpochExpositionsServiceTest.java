package fr.gouv.stopc.robertserver.batch;

import fr.gouv.stopc.robert.server.batch.configuration.PropertyLoader;
import fr.gouv.stopc.robertserver.batch.test.IntegrationTest;
import fr.gouv.stopc.robertserver.common.RobertClock;
import fr.gouv.stopc.robertserver.database.model.EpochExposition;
import fr.gouv.stopc.robertserver.database.model.Kpi;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;

import static fr.gouv.stopc.robertserver.batch.test.LogbackManager.assertThatInfoLogs;
import static fr.gouv.stopc.robertserver.batch.test.MessageMatcher.assertThatEpochExpositionsForIdA;
import static fr.gouv.stopc.robertserver.batch.test.MongodbManager.assertThatKpis;
import static fr.gouv.stopc.robertserver.batch.test.MongodbManager.givenRegistrationExistsForIdA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@IntegrationTest
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class PurgeOldEpochExpositionsServiceTest {

    private final JobLauncherTestUtils jobLauncher;

    private final RobertClock clock;

    public static final int CONTAGIOUS_PERIOD = 14;

    @SneakyThrows
    private void runRobertBatchJob() {
        jobLauncher.launchJob();
    }

    @Test
    void contagious_period_is_set_to_fourteen_days(@Autowired PropertyLoader propertyLoader) {
        assertThat(propertyLoader.getContagiousPeriod())
                .as("Contagious period property is set to 14 days")
                .isEqualTo(CONTAGIOUS_PERIOD);
    }

    @Test
    void logs_start_and_end_of_purge_process() {
        // When
        runRobertBatchJob();

        assertThatInfoLogs()
                .contains(
                        "START : Purge Old Epoch Expositions.",
                        "END : Purge Old Epoch Expositions."
                );
    }

    @Test
    void delete_epochs_when_are_before_contagious_period() {
        final int contagiousPeriodPlusOneDay = CONTAGIOUS_PERIOD + 1;
        final var now = clock.now();
        final var beforeStartOfContagiousPeriod = now.minus(contagiousPeriodPlusOneDay, ChronoUnit.DAYS);

        givenRegistrationExistsForIdA(
                "user___1", r -> r
                        .exposedEpochs(
                                List.of(
                                        EpochExposition.builder()
                                                .epochId(now.asEpochId())
                                                .expositionScores(Collections.singletonList(1.0))
                                                .build(),
                                        EpochExposition.builder()
                                                .epochId(beforeStartOfContagiousPeriod.asEpochId())
                                                .expositionScores(Collections.singletonList(1.0))
                                                .build()

                                )
                        )
        );

        // When
        runRobertBatchJob();

        // Then
        assertThatEpochExpositionsForIdA("user___1")
                .containsOnlyOnce(
                        new EpochExposition(now.asEpochId(), List.of(1.0))
                );

        assertThatKpis().as("check kpis values")
                .extracting(Kpi::getName, Kpi::getValue)
                .containsExactlyInAnyOrder(
                        tuple("exposedButNotAtRiskUsers", 1L),
                        tuple("infectedUsersNotNotified", 0L),
                        tuple("notifiedUsersScoredAgain", 0L)
                );
    }

    @ParameterizedTest
    @ValueSource(ints = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14 })
    void no_delete_epochs_when_not_before_contagious_period(int inContagiousPeriod) {
        // Given
        final var now = clock.now();
        final var beforeStartOfContagiousPeriod = now.minus(inContagiousPeriod, ChronoUnit.DAYS);
        givenRegistrationExistsForIdA(
                "user___1", r -> r
                        .exposedEpochs(
                                List.of(
                                        EpochExposition.builder()
                                                .epochId(now.asEpochId())
                                                .expositionScores(Collections.singletonList(2.0))
                                                .build(),
                                        EpochExposition.builder()
                                                .epochId(beforeStartOfContagiousPeriod.asEpochId())
                                                .expositionScores(Collections.singletonList(3.0))
                                                .build()

                                )
                        )
        );

        // When
        runRobertBatchJob();

        // Then
        assertThatEpochExpositionsForIdA("user___1")
                .containsOnlyOnce(
                        new EpochExposition(now.asEpochId(), List.of(2.0)),
                        new EpochExposition(beforeStartOfContagiousPeriod.asEpochId(), List.of(3.0))
                );

        assertThatKpis().as("check kpis values")
                .extracting(Kpi::getName, Kpi::getValue)
                .containsExactlyInAnyOrder(
                        tuple("exposedButNotAtRiskUsers", 1L),
                        tuple("infectedUsersNotNotified", 0L),
                        tuple("notifiedUsersScoredAgain", 0L)
                );
    }
}
