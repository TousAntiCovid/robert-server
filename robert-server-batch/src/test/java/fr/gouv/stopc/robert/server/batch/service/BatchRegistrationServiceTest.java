package fr.gouv.stopc.robert.server.batch.service;

import fr.gouv.stopc.robert.server.batch.service.impl.BatchRegistrationServiceImpl;
import fr.gouv.stopc.robert.server.batch.utils.PropertyLoader;
import fr.gouv.stopc.robert.server.common.service.RobertClock;
import fr.gouv.stopc.robert.server.common.utils.TimeUtils;
import fr.gouv.stopc.robertserver.database.model.EpochExposition;
import fr.gouv.stopc.robertserver.database.model.Registration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class BatchRegistrationServiceTest {

    BatchRegistrationService batchRegistrationService;

    @Captor
    private ArgumentCaptor<List<Double>> acAggregateScores;

    @Mock
    ScoringStrategyService scoringStrategyService;

    final long timeStart = TimeUtils.convertUnixMillistoNtpSeconds(System.currentTimeMillis() - (48 * 3600 * 1000));

    @BeforeEach
    public void setUp() {
        final var robertClock = new RobertClock(timeStart);
        final var propertyLoader = Mockito.mock(PropertyLoader.class);
        this.batchRegistrationService = new BatchRegistrationServiceImpl(
                scoringStrategyService, propertyLoader, robertClock
        );
    }

    @Test
    public void getExposedEpochsWithoutEpochsOlderThanContagiousPeriod_should_remove_too_old_exposed_epochs() {

        final var currentEpoch = 26543;

        // GIVEN
        final var epochId = currentEpoch - (14 * 96) + new SecureRandom().nextInt(100) + 1;
        final var expositions = List.of(
                EpochExposition.builder()
                        .epochId(epochId)
                        .expositionScores(List.of(1.0))
                        .build(),
                EpochExposition.builder()
                        .epochId(epochId - (30 * 96))
                        .expositionScores(List.of(12.5))
                        .build()
        );

        // WHEN
        final var filteredEpochExpositions = batchRegistrationService
                .getExposedEpochsWithoutEpochsOlderThanContagiousPeriod(expositions, currentEpoch, 14, 900);

        // THEN
        assertThat(filteredEpochExpositions)
                .containsExactly(
                        EpochExposition.builder()
                                .epochId(epochId)
                                .expositionScores(List.of(1.0))
                                .build()
                );
    }

    @Test
    public void getExposedEpochsWithoutEpochsOlderThanContagiousPeriod_should_return_empty_list_when_it_receives_an_empty_list() {

        // WHEN
        final var filteredEpochExpositions = batchRegistrationService
                .getExposedEpochsWithoutEpochsOlderThanContagiousPeriod(new ArrayList<>(), 26543, 14, 900);

        // THEN
        assertThat(filteredEpochExpositions).isEmpty();
    }

    @Test
    public void getExposedEpochsWithoutEpochsOlderThanContagiousPeriod_should_return_empty_list_when_it_receives_too_old_epochs() {

        final var currentEpoch = 26543;

        // GIVEN
        final var epochId = currentEpoch - (14 * 96) + new SecureRandom().nextInt(100) + 1;
        final var expositions = List.of(
                EpochExposition.builder()
                        .epochId(epochId - (30 * 96))
                        .expositionScores(List.of(1.0))
                        .build()
        );

        // WHEN
        final var filteredEpochExpositions = batchRegistrationService
                .getExposedEpochsWithoutEpochsOlderThanContagiousPeriod(expositions, currentEpoch, 14, 900);

        // THEN
        assertThat(filteredEpochExpositions).isEmpty();
    }

    @Test
    public void updateRegistrationIfRisk_should_set_registration_at_risk() {

        // GIVEN
        final var currentEpoch = TimeUtils.getCurrentEpochFrom(timeStart);
        final var registration = Registration.builder()
                .atRisk(false)
                .exposedEpochs(
                        List.of(
                                EpochExposition.builder()
                                        .epochId(currentEpoch)
                                        .expositionScores(List.of(0.5, 0.4))
                                        .build()
                        )
                )
                .build();

        when(scoringStrategyService.aggregate(anyList())).thenReturn(1.2);

        // WHEN
        final var isAtRisk = batchRegistrationService.updateRegistrationIfRisk(registration, timeStart, 1.0);

        // THEN
        assertThat(isAtRisk)
                .as("risk status return value")
                .isTrue();
        assertThat(registration.getLatestRiskEpoch())
                .as("registration latest risk epoch")
                .isEqualTo(currentEpoch);
    }

    @Test
    public void updateRegistrationIfRisk_should_filter_out_scores_before_last_risk_epoch() {

        // GIVEN
        final var currentEpoch = TimeUtils.getCurrentEpochFrom(timeStart);
        final var latestRiskEpoch = currentEpoch - 5;
        final var registration = Registration.builder()
                .atRisk(false)
                .latestRiskEpoch(latestRiskEpoch)
                .exposedEpochs(
                        List.of(
                                EpochExposition.builder()
                                        .epochId(currentEpoch)
                                        .expositionScores(Arrays.asList(0.5, 0.4))
                                        .build(),
                                EpochExposition.builder()
                                        .epochId(currentEpoch - 60)
                                        .expositionScores(Arrays.asList(0.1))
                                        .build(),
                                EpochExposition.builder()
                                        .epochId(currentEpoch - 1)
                                        .expositionScores(Arrays.asList(0.05))
                                        .build()
                        )
                )
                .build();

        when(scoringStrategyService.aggregate(anyList())).thenReturn(0.2);

        // WHEN
        final var isAtRisk = batchRegistrationService.updateRegistrationIfRisk(registration, timeStart, 1.0);

        // THEN
        assertThat(isAtRisk)
                .as("risk status return value")
                .isFalse();
        assertThat(registration.getLatestRiskEpoch())
                .as("registration latest risk epoch")
                .isEqualTo(latestRiskEpoch);

        verify(scoringStrategyService).aggregate(acAggregateScores.capture());
        assertThat(acAggregateScores.getAllValues())
                .containsExactly(List.of(0.9, 0.05));
    }

    @Test
    public void updateRegistrationIfRisk_should_ignore_randomized_last_contact_date_if_it_is_before_registration_lastContactDate() {
        // GIVEN
        final var currentEpoch = TimeUtils.getCurrentEpochFrom(timeStart);
        final var lastContactDateFromExposedEpoch = currentEpoch;
        final var lastContactDateFromRegistration = TimeUtils.getNtpSeconds(currentEpoch - 2, timeStart);
        final var latestRiskEpoch = currentEpoch - 5;

        final var registration = Registration.builder()
                .atRisk(false)
                .lastContactTimestamp(lastContactDateFromRegistration)
                .latestRiskEpoch(latestRiskEpoch)
                .exposedEpochs(List.of(
                        EpochExposition.builder()
                                .epochId(currentEpoch - 60)
                                .expositionScores(List.of(0.1))
                                .build(),
                        EpochExposition.builder()
                                .epochId(lastContactDateFromExposedEpoch)
                                .expositionScores(List.of(0.05))
                        .build()
                ))
                .build();

        when(scoringStrategyService.aggregate(anyList())).thenReturn(1.2);

        final var isAtRisk = batchRegistrationService.updateRegistrationIfRisk(registration, timeStart, 1.0);

        // THEN
        assertThat(isAtRisk)
                .as("risk status return value")
                .isTrue();
        assertThat(registration.getLastContactTimestamp())
                .as("last contact timestamp")
                .isEqualTo(lastContactDateFromRegistration);
        assertThat(registration.isAtRisk())
                .as("registration risk status")
                .isTrue();
    }
}
