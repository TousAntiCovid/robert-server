package fr.gouv.stopc.robert.server.batch.service;

import fr.gouv.stopc.robert.server.batch.RobertServerBatchApplication;
import fr.gouv.stopc.robert.server.batch.service.impl.BatchRegistrationServiceImpl;
import fr.gouv.stopc.robert.server.common.service.IServerConfigurationService;
import fr.gouv.stopc.robert.server.common.utils.TimeUtils;
import fr.gouv.stopc.robertserver.database.model.EpochExposition;
import fr.gouv.stopc.robertserver.database.model.Registration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static fr.gouv.stopc.robert.server.common.utils.TimeUtils.EPOCHS_PER_DAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { RobertServerBatchApplication.class })
@TestPropertySource(locations = "classpath:application.properties", properties = { "robert.scoring.algo-version=2",
        "robert.scoring.batch-mode=SCORE_CONTACTS_AND_COMPUTE_RISK" })
public class BatchRegistrationServiceTest {

    @Autowired
    private IServerConfigurationService serverConfigurationService;

    BatchRegistrationService batchRegistrationService;

    @Captor
    private ArgumentCaptor<ArrayList<Double>> acAggregateScores;

    @Mock
    ScoringStrategyService scoringStrategyService;

    @BeforeEach
    public void setUp() {
        this.batchRegistrationService = new BatchRegistrationServiceImpl(scoringStrategyService);
    }

    @Test
    public void shouldExposedEpochsWithoutEpochsOlderThanContagiousPeriodFilterTooOldExposedEpochs() {

        final var currentEpoch = 26543;

        // GIVEN
        final var epochId = currentEpoch - (14 * 96) + new SecureRandom().nextInt(100) + 1;
        Double[] expositionsForFirstEpoch = new Double[] { 1.0 };
        Double[] expositionsForSecondEpoch = new Double[] { 12.5 };
        ArrayList<EpochExposition> expositions = new ArrayList<>();
        expositions.add(
                EpochExposition.builder()
                        .epochId(epochId)
                        .expositionScores(Arrays.asList(expositionsForFirstEpoch))
                        .build()
        );
        expositions.add(
                EpochExposition.builder()
                        .epochId(epochId - (30 * 96))
                        .expositionScores(Arrays.asList(expositionsForSecondEpoch))
                        .build()
        );

        // WHEN
        final var filteredEpochExpositions = batchRegistrationService
                .getExposedEpochsWithoutEpochsOlderThanContagiousPeriod(expositions, currentEpoch, 14, 900);

        // THEN
        assertThat(filteredEpochExpositions.size()).isEqualTo(1);
        assertThat(filteredEpochExpositions.get(0).getEpochId()).isEqualTo(epochId);
    }

    @Test
    public void shouldExposedEpochsWithoutEpochsOlderThanContagiousPeriodReturnsEmptyListInCaseEmptyListIsProvided() {

        // WHEN
        final var filteredEpochExpositions = batchRegistrationService
                .getExposedEpochsWithoutEpochsOlderThanContagiousPeriod(new ArrayList<>(), 26543, 14, 900);

        // THEN
        assertThat(filteredEpochExpositions.size()).isEqualTo(0);

    }

    @Test
    public void shouldExposedEpochsWithoutEpochsOlderThanContagiousPeriodReturnsEmptyListInCaseAllProvidedExposedEpochsAreTooOld() {

        final var currentEpoch = 26543;

        // GIVEN
        final var epochId = currentEpoch - (14 * 96) + new SecureRandom().nextInt(100) + 1;
        final var expositionsForFirstEpoch = new Double[] { 1.0 };
        final ArrayList<EpochExposition> expositions = new ArrayList<>();
        expositions.add(
                EpochExposition.builder()
                        .epochId(epochId - (30 * 96))
                        .expositionScores(Arrays.asList(expositionsForFirstEpoch))
                        .build()
        );

        // WHEN
        final var filteredEpochExpositions = batchRegistrationService
                .getExposedEpochsWithoutEpochsOlderThanContagiousPeriod(expositions, currentEpoch, 14, 900);

        // THEN
        assertThat(filteredEpochExpositions.size()).isEqualTo(0);

    }

