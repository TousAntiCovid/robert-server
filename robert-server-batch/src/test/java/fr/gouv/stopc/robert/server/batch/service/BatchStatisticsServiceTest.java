package fr.gouv.stopc.robert.server.batch.service;

import fr.gouv.stopc.robert.server.batch.test.IntegrationTest;
import fr.gouv.stopc.robert.server.common.service.RobertClock;
import fr.gouv.stopc.robertserver.database.model.BatchStatistics;
import fr.gouv.stopc.robertserver.database.model.EpochExposition;
import fr.gouv.stopc.robertserver.database.model.Registration;
import fr.gouv.stopc.robertserver.database.repository.BatchStatisticsRepository;
import fr.gouv.stopc.robertserver.database.repository.RegistrationRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.tuple;

@IntegrationTest
class BatchStatisticsServiceTest {

    @Autowired
    private BatchStatisticsService batchStatisticsService;

    @Autowired
    private BatchStatisticsRepository batchStatisticsRepository;

    @Autowired
    private RegistrationRepository registrationRepository;

    @Autowired
    private RobertClock clock;

    private Instant batchExecutionTime;

    @BeforeEach
    public void init() {
        batchExecutionTime = Instant.parse("2021-01-01T12:30:00Z");
        batchStatisticsRepository.deleteAll();
        registrationRepository.deleteAll();
    }

    @Test
    void should_save_a_batch_statistic_with_3_nbExposedUsersButNotAtRisk() {

        // given
        givenThereIsABatchStatisticForToday();
        givenThereAreThreeExposedRegistrationsAtRiskAndNotNotified();

        // when
        batchStatisticsService.saveNbExposedButNotAtRiskUsersInStatistics(batchExecutionTime);

        // then
        Assertions.assertThat(batchStatisticsRepository.findAll())
                .extracting(
                        stat -> tuple(
                                stat.getBatchExecution(),
                                stat.getNbExposedButNotAtRiskUsers()
                        )
                )
                .containsExactly(
                        tuple(Instant.parse("2021-01-01T12:30:00Z"), 3L)
                );
    }

    @Test
    void should_save_a_batch_statistic_with_3_nbNotifiedUsersScoredAgain() {

        // given
        givenThereIsABatchStatisticForToday();
        givenThereAreThreeExposedRegistrationsAtRiskAndNotified();

        // when
        batchStatisticsService.saveNbNotifiedUsersScoredAgainInStatistics(batchExecutionTime);

        // then
        Assertions.assertThat(batchStatisticsRepository.findAll())
                .extracting(
                        stat -> tuple(
                                stat.getBatchExecution(),
                                stat.getNbNotifiedUsersScoredAgain()
                        )
                )
                .containsExactly(
                        tuple(Instant.parse("2021-01-01T12:30:00Z"), 3L)
                );
    }

    private void givenThereIsABatchStatisticForToday() {
        batchStatisticsRepository.save(
                BatchStatistics.builder()
                        .batchExecution(batchExecutionTime)
                        .nbExposedButNotAtRiskUsers(0L)
                        .usersAboveRiskThresholdButRetentionPeriodExpired(0L)
                        .nbExposedButNotAtRiskUsers(0L)
                        .build()
        );
    }

    private void givenThereAreThreeExposedRegistrationsAtRiskAndNotified() {
        registrationRepository.saveAll(
                List.of(
                        Registration.builder()
                                .permanentIdentifier("id_1".getBytes())
                                .atRisk(false)
                                .isNotified(true)
                                .exposedEpochs(
                                        List.of(
                                                EpochExposition.builder()
                                                        .epochId(clock.now().asEpochId())
                                                        .expositionScores(List.of(1.0, 1.2))
                                                        .build()
                                        )
                                )
                                .build(),
                        Registration.builder()
                                .permanentIdentifier("id_2".getBytes())
                                .atRisk(false)
                                .isNotified(true)
                                .exposedEpochs(
                                        List.of(
                                                EpochExposition.builder()
                                                        .epochId(clock.now().asEpochId())
                                                        .expositionScores(List.of(1.0, 1.2))
                                                        .build()
                                        )
                                )
                                .build(),
                        Registration.builder()
                                .permanentIdentifier("id_3".getBytes())
                                .atRisk(false)
                                .isNotified(true)
                                .exposedEpochs(
                                        List.of(
                                                EpochExposition.builder()
                                                        .epochId(clock.now().asEpochId())
                                                        .expositionScores(List.of(1.0, 1.2))
                                                        .build()
                                        )
                                )
                                .build()
                )
        );
    }

    private void givenThereAreThreeExposedRegistrationsAtRiskAndNotNotified() {
        registrationRepository.saveAll(
                List.of(
                        Registration.builder()
                                .permanentIdentifier("id_1".getBytes())
                                .atRisk(false)
                                .isNotified(false)
                                .exposedEpochs(
                                        List.of(
                                                EpochExposition.builder()
                                                        .epochId(clock.now().asEpochId())
                                                        .expositionScores(List.of(1.0, 1.2))
                                                        .build()
                                        )
                                )
                                .build(),
                        Registration.builder()
                                .permanentIdentifier("id_2".getBytes())
                                .atRisk(false)
                                .isNotified(false)
                                .exposedEpochs(
                                        List.of(
                                                EpochExposition.builder()
                                                        .epochId(clock.now().asEpochId())
                                                        .expositionScores(List.of(1.0, 1.2))
                                                        .build()
                                        )
                                )
                                .build(),
                        Registration.builder()
                                .permanentIdentifier("id_3".getBytes())
                                .atRisk(false)
                                .isNotified(false)
                                .exposedEpochs(
                                        List.of(
                                                EpochExposition.builder()
                                                        .epochId(clock.now().asEpochId())
                                                        .expositionScores(List.of(1.0, 1.2))
                                                        .build()
                                        )
                                )
                                .build()
                )
        );
    }
}
