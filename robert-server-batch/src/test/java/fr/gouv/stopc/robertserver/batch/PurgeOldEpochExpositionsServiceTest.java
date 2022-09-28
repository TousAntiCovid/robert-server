package fr.gouv.stopc.robertserver.batch;

import fr.gouv.stopc.robert.server.batch.configuration.PropertyLoader;
import fr.gouv.stopc.robert.server.common.service.RobertClock;
import fr.gouv.stopc.robertserver.batch.test.IntegrationTest;
import fr.gouv.stopc.robertserver.database.model.EpochExposition;
import fr.gouv.stopc.robertserver.database.model.Registration;
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
import static fr.gouv.stopc.robertserver.batch.test.MongodbManager.assertThatRegistrationForUser;
import static fr.gouv.stopc.robertserver.batch.test.MongodbManager.givenRegistrationExistsForUser;
import static org.assertj.core.api.Assertions.assertThat;

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
    void can_log_start_and_end_of_purge_process() {
        // When
        runRobertBatchJob();

        assertThatInfoLogs()
                .contains(
                        "START : Purge Old Epoch Expositions.",
                        "END : Purge Old Epoch Expositions."
                );
    }

    @Test
    void epochs_expositions_are_deleted_when_epochs_are_before_contagious_period() {
        final int contagiousPeriodPlusOneDay = CONTAGIOUS_PERIOD + 1;
        var beforeStartOfContagiousPeriod = clock.now().minus(contagiousPeriodPlusOneDay, ChronoUnit.DAYS);

        givenRegistrationExistsForUser(
                "user___1", r -> r
                        .exposedEpochs(
                                List.of(
                                        EpochExposition.builder()
                                                .epochId(clock.now().asEpochId())
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
        assertThatRegistrationForUser("user___1")
                .extracting(Registration::getExposedEpochs)
                .asList()
                .hasSize(1);
    }

    @ParameterizedTest
    @ValueSource(ints = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14 })
    void epochs_expositions_are_not_deleted_when_epochs_are_not_before_contagious_period(int inContagiousPeriod) {
        // Given
        var beforeStartOfContagiousPeriod = clock.now().minus(inContagiousPeriod, ChronoUnit.DAYS);
        var registration = givenRegistrationExistsForUser(
                "user___1", r -> r
                        .exposedEpochs(
                                List.of(
                                        EpochExposition.builder()
                                                .epochId(clock.now().asEpochId())
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
        assertThatRegistrationForUser("user___1")
                .as("Object has not been updated")
                .isEqualTo(registration);

        assertThatRegistrationForUser("user___1")
                .extracting(Registration::getExposedEpochs)
                .asList()
                .hasSize(2);
    }
}
