package fr.gouv.stopc.robert.server.batch.service;

import fr.gouv.stopc.robert.server.batch.IntegrationTest;
import fr.gouv.stopc.robert.server.batch.configuration.PropertyLoader;
import fr.gouv.stopc.robert.server.batch.manager.MetricsManager;
import fr.gouv.stopc.robert.server.batch.manager.MongodbManager;
import fr.gouv.stopc.robert.server.common.service.RobertClock;
import fr.gouv.stopc.robertserver.database.model.EpochExposition;
import fr.gouv.stopc.robertserver.database.model.Registration;
import fr.gouv.stopc.robertserver.database.repository.RegistrationRepository;
import lombok.RequiredArgsConstructor;
import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestExecutionListeners;

import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;

import static fr.gouv.stopc.robert.server.batch.manager.MetricsManager.assertThatTimerMetricIncrement;
import static fr.gouv.stopc.robert.server.batch.manager.MongodbManager.assertThatRegistrationForUser;
import static fr.gouv.stopc.robert.server.batch.manager.MongodbManager.givenRegistrationExistsForUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.context.TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS;

@IntegrationTest
@TestExecutionListeners(listeners = {
        MetricsManager.class,
        MongodbManager.class
}, mergeMode = MERGE_WITH_DEFAULTS)
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class PurgeOldEpochExpositionsServiceTest {

    private final PurgeOldEpochExpositionsService purgeOldEpochExpositionsService;

    private final RegistrationRepository registrationRepository;

    private final PropertyLoader propertyLoader;

    private final RobertClock clock;

    @Test
    void contagious_period_is_forteen_days() {
        assertThat(propertyLoader.getContagiousPeriod()).as("Contagious period property is set to 14 days")
                .isEqualTo(14);
    }

    @Test
    void can_log_start_and_end_of_purge_process() {
        try (final var logCaptor = LogCaptor.forClass(PurgeOldEpochExpositionsService.class)) {
            purgeOldEpochExpositionsService.performs();

            assertThat(logCaptor.getInfoLogs())
                    .contains(
                            "START : Purge Old Epoch Expositions.",
                            "END : Purge Old Epoch Expositions."
                    );
        }
    }

    @Test
    void epochs_expositions_are_deleted_when_epochs_are_before_contagious_period() {
        final int contagiousPeriodPlusOneDay = propertyLoader.getContagiousPeriod() + 1;
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
        purgeOldEpochExpositionsService.performs();

        // Then
        assertThatRegistrationForUser("user___1")
                .extracting(Registration::getExposedEpochs)
                .asList()
                .hasSize(1);

        assertThatTimerMetricIncrement("robert.batch", "operation", "PURGE_OLD_EXPOSITIONS_STEP").isEqualTo(1L);
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
        purgeOldEpochExpositionsService.performs();

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