    @Test
    public void should_update_registration_at_risk_with_threshold_exceeded_and_lastContact_before_seven_days() {

        // GIVEN
        final var timeStart = this.serverConfigurationService.getServiceTimeStart();
        final var currentEpoch = TimeUtils.getCurrentEpochFrom(timeStart);
        final var contact6DaysAgoNTPTimestamp = TimeUtils
                .convertUnixMillistoNtpSeconds(Instant.now().minus(6, ChronoUnit.DAYS).toEpochMilli());

        final List<EpochExposition> exposedEpochs = new ArrayList<>();
        exposedEpochs
                .add(EpochExposition.builder().epochId(currentEpoch).expositionScores(Arrays.asList(0.5, 0.4)).build());

        final var registration = Registration.builder()
                .atRisk(false)
                .exposedEpochs(exposedEpochs)
                .lastContactTimestamp(contact6DaysAgoNTPTimestamp)
                .build();

        when(scoringStrategyService.aggregate(anyList())).thenReturn(1.2);

        // WHEN
        final var isAtRisk = batchRegistrationService.updateRegistrationIfRisk(registration, timeStart, 1.0);

        // THEN
        assertThat(isAtRisk).isTrue();
        assertThat(registration.getLatestRiskEpoch()).isEqualTo(currentEpoch);
    }

    @Test
    public void should_not_update_registration_at_risk_with_threshold_exceeded_but_lastContact_after_seven_days() {

        // GIVEN
        final var timeStart = this.serverConfigurationService.getServiceTimeStart();
        final var currentEpoch = TimeUtils.getCurrentEpochFrom(timeStart);
        final var contact8DaysAgoNTPTimestamp = TimeUtils
                .convertUnixMillistoNtpSeconds(Instant.now().minus(8, ChronoUnit.DAYS).toEpochMilli());
        final var latestRiskEpoch = currentEpoch - 5;

        final List<EpochExposition> exposedEpochs = new ArrayList<>();
        exposedEpochs
                .add(EpochExposition.builder().epochId(currentEpoch).expositionScores(Arrays.asList(0.5, 0.4)).build());

        final var registration = Registration.builder()
                .atRisk(false)
                .exposedEpochs(exposedEpochs)
                .lastContactTimestamp(contact8DaysAgoNTPTimestamp)
                .latestRiskEpoch(latestRiskEpoch)
                .build();

        when(scoringStrategyService.aggregate(anyList())).thenReturn(1.2);

        // WHEN
        final var isAtRisk = batchRegistrationService.updateRegistrationIfRisk(registration, timeStart, 1.0);

        // THEN
        assertThat(isAtRisk).isFalse();
        assertThat(registration.getLatestRiskEpoch()).isEqualTo(latestRiskEpoch);
    }

    @Test
    public void should_not_update_registration_at_risk_with_threshold_not_exceeded_and_lastContact_before_seven_days() {

        // GIVEN
        final var timeStart = this.serverConfigurationService.getServiceTimeStart();
        final var currentEpoch = TimeUtils.getCurrentEpochFrom(timeStart);
        final var contact6DaysAgoNTPTimestamp = TimeUtils
                .convertUnixMillistoNtpSeconds(Instant.now().minus(6, ChronoUnit.DAYS).toEpochMilli());
        final var latestRiskEpoch = currentEpoch - 5;

        final List<EpochExposition> exposedEpochs = new ArrayList<>();
        exposedEpochs
                .add(EpochExposition.builder().epochId(currentEpoch).expositionScores(Arrays.asList(0.5, 0.4)).build());

        final var registration = Registration.builder()
                .atRisk(false)
                .exposedEpochs(exposedEpochs)
                .lastContactTimestamp(contact6DaysAgoNTPTimestamp)
                .latestRiskEpoch(latestRiskEpoch)
                .build();

        when(scoringStrategyService.aggregate(anyList())).thenReturn(0.5);

        // WHEN
        final var isAtRisk = batchRegistrationService.updateRegistrationIfRisk(registration, timeStart, 1.0);

        // THEN
        assertThat(isAtRisk).isFalse();
        assertThat(registration.getLatestRiskEpoch()).isEqualTo(latestRiskEpoch);
    }

