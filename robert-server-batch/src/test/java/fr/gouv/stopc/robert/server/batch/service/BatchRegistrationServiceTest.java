package fr.gouv.stopc.robert.server.batch.service;

import fr.gouv.stopc.robert.server.batch.RobertServerBatchProperties;
import fr.gouv.stopc.robert.server.batch.RobertServerBatchProperties.RiskThreshold;
import fr.gouv.stopc.robert.server.batch.configuration.PropertyLoader;
import fr.gouv.stopc.robert.server.batch.service.impl.BatchRegistrationServiceImpl;
import fr.gouv.stopc.robert.server.common.service.RobertClock;
import fr.gouv.stopc.robertserver.database.model.EpochExposition;
import fr.gouv.stopc.robertserver.database.model.Registration;
import fr.gouv.stopc.robertserver.database.repository.BatchStatisticsRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.time.temporal.ChronoUnit.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.jupiter.api.DynamicContainer.dynamicContainer;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(ReplaceUnderscores.class)
@SpringBootTest
class BatchRegistrationServiceTest {

    BatchRegistrationService batchRegistrationService;

    @Captor
    ArgumentCaptor<List<Double>> acAggregateScores;

    @Mock
    ScoringStrategyService scoringStrategyService;

    @Autowired
    private BatchStatisticsService batchStatisticsService;

    @Autowired
    private BatchStatisticsRepository batchStatisticsRepository;

    final RobertClock robertClock = new RobertClock("2020-06-01");

    @BeforeEach
    public void setUp() {
        final var properties = new RobertServerBatchProperties(
                new RiskThreshold(Duration.ofDays(11))
        );
        final var propertyLoader = mock(PropertyLoader.class, withSettings().lenient());
        when(propertyLoader.getRiskLevelRetentionPeriodInDays()).thenReturn(7);
        batchRegistrationService = new BatchRegistrationServiceImpl(
                scoringStrategyService, propertyLoader, properties, robertClock, batchStatisticsService
        );
        batchStatisticsRepository.deleteAll();
    }

