package fr.gouv.stopc.robert.server.batch.service;

import fr.gouv.stopc.robert.server.batch.IntegrationTest;
import fr.gouv.stopc.robert.server.batch.configuration.PropertyLoader;
import fr.gouv.stopc.robert.server.batch.utils.ProcessorTestUtils;
import fr.gouv.stopc.robert.server.common.service.IServerConfigurationService;
import fr.gouv.stopc.robert.server.common.utils.TimeUtils;
import fr.gouv.stopc.robertserver.database.model.EpochExposition;
import fr.gouv.stopc.robertserver.database.model.Registration;
import fr.gouv.stopc.robertserver.database.repository.RegistrationRepository;
import lombok.RequiredArgsConstructor;
import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Arrays;

import static fr.gouv.stopc.robert.server.batch.manager.MetricsManager.assertThatTimerMetricIncrement;
import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class PurgeOldEpochExpositionsServiceTest {

    private final PurgeOldEpochExpositionsService purgeOldEpochExpositionsService;

    private final RegistrationRepository registrationRepository;

    private final IServerConfigurationService serverConfigurationService;

    private final PropertyLoader propertyLoader;

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
        // Given
        byte[] rndBytes = ProcessorTestUtils.generateIdA();
        final int currentEpochId = TimeUtils.getCurrentEpochFrom(this.serverConfigurationService.getServiceTimeStart());
        final int contagiousPeriodPlusOneDay = propertyLoader.getContagiousPeriod() + 1;
        final int epochsByDay = 96;
        final int notContagiousEpochId = currentEpochId - contagiousPeriodPlusOneDay * epochsByDay;

        Registration registration = Registration.builder().permanentIdentifier(rndBytes)
                .build();

        ArrayList<EpochExposition> expositions = new ArrayList<>();

        Double[] expositionScore = new Double[] { 1.0 };
        expositions.add(
                EpochExposition.builder()
                        .epochId(currentEpochId)
                        .expositionScores(Arrays.asList(expositionScore))
                        .build()
        );
        expositions.add(
                EpochExposition.builder()
                        .epochId(notContagiousEpochId)
                        .expositionScores(Arrays.asList(expositionScore))
                        .build()
        );
        registration.setExposedEpochs(expositions);
        registrationRepository.save(registration);

        // When
        purgeOldEpochExpositionsService.performs();

        // Then
        Registration updatedRegistration = registrationRepository.findById(rndBytes).orElse(null);
        assertThat(updatedRegistration).as("Registration is null").isNotNull();
        assertThat(updatedRegistration.getExposedEpochs()).as("Exposed epochs is null").isNotNull();
        assertThat(updatedRegistration.getExposedEpochs()).hasSize(1);
        assertThatTimerMetricIncrement("robert.batch", "operation", "PURGE_OLD_EXPOSITIONS_STEP").isEqualTo(1L);
    }

    @ParameterizedTest
    @ValueSource(ints = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14 })
    void epochs_expositions_are_not_deleted_when_epochs_are_not_before_contagious_period(int inContagiousPeriod) {
        // Given
        byte[] rndBytes = ProcessorTestUtils.generateIdA();
        final int currentEpochId = TimeUtils.getCurrentEpochFrom(this.serverConfigurationService.getServiceTimeStart());
        final int epochsByDay = 96;
        final int inContagiousPeriodEpochId = currentEpochId - inContagiousPeriod * epochsByDay;

        Registration registration = Registration.builder().permanentIdentifier(rndBytes)
                .build();

        ArrayList<EpochExposition> expositions = new ArrayList<>();

        Double[] expositionScore = new Double[] { 1.0 };
        expositions.add(
                EpochExposition.builder()
                        .epochId(currentEpochId)
                        .expositionScores(Arrays.asList(expositionScore))
                        .build()
        );

        expositions.add(
                EpochExposition.builder()
                        .epochId(inContagiousPeriodEpochId)
                        .expositionScores(Arrays.asList(expositionScore))
                        .build()
        );
        registration.setExposedEpochs(expositions);
        registrationRepository.save(registration);

        // When
        purgeOldEpochExpositionsService.performs();

        // Then
        Registration updatedRegistration = registrationRepository.findById(rndBytes).orElse(null);
        assertThat(updatedRegistration).as("Registration is null").isNotNull();
        assertThat(updatedRegistration).as("Object has not been updated").isEqualTo(registration);
        assertThat(updatedRegistration.getExposedEpochs()).as("Exposed epochs is null").isNotNull();
        assertThat(updatedRegistration.getExposedEpochs()).hasSize(2);
    }
}
