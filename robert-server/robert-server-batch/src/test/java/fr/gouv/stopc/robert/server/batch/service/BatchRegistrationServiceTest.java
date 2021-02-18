package fr.gouv.stopc.robert.server.batch.service;

import static fr.gouv.stopc.robert.server.common.utils.TimeUtils.NB_EPOCH_PER_DAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import fr.gouv.stopc.robert.server.batch.service.impl.BatchRegistrationServiceImpl;
import fr.gouv.stopc.robert.server.common.utils.TimeUtils;
import fr.gouv.stopc.robertserver.database.model.EpochExposition;
import fr.gouv.stopc.robertserver.database.model.Registration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class BatchRegistrationServiceTest {

    @InjectMocks
    BatchRegistrationServiceImpl batchRegistrationService;

    @Captor
    private ArgumentCaptor<ArrayList<Double>> acAggregateScores;

    @Mock
    ScoringStrategyService scoringStrategyService;

    @Test
    public void shouldExposedEpochsWithoutEpochsOlderThanContagiousPeriodFilterTooOldExposedEpochs(){

        int currentEpoch = 26543;

        //GIVEN
        int epochId = currentEpoch - (14 * 96) + new SecureRandom().nextInt(100) + 1;
        Double[] expositionsForFirstEpoch = new Double[] { 1.0 };
        Double[] expositionsForSecondEpoch = new Double[] { 12.5 };
        ArrayList<EpochExposition> expositions = new ArrayList<>();
        expositions.add(EpochExposition.builder()
                .epochId(epochId)
                .expositionScores(Arrays.asList(expositionsForFirstEpoch))
                .build());
        expositions.add(EpochExposition.builder()
                .epochId(epochId - (30 * 96))
                .expositionScores(Arrays.asList(expositionsForSecondEpoch))
                .build());


        //WHEN
        List<EpochExposition> filteredEpochExpositions = batchRegistrationService.getExposedEpochsWithoutEpochsOlderThanContagiousPeriod(expositions, currentEpoch, 14 , 900);

        //THEN
        assertThat(filteredEpochExpositions.size()).isEqualTo(1);
        assertThat(filteredEpochExpositions.get(0).getEpochId()).isEqualTo(epochId);
    }

    @Test
    public void shouldExposedEpochsWithoutEpochsOlderThanContagiousPeriodReturnsEmptyListInCaseEmptyListIsProvided(){

        //WHEN
        List<EpochExposition> filteredEpochExpositions = batchRegistrationService.getExposedEpochsWithoutEpochsOlderThanContagiousPeriod(new ArrayList<>(), 26543, 14 , 900);

        //THEN
        assertThat(filteredEpochExpositions.size()).isEqualTo(0);

    }

    @Test
    public void shouldExposedEpochsWithoutEpochsOlderThanContagiousPeriodReturnsEmptyListInCaseAllProvidedExposedEpochsAreTooOld(){

        int currentEpoch = 26543;

        //GIVEN
        int epochId = currentEpoch - (14 * 96) + new SecureRandom().nextInt(100) + 1;
        Double[] expositionsForFirstEpoch = new Double[] { 1.0 };
        ArrayList<EpochExposition> expositions = new ArrayList<>();
        expositions.add(EpochExposition.builder()
                .epochId(epochId - (30 * 96))
                .expositionScores(Arrays.asList(expositionsForFirstEpoch))
                .build());

        //WHEN
        List<EpochExposition> filteredEpochExpositions = batchRegistrationService.getExposedEpochsWithoutEpochsOlderThanContagiousPeriod(expositions, currentEpoch, 14 , 900);

        //THEN
        assertThat(filteredEpochExpositions.size()).isEqualTo(0);

    }

    @Test
    public void shouldUpdateRegistrationIfRiskSetRegistrationAtRisk() {

        //GIVEN
        long timeStart = TimeUtils.convertUnixMillistoNtpSeconds(System.currentTimeMillis()-(48*3600*1000));
        int currentEpoch = TimeUtils.getCurrentEpochFrom(timeStart);

        List<EpochExposition> exposedEpochs = new ArrayList<>();
        exposedEpochs.add(EpochExposition.builder().epochId(currentEpoch).expositionScores(Arrays.asList(0.5,0.4)).build());

        Registration registration = Registration.builder()
                .atRisk(false)
                .exposedEpochs(exposedEpochs)
                .build();

        when(scoringStrategyService.aggregate(anyList())).thenReturn(1.2);

        //WHEN
        boolean isAtRisk = batchRegistrationService.updateRegistrationIfRisk(registration, timeStart, 1.0);

        //THEN
        assertThat(isAtRisk).isTrue();
        assertThat(registration.getLatestRiskEpoch()).isEqualTo(currentEpoch);

    }

    @Test
    public void shouldUpdateRegistrationIfRiskFilteredOutScoresBeforeLastRiskEpoch() {

        //GIVEN
        long timeStart = TimeUtils.convertUnixMillistoNtpSeconds(System.currentTimeMillis()-(48*3600*1000));
        int currentEpoch = TimeUtils.getCurrentEpochFrom(timeStart);
        int latestRiskEpoch = currentEpoch - 5;


        List<EpochExposition> exposedEpochs = new ArrayList<>();
        exposedEpochs.add(EpochExposition.builder().epochId(currentEpoch).expositionScores(Arrays.asList(0.5,0.4)).build());
        exposedEpochs.add(EpochExposition.builder().epochId(currentEpoch - 60).expositionScores(Arrays.asList(0.1)).build());
        exposedEpochs.add(EpochExposition.builder().epochId(currentEpoch - 1).expositionScores(Arrays.asList(0.05)).build());

        Registration registration = Registration.builder()
                .atRisk(false)
                .latestRiskEpoch(latestRiskEpoch)
                .exposedEpochs(exposedEpochs)
                .build();


        when(scoringStrategyService.aggregate(anyList())).thenReturn(0.2);

        //WHEN
        boolean isAtRisk = batchRegistrationService.updateRegistrationIfRisk(registration, timeStart, 1.0);

        //THEN
        assertThat(isAtRisk).isFalse();
        assertThat(registration.getLatestRiskEpoch()).isEqualTo(latestRiskEpoch);

        verify(scoringStrategyService).aggregate(acAggregateScores.capture());
        List<ArrayList<Double>> aggregatedScores = acAggregateScores.getAllValues();
        assertThat(aggregatedScores.get(0)).containsExactly(0.9, 0.05);


    }

    @Test
    public void shouldNotTakeIntoAccountRandomizedLastContactDateIfItIsBeforeRegistrationLastContactDate() {
        //GIVEN
        long timeStart = TimeUtils.convertUnixMillistoNtpSeconds(System.currentTimeMillis()-(48*3600*1000));
        int currentEpoch = TimeUtils.getCurrentEpochFrom(timeStart);
        int lastContactDateFromExposedEpoch = currentEpoch;
        long realLastContactDateFromExposedEpoch = TimeUtils.getNtpSeconds(lastContactDateFromExposedEpoch, timeStart);
        long lastContactDateFromRegistration = TimeUtils.getNtpSeconds(currentEpoch - 2, timeStart);
        long randomizedLastContactDate = TimeUtils.getNtpSeconds(currentEpoch - NB_EPOCH_PER_DAY, timeStart);
        long truncateTimestamp = TimeUtils.dayTruncatedTimestamp(randomizedLastContactDate);
        int latestRiskEpoch = currentEpoch - 5;


        List<EpochExposition> exposedEpochs = new ArrayList<>();
        exposedEpochs.add(EpochExposition.builder().epochId(currentEpoch - 60).expositionScores(Arrays.asList(0.1)).build());
        exposedEpochs.add(EpochExposition.builder().epochId(lastContactDateFromExposedEpoch).expositionScores(Arrays.asList(0.05)).build());

        Registration registration = Registration.builder()
                .atRisk(false)
                .lastContactTimestamp(lastContactDateFromRegistration)
                .latestRiskEpoch(latestRiskEpoch)
                .exposedEpochs(exposedEpochs)
                .build();

        when(scoringStrategyService.aggregate(anyList())).thenReturn(1.2);

        try (MockedStatic<TimeUtils> mockedTimeUtils = Mockito.mockStatic(TimeUtils.class)) {
            //mock the static methods
            mockedTimeUtils.when(()-> TimeUtils.getNtpSeconds(lastContactDateFromExposedEpoch, timeStart)).thenReturn(realLastContactDateFromExposedEpoch);
            mockedTimeUtils.when(()-> TimeUtils.getRandomizedDateNotInFuture(realLastContactDateFromExposedEpoch)).thenReturn(randomizedLastContactDate);
            mockedTimeUtils.when(()-> TimeUtils.dayTruncatedTimestamp(randomizedLastContactDate)).thenReturn(truncateTimestamp);

            //WHEN
            batchRegistrationService.updateRegistrationIfRisk(registration, timeStart, 1.0);
        } // the static mock is not visible outside the try resource block


        //THEN
        assertThat(registration.getLastContactTimestamp()).isEqualTo(lastContactDateFromRegistration);
        assertThat(registration.isAtRisk()).isTrue();


    }
}