    @Test
    void getExposedEpochsWithoutEpochsOlderThanContagiousPeriod_should_remove_too_old_exposed_epochs() {

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
    void getExposedEpochsWithoutEpochsOlderThanContagiousPeriod_should_return_empty_list_when_it_receives_an_empty_list() {

        // WHEN
        final var filteredEpochExpositions = batchRegistrationService
                .getExposedEpochsWithoutEpochsOlderThanContagiousPeriod(List.of(), 26543, 14, 900);

        // THEN
        assertThat(filteredEpochExpositions).isEmpty();
    }

    @Test
    void getExposedEpochsWithoutEpochsOlderThanContagiousPeriod_should_return_empty_list_when_it_receives_too_old_epochs() {

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
    void updateRegistrationIfRisk_should_set_registration_at_risk() {

        // GIVEN
        final var currentEpoch = robertClock.now().asEpochId();
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
        batchRegistrationService.updateRegistrationIfRisk(
                registration, robertClock.getStartNtpTimestamp(), 1.0, Instant.parse("2021-01-01T12:30:00Z")
        );

        // THEN
        assertThat(registration.isAtRisk())
                .as("registration risk status")
                .isTrue();
        assertThat(registration.getLatestRiskEpoch())
                .as("registration latest risk epoch")
                .isEqualTo(currentEpoch);
    }

    @Test
    void updateRegistrationIfRisk_should_filter_out_scores_before_last_risk_epoch() {

        // GIVEN
        final var currentEpoch = robertClock.now().asEpochId();
        final var latestRiskEpoch = currentEpoch - 5;
        final var registration = Registration.builder()
                .atRisk(false)
                .latestRiskEpoch(latestRiskEpoch)
                .exposedEpochs(
                        List.of(
                                EpochExposition.builder()
                                        .epochId(currentEpoch)
                                        .expositionScores(List.of(0.5, 0.4))
                                        .build(),
                                EpochExposition.builder()
                                        .epochId(currentEpoch - 60)
                                        .expositionScores(List.of(0.1))
                                        .build(),
                                EpochExposition.builder()
                                        .epochId(currentEpoch - 1)
                                        .expositionScores(List.of(0.05))
                                        .build()
                        )
                )
                .build();

        when(scoringStrategyService.aggregate(anyList())).thenReturn(0.2);

        // WHEN
        batchRegistrationService.updateRegistrationIfRisk(
                registration, robertClock.getStartNtpTimestamp(), 1.0, Instant.parse("2021-01-01T12:30:00Z")
        );

        // THEN
        assertThat(registration.isAtRisk())
                .as("registration risk status")
                .isFalse();
        assertThat(registration.getLatestRiskEpoch())
                .as("registration latest risk epoch")
                .isEqualTo(latestRiskEpoch);

        verify(scoringStrategyService).aggregate(acAggregateScores.capture());
        assertThat(acAggregateScores.getAllValues())
                .containsExactly(List.of(0.9, 0.05));
    }

    @Test
    void updateRegistrationIfRisk_should_ignore_randomized_last_contact_date_if_it_is_before_registration_lastContactDate() {
        // GIVEN
        final var currentEpoch = robertClock.now().asEpochId();
        final var lastContactDateFromExposedEpoch = currentEpoch;
        final var lastContactDateFromRegistration = robertClock.now().minusEpochs(2).asNtpTimestamp();
        final var latestRiskEpoch = currentEpoch - 5;

        final var registration = Registration.builder()
                .atRisk(false)
                .lastContactTimestamp(lastContactDateFromRegistration)
                .latestRiskEpoch(latestRiskEpoch)
                .exposedEpochs(
                        List.of(
                                EpochExposition.builder()
                                        .epochId(currentEpoch - 60)
                                        .expositionScores(List.of(0.1))
                                        .build(),
                                EpochExposition.builder()
                                        .epochId(lastContactDateFromExposedEpoch)
                                        .expositionScores(List.of(0.05))
                                        .build()
                        )
                )
                .build();

        when(scoringStrategyService.aggregate(anyList())).thenReturn(1.2);

        // WHEN
        batchRegistrationService.updateRegistrationIfRisk(
                registration, robertClock.getStartNtpTimestamp(), 1.0, Instant.parse("2021-01-01T12:30:00Z")
        );

        // THEN
        assertThat(registration.isAtRisk())
                .as("registration risk status")
                .isTrue();
        assertThat(registration.getLastContactTimestamp())
                .as("last contact timestamp")
                .isEqualTo(lastContactDateFromRegistration);
        assertThat(registration.isAtRisk())
                .as("registration risk status")
                .isTrue();
    }

    @Test
    void updateRegistrationIfRisk_should_ignore_risk_when_last_contact_occurred_before_riskThresholdLastContactDelay() {
        // GIVEN
        final var expositionEpoch = robertClock.now()
                .minus(11, DAYS)
                .minus(1, MINUTES)
                .asEpochId();
        final var registration = Registration.builder()
                .atRisk(false)
                .exposedEpochs(
                        List.of(
                                EpochExposition.builder()
                                        .epochId(expositionEpoch)
                                        .expositionScores(List.of(0.5, 0.4))
                                        .build()
                        )
                )
                .build();

        when(scoringStrategyService.aggregate(anyList())).thenReturn(1.2);

        // WHEN
        batchRegistrationService.updateRegistrationIfRisk(
                registration, robertClock.getStartNtpTimestamp(), 1.0, Instant.parse("2021-01-01T12:30:00Z")
        );

        // THEN
        assertThat(registration.isAtRisk())
                .as("registration risk status")
                .isFalse();

    }

    @Test
    void should_increment_usersAboveRiskThresholdButRetentionPeriodExpired_stat() {
        final var nowMinus12Days = robertClock.now()
                .truncatedTo(DAYS)
                .minus(12, DAYS);

        final var registration1 = Registration.builder()
                .atRisk(true)
                .isNotified(true)
                .latestRiskEpoch(nowMinus12Days.asEpochId())
                .lastContactTimestamp(nowMinus12Days.asNtpTimestamp())
                .exposedEpochs(
                        List.of(
                                EpochExposition.builder()
                                        .epochId(nowMinus12Days.asEpochId())
                                        .expositionScores(List.of(0.1))
                                        .build()
                        )
                )
                .build();
        final var registration2 = Registration.builder()
                .atRisk(true)
                .isNotified(true)
                .latestRiskEpoch(nowMinus12Days.asEpochId())
                .lastContactTimestamp(nowMinus12Days.asNtpTimestamp())
                .exposedEpochs(
                        List.of(
                                EpochExposition.builder()
                                        .epochId(nowMinus12Days.asEpochId())
                                        .expositionScores(List.of(0.1))
                                        .build()
                        )
                )
                .build();

        when(scoringStrategyService.aggregate(anyList())).thenReturn(1.2);

        batchRegistrationService.updateRegistrationIfRisk(
                registration1, robertClock.getStartNtpTimestamp(), 1.0, Instant.parse("2021-01-01T12:30:00Z")
        );
        batchRegistrationService.updateRegistrationIfRisk(
                registration2, robertClock.getStartNtpTimestamp(), 1.0, Instant.parse("2021-01-01T12:30:00Z")
        );

        assertThat(batchStatisticsRepository.findAll())
                .extracting(
                        stat -> tuple(
                                stat.getJobStartInstant(),
                                stat.getUsersAboveRiskThresholdButRetentionPeriodExpired()
                        )
                )
                .containsExactly(
                        tuple(Instant.parse("2021-01-01T12:30:00Z"), 2L)
                );
    }

    @Test
    void should_not_increment_usersAboveRiskThresholdButRetentionPeriodExpired_stat() {
        final var nowMinus6Days = robertClock.now()
                .truncatedTo(DAYS)
                .minus(6, DAYS);

        final var registration1 = Registration.builder()
                .atRisk(true)
                .isNotified(true)
                .latestRiskEpoch(nowMinus6Days.asEpochId())
                .exposedEpochs(
                        List.of(
                                EpochExposition.builder()
                                        .epochId(nowMinus6Days.asEpochId())
                                        .expositionScores(List.of(0.1))
                                        .build()
                        )
                )
                .lastContactTimestamp(nowMinus6Days.asNtpTimestamp())

                .build();
        final var registration2 = Registration.builder()
                .atRisk(true)
                .isNotified(true)
                .latestRiskEpoch(nowMinus6Days.asEpochId())
                .lastContactTimestamp(nowMinus6Days.asNtpTimestamp())
                .exposedEpochs(
                        List.of(
                                EpochExposition.builder()
                                        .epochId(nowMinus6Days.asEpochId())
                                        .expositionScores(List.of(0.1))
                                        .build()
                        )
                )
                .build();

        when(scoringStrategyService.aggregate(anyList())).thenReturn(1.2);

        batchRegistrationService.updateRegistrationIfRisk(
                registration1, robertClock.getStartNtpTimestamp(), 1.0, Instant.parse("2021-01-01T12:30:00Z")
        );
        batchRegistrationService.updateRegistrationIfRisk(
                registration2, robertClock.getStartNtpTimestamp(), 1.0, Instant.parse("2021-01-01T12:30:00Z")
        );

        assertThat(batchStatisticsRepository.findAll()).isEmpty();
    }

    @Test
    void should_increment_usersAboveRiskThresholdButRetentionPeriodExpired_stat_at_threshold() {
        final var nowMinus11Days = robertClock.now()
                .truncatedTo(DAYS)
                .minus(11, DAYS);

        final var registration1 = Registration.builder()
                .atRisk(true)
                .isNotified(true)
                .latestRiskEpoch(nowMinus11Days.asEpochId())
                .lastContactTimestamp(nowMinus11Days.asNtpTimestamp())
                .exposedEpochs(
                        List.of(
                                EpochExposition.builder()
                                        .epochId(nowMinus11Days.asEpochId())
                                        .expositionScores(List.of(0.1))
                                        .build()
                        )
                )
                .build();
        final var registration2 = Registration.builder()
                .atRisk(true)
                .isNotified(true)
                .latestRiskEpoch(nowMinus11Days.asEpochId())
                .lastContactTimestamp(nowMinus11Days.asNtpTimestamp())
                .exposedEpochs(
                        List.of(
                                EpochExposition.builder()
                                        .epochId(nowMinus11Days.asEpochId())
                                        .expositionScores(List.of(0.1))
                                        .build()
                        )
                )
                .build();

        when(scoringStrategyService.aggregate(anyList())).thenReturn(1.2);

        batchRegistrationService.updateRegistrationIfRisk(
                registration1, robertClock.getStartNtpTimestamp(), 1.0, Instant.parse("2021-01-01T12:30:00Z")
        );
        batchRegistrationService.updateRegistrationIfRisk(
                registration2, robertClock.getStartNtpTimestamp(), 1.0, Instant.parse("2021-01-01T12:30:00Z")
        );

        assertThat(batchStatisticsRepository.findAll())
                .extracting(
                        stat -> tuple(
                                stat.getJobStartInstant(),
                                stat.getUsersAboveRiskThresholdButRetentionPeriodExpired()
                        )
                )
                .containsExactly(
                        tuple(Instant.parse("2021-01-01T12:30:00Z"), 2L)
                );
    }

    @Test
    void should_increment_usersAboveRiskThresholdButRetentionPeriodExpired_stat_same_day_under_threshold() {
        final var nowMinus11DaysAnd1Minutes = robertClock.now()
                .truncatedTo(DAYS)
                .minus(11, DAYS)
                .minus(1, MINUTES);

        final var registration1 = Registration.builder()
                .atRisk(true)
                .isNotified(true)
                .latestRiskEpoch(nowMinus11DaysAnd1Minutes.asEpochId())
                .lastContactTimestamp(nowMinus11DaysAnd1Minutes.asNtpTimestamp())
                .exposedEpochs(
                        List.of(
                                EpochExposition.builder()
                                        .epochId(
                                                nowMinus11DaysAnd1Minutes.asEpochId()
                                        )
                                        .expositionScores(List.of(0.1))
                                        .build()
                        )
                )
                .build();
        final var registration2 = Registration.builder()
                .atRisk(true)
                .isNotified(true)
                .latestRiskEpoch(nowMinus11DaysAnd1Minutes.asEpochId())
                .lastContactTimestamp(nowMinus11DaysAnd1Minutes.asNtpTimestamp())
                .exposedEpochs(
                        List.of(
                                EpochExposition.builder()
                                        .epochId(
                                                nowMinus11DaysAnd1Minutes.asEpochId()
                                        )
                                        .expositionScores(List.of(0.1))
                                        .build()
                        )
                )
                .build();

        when(scoringStrategyService.aggregate(anyList())).thenReturn(1.2);

        batchRegistrationService.updateRegistrationIfRisk(
                registration1, robertClock.getStartNtpTimestamp(), 1.0, Instant.parse("2021-01-01T12:30:00Z")
        );
        batchRegistrationService.updateRegistrationIfRisk(
                registration2, robertClock.getStartNtpTimestamp(), 1.0, Instant.parse("2021-01-01T12:30:00Z")
        );

        assertThat(batchStatisticsRepository.findAll())
                .extracting(
                        stat -> tuple(
                                stat.getJobStartInstant(),
                                stat.getUsersAboveRiskThresholdButRetentionPeriodExpired()
                        )
                )
                .containsExactly(
                        tuple(Instant.parse("2021-01-01T12:30:00Z"), 2L)
                );
    }

    @Test
    void should_increment_usersAboveRiskThresholdButRetentionPeriodExpired_stat_same_day_above_threshold() {
        final var nowMinus11DaysAnd59Minutes = robertClock.now()
                .truncatedTo(DAYS)
                .minus(11, DAYS)
                .minus(12, HOURS);

        final var registration1 = Registration.builder()
                .atRisk(true)
                .isNotified(true)
                .latestRiskEpoch(nowMinus11DaysAnd59Minutes.asEpochId())
                .lastContactTimestamp(nowMinus11DaysAnd59Minutes.asNtpTimestamp())
                .exposedEpochs(
                        List.of(
                                EpochExposition.builder()
                                        .epochId(
                                                nowMinus11DaysAnd59Minutes.asEpochId()
                                        )
                                        .expositionScores(List.of(0.1))
                                        .build()
                        )
                )
                .build();
        final var registration2 = Registration.builder()
                .atRisk(true)
                .isNotified(true)
                .latestRiskEpoch(nowMinus11DaysAnd59Minutes.asEpochId())
                .lastContactTimestamp(nowMinus11DaysAnd59Minutes.asNtpTimestamp())
                .exposedEpochs(
                        List.of(
                                EpochExposition.builder()
                                        .epochId(
                                                nowMinus11DaysAnd59Minutes.asEpochId()
                                        )
                                        .expositionScores(List.of(0.1))
                                        .build()
                        )
                )
                .build();

        when(scoringStrategyService.aggregate(anyList())).thenReturn(1.2);

        batchRegistrationService.updateRegistrationIfRisk(
                registration1, robertClock.getStartNtpTimestamp(), 1.0, Instant.parse("2021-01-01T12:30:00Z")
        );
        batchRegistrationService.updateRegistrationIfRisk(
                registration2, robertClock.getStartNtpTimestamp(), 1.0, Instant.parse("2021-01-01T12:30:00Z")
        );

        assertThat(batchStatisticsRepository.findAll())
                .extracting(
                        stat -> tuple(
                                stat.getJobStartInstant(),
                                stat.getUsersAboveRiskThresholdButRetentionPeriodExpired()
                        )
                )
                .containsExactly(
                        tuple(Instant.parse("2021-01-01T12:30:00Z"), 2L)
                );
    }

    // don't see another way to test random results than repeating execution 🤞
    @Nested
    class RandomLastContactDateTest {

        Registration registration;

        @BeforeEach
        void initialize_a_registration() {
            registration = Registration.builder()
                    .atRisk(false)
                    .lastContactTimestamp(0)
                    .latestRiskEpoch(0)
                    .exposedEpochs(
                            List.of(
                                    EpochExposition.builder()
                                            .epochId(0)
                                            .expositionScores(List.of(0.1))
                                            .build()
                            )
                    )
                    .build();

            when(scoringStrategyService.aggregate(anyList())).thenReturn(1.2);
        }

        @RepeatedTest(100)
        void should_be_either_today_or_yesterday_when_most_recent_epoch_exposition_is_in_the_future() {
            final var yesterday = Instant.now().minus(1, DAYS).truncatedTo(DAYS);
            final var today = Instant.now().truncatedTo(DAYS);
            // given
            registration.getExposedEpochs().get(0)
                    .setEpochId(robertClock.now().plus(2, MINUTES).asEpochId());
            // when
            batchRegistrationService.updateRegistrationIfRisk(
                    registration, robertClock.getStartNtpTimestamp(), 1.0, Instant.parse("2021-01-01T12:30:00Z")
            );
            // then
            final var lastContact = robertClock.atNtpTimestamp(registration.getLastContactTimestamp());
            assertThat(lastContact.asInstant())
                    .isIn(yesterday, today);
        }

        @RepeatedTest(100)
        void should_be_either_today_or_yesterday_when_most_recent_epoch_exposition_is_today() {
            final var yesterday = Instant.now().minus(1, DAYS).truncatedTo(DAYS);
            final var today = Instant.now().truncatedTo(DAYS);
            // given
            registration.getExposedEpochs().get(0)
                    .setEpochId(robertClock.now().asEpochId());
            // when
            batchRegistrationService.updateRegistrationIfRisk(
                    registration, robertClock.getStartNtpTimestamp(), 1.0, Instant.parse("2021-01-01T12:30:00Z")
            );
            // then
            final var lastContact = robertClock.atNtpTimestamp(registration.getLastContactTimestamp());
            assertThat(lastContact.asInstant())
                    .isIn(yesterday, today);
        }

        @RepeatedTest(100)
        void should_be_either_7daysAgo_or_6daysAgo_when_most_recent_epoch_exposition_is_7_days_ago() {
            final var sevenDaysAgo = Instant.now().minus(7, DAYS).truncatedTo(DAYS);
            final var sixDaysAgo = Instant.now().minus(6, DAYS).truncatedTo(DAYS);
            // given
            registration.getExposedEpochs().get(0)
                    .setEpochId(robertClock.now().minus(7, DAYS).asEpochId());
            // when
            batchRegistrationService.updateRegistrationIfRisk(
                    registration, robertClock.getStartNtpTimestamp(), 1.0, Instant.parse("2021-01-01T12:30:00Z")
            );
            // then
            final var lastContact = robertClock.atNtpTimestamp(registration.getLastContactTimestamp());
            assertThat(lastContact.asInstant())
                    .isIn(sevenDaysAgo, sixDaysAgo);
        }

        @RepeatedTest(100)
        void should_be_either_9daysAgo_or_8daysAgo_when_most_recent_epoch_exposition_is_8_days_ago() {
            final var nineDaysAgo = Instant.now().minus(9, DAYS).truncatedTo(DAYS);
            final var eightDaysAgo = Instant.now().minus(8, DAYS).truncatedTo(DAYS);
            // given
            registration.getExposedEpochs().get(0)
                    .setEpochId(robertClock.now().minus(8, DAYS).asEpochId());
            // when
            batchRegistrationService.updateRegistrationIfRisk(
                    registration, robertClock.getStartNtpTimestamp(), 1.0, Instant.parse("2021-01-01T12:30:00Z")
            );
            // then
            final var lastContact = robertClock.atNtpTimestamp(registration.getLastContactTimestamp());
            assertThat(lastContact.asInstant())
                    .isIn(nineDaysAgo, eightDaysAgo);
        }

        @TestFactory
        Stream<DynamicContainer> should_be_one_day_around_most_recent_epoch_exposition() {
            final var expositionDays = IntStream.concat(
                    IntStream.rangeClosed(1, 6),
                    IntStream.rangeClosed(9, 10)
            );
            return expositionDays.mapToObj(expositionDay -> {
                final var repeatedTests = IntStream.rangeClosed(1, 100)
                        .mapToObj(repetition -> dynamicTest("repetition " + repetition, () -> {
                            initialize_a_registration();
                            random_date_should_be_one_day_around_most_recent_epoch_exposition(expositionDay);
                        }));
                return dynamicContainer("when expositionDay is d-" + expositionDay, repeatedTests);
            });
        }

        void random_date_should_be_one_day_around_most_recent_epoch_exposition(final int expositionDay) {
            final var lowerBound = Instant.now().minus(expositionDay + 1, DAYS).truncatedTo(DAYS);
            final var upperBound = Instant.now().minus(expositionDay - 1, DAYS).truncatedTo(DAYS);
            // given
            final var lastExposition = robertClock.now().minus(expositionDay, DAYS);
            registration.getExposedEpochs().get(0)
                    .setEpochId(lastExposition.asEpochId());
            // when
            batchRegistrationService.updateRegistrationIfRisk(
                    registration, robertClock.getStartNtpTimestamp(),
                    1.0,
                    Instant.parse("2021-01-01T12:30:00Z")
            );
            // then
            final var lastContact = robertClock.atNtpTimestamp(registration.getLastContactTimestamp());
            assertThat(lastContact.asInstant())
                    .as("random last contact date for d-%d (%s) ", expositionDay, lastExposition)
                    .isIn(lowerBound, lastExposition.asInstant().truncatedTo(DAYS), upperBound);
        }
    }
}