    @Test
    public void should_not_update_Registration_at_risk_after_filtering_out_scores_before_latestRiskEpoch() {

        // GIVEN
        final var timeStart = this.serverConfigurationService.getServiceTimeStart();
        final var currentEpoch = TimeUtils.getCurrentEpochFrom(timeStart);
        final var latestRiskEpoch = currentEpoch - 5;
        final var contact6DaysAgoNTPTimestamp = TimeUtils
                .convertUnixMillistoNtpSeconds(Instant.now().minus(6, ChronoUnit.DAYS).toEpochMilli());

        final List<EpochExposition> exposedEpochs = new ArrayList<>();
        exposedEpochs
                .add(EpochExposition.builder().epochId(currentEpoch).expositionScores(Arrays.asList(0.5, 0.4)).build());
        exposedEpochs
                .add(EpochExposition.builder().epochId(currentEpoch - 60).expositionScores(Arrays.asList(0.1)).build());
        exposedEpochs
                .add(EpochExposition.builder().epochId(currentEpoch - 1).expositionScores(Arrays.asList(0.05)).build());

        final var registration = Registration.builder()
                .atRisk(false)
                .latestRiskEpoch(latestRiskEpoch)
                .exposedEpochs(exposedEpochs)
                .lastContactTimestamp(contact6DaysAgoNTPTimestamp)
                .build();

        when(scoringStrategyService.aggregate(anyList())).thenReturn(0.2);

        // WHEN
        final var isAtRisk = batchRegistrationService.updateRegistrationIfRisk(registration, timeStart, 1.0);

        // THEN
        assertThat(isAtRisk).isFalse();
        assertThat(registration.getLatestRiskEpoch()).isEqualTo(latestRiskEpoch);

        verify(scoringStrategyService).aggregate(acAggregateScores.capture());
        final List<ArrayList<Double>> aggregatedScores = acAggregateScores.getAllValues();
        assertThat(aggregatedScores.get(0)).containsExactly(0.9, 0.05);
    }

    @Test
    public void should_not_take_into_account_RandomizedLastContactDate_if_it_is_before_RegistrationLastContactDate() {
        // GIVEN
        final var timeStart = this.serverConfigurationService.getServiceTimeStart();
        final var currentEpoch = TimeUtils.getCurrentEpochFrom(timeStart);
        final var lastContactDateFromExposedEpoch = currentEpoch;
        final var realLastContactDateFromExposedEpoch = TimeUtils
                .getNtpSeconds(lastContactDateFromExposedEpoch, timeStart);
        final var lastContactDateFromRegistration = TimeUtils.getNtpSeconds(currentEpoch - 2, timeStart);
        final var randomizedLastContactDate = TimeUtils.getNtpSeconds(currentEpoch - EPOCHS_PER_DAY, timeStart);
        final var lastContactDate = TimeUtils.convertNTPSecondsToUnixMillis(lastContactDateFromRegistration);
        final var truncateTimestamp = TimeUtils.dayTruncatedTimestamp(randomizedLastContactDate);
        final var latestRiskEpoch = currentEpoch - 5;

        final List<EpochExposition> exposedEpochs = new ArrayList<>();
        exposedEpochs
                .add(EpochExposition.builder().epochId(currentEpoch - 60).expositionScores(Arrays.asList(0.1)).build());
        exposedEpochs.add(
                EpochExposition.builder().epochId(lastContactDateFromExposedEpoch).expositionScores(Arrays.asList(0.05))
                        .build()
        );

        final var registration = Registration.builder()
                .atRisk(false)
                .lastContactTimestamp(lastContactDateFromRegistration)
                .latestRiskEpoch(latestRiskEpoch)
                .exposedEpochs(exposedEpochs)
                .build();

        when(scoringStrategyService.aggregate(anyList())).thenReturn(1.2);

        try (MockedStatic<TimeUtils> mockedTimeUtils = Mockito.mockStatic(TimeUtils.class)) {
            // mock the static methods
            mockedTimeUtils.when(() -> TimeUtils.getNtpSeconds(lastContactDateFromExposedEpoch, timeStart))
                    .thenReturn(realLastContactDateFromExposedEpoch);
            mockedTimeUtils.when(() -> TimeUtils.getRandomizedDateNotInFuture(realLastContactDateFromExposedEpoch))
                    .thenReturn(randomizedLastContactDate);
            mockedTimeUtils.when(() -> TimeUtils.dayTruncatedTimestamp(randomizedLastContactDate))
                    .thenReturn(truncateTimestamp);
            mockedTimeUtils.when(() -> TimeUtils.convertNTPSecondsToUnixMillis(lastContactDateFromRegistration))
                    .thenReturn(lastContactDate);

            // WHEN
            batchRegistrationService.updateRegistrationIfRisk(registration, timeStart, 1.0);
        } // the static mock is not visible outside the try resource block

        // THEN
        assertThat(registration.getLastContactTimestamp()).isEqualTo(lastContactDateFromRegistration);
        assertThat(registration.isAtRisk()).isTrue();

    }
}
