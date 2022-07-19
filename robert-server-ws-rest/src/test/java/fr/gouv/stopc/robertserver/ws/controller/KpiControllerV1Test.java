package fr.gouv.stopc.robertserver.ws.controller;

import fr.gouv.stopc.robertserver.database.model.BatchStatistics;
import fr.gouv.stopc.robertserver.database.model.EpochExposition;
import fr.gouv.stopc.robertserver.database.model.Registration;
import fr.gouv.stopc.robertserver.database.model.WebserviceStatistics;
import fr.gouv.stopc.robertserver.database.repository.BatchStatisticsRepository;
import fr.gouv.stopc.robertserver.database.repository.RegistrationRepository;
import fr.gouv.stopc.robertserver.database.repository.WebserviceStatisticsRepository;
import fr.gouv.stopc.robertserver.ws.test.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.time.LocalDate;
import java.util.List;

import static fr.gouv.stopc.robertserver.ws.test.MongodbManager.givenMongodbIsOffline;
import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static java.time.LocalDate.now;
import static java.time.ZoneOffset.UTC;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.hamcrest.Matchers.*;
import static org.springframework.http.HttpStatus.ACCEPTED;
import static org.springframework.http.HttpStatus.OK;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;
import static org.testcontainers.shaded.org.awaitility.pollinterval.FibonacciPollInterval.fibonacci;

@IntegrationTest
@ExtendWith(OutputCaptureExtension.class)
class KpiControllerV1Test {

    @Autowired
    private RegistrationRepository registrationRepository;

    @Autowired
    private BatchStatisticsRepository batchStatisticsRepository;

    @Autowired
    private WebserviceStatisticsRepository webserviceStatisticsRepository;

    @BeforeEach
    void initialize_some_registrations_to_produce_total_statistics() {
        registrationRepository.deleteAll();
        registrationRepository.saveAll(
                List.of(
                        Registration.builder()
                                .permanentIdentifier("user1".getBytes())
                                .atRisk(false)
                                .build(),
                        Registration.builder()
                                .permanentIdentifier("user2".getBytes())
                                .atRisk(false)
                                .isNotified(true)
                                .build(),
                        Registration.builder()
                                .permanentIdentifier("user2".getBytes())
                                .atRisk(false)
                                .isNotified(true)
                                .exposedEpochs(
                                        List.of(
                                                EpochExposition.builder()
                                                        .epochId(99)
                                                        .expositionScores(List.of(1.0))
                                                        .build()
                                        )
                                )
                                .build(),
                        Registration.builder()
                                .permanentIdentifier("user3".getBytes())
                                .atRisk(true)
                                .build(),
                        Registration.builder()
                                .permanentIdentifier("user4".getBytes())
                                .atRisk(true)
                                .isNotified(true)
                                .build()
                )
        );
    }

    @BeforeEach
    void initialize_batch_statistics() {
        batchStatisticsRepository.deleteAll();
        batchStatisticsRepository.saveAll(
                List.of(
                        BatchStatistics.builder()
                                .jobStartInstant(now().minusDays(4).atTime(2, 0).toInstant(UTC))
                                .usersAboveRiskThresholdButRetentionPeriodExpired(1L)
                                .build(),
                        BatchStatistics.builder()
                                .jobStartInstant(now().minusDays(4).atTime(14, 0).toInstant(UTC))
                                .usersAboveRiskThresholdButRetentionPeriodExpired(1L)
                                .build(),
                        BatchStatistics.builder()
                                .jobStartInstant(now().minusDays(3).atTime(2, 0).toInstant(UTC))
                                .usersAboveRiskThresholdButRetentionPeriodExpired(2L)
                                .build(),
                        BatchStatistics.builder()
                                .jobStartInstant(now().minusDays(3).atTime(14, 0).toInstant(UTC))
                                .usersAboveRiskThresholdButRetentionPeriodExpired(2L)
                                .build(),
                        BatchStatistics.builder()
                                .jobStartInstant(now().minusDays(2).atTime(2, 0).toInstant(UTC))
                                .usersAboveRiskThresholdButRetentionPeriodExpired(3L)
                                .build(),
                        BatchStatistics.builder()
                                .jobStartInstant(now().minusDays(2).atTime(15, 0).toInstant(UTC))
                                .usersAboveRiskThresholdButRetentionPeriodExpired(3L)
                                .build(),
                        BatchStatistics.builder()
                                .jobStartInstant(now().minusDays(1).atTime(2, 0).toInstant(UTC))
                                .usersAboveRiskThresholdButRetentionPeriodExpired(4L)
                                .build(),
                        BatchStatistics.builder()
                                .jobStartInstant(now().minusDays(1).atTime(14, 30).toInstant(UTC))
                                .usersAboveRiskThresholdButRetentionPeriodExpired(4L)
                                .build(),
                        BatchStatistics.builder()
                                .jobStartInstant(now().atTime(2, 0).toInstant(UTC))
                                .usersAboveRiskThresholdButRetentionPeriodExpired(90L)
                                .build()
                )
        );
    }

    @BeforeEach
    void initialize_webservice_statistics() {
        webserviceStatisticsRepository.deleteAll();
        webserviceStatisticsRepository.saveAll(
                List.of(
                        WebserviceStatistics.builder()
                                .date(now().minusDays(4).atStartOfDay().toInstant(UTC))
                                .totalAlertedUsers(10L)
                                .totalExposedButNotAtRiskUsers(5L)
                                .totalInfectedUsersNotNotified(3L)
                                .totalNotifiedUsersScoredAgain(2L)
                                .notifiedUsers(1L)
                                .build(),
                        WebserviceStatistics.builder()
                                .date(now().minusDays(3).atStartOfDay().toInstant(UTC))
                                .totalAlertedUsers(11L)
                                .totalExposedButNotAtRiskUsers(6L)
                                .totalInfectedUsersNotNotified(4L)
                                .totalNotifiedUsersScoredAgain(3L)
                                .notifiedUsers(1L)
                                .build(),
                        WebserviceStatistics.builder()
                                .date(now().minusDays(2).atStartOfDay().toInstant(UTC))
                                .totalAlertedUsers(12L)
                                .totalExposedButNotAtRiskUsers(7L)
                                .totalInfectedUsersNotNotified(5L)
                                .totalNotifiedUsersScoredAgain(4L)
                                .notifiedUsers(2L)
                                .build(),
                        WebserviceStatistics.builder()
                                .date(now().minusDays(1).atStartOfDay().toInstant(UTC))
                                .totalAlertedUsers(12L)
                                .totalExposedButNotAtRiskUsers(8L)
                                .totalInfectedUsersNotNotified(6L)
                                .totalNotifiedUsersScoredAgain(5L)
                                .notifiedUsers(0L)
                                .build(),
                        WebserviceStatistics.builder()
                                .date(now().atStartOfDay().toInstant(UTC))
                                .totalAlertedUsers(99L)
                                .totalExposedButNotAtRiskUsers(98L)
                                .totalInfectedUsersNotNotified(97L)
                                .totalNotifiedUsersScoredAgain(96L)
                                .notifiedUsers(95L)
                                .build()
                )
        );
    }

    @Test
    void can_fetch_statistics_for_a_single_day() {
        given()
                .params(
                        "fromDate", now().minusDays(1).toString(),
                        "toDate", now().minusDays(1).toString()
                )

                .when()
                .get("/internal/api/v1/kpi")

                .then()
                .statusCode(OK.value())
                .body("[0].date", equalTo(LocalDate.now().minusDays(1).toString()))
                .body("[0].nbAlertedUsers", equalTo(12))
                .body("[0].nbExposedButNotAtRiskUsers", equalTo(8))
                .body("[0].nbInfectedUsersNotNotified", equalTo(6))
                .body("[0].nbNotifiedUsersScoredAgain", equalTo(5))
                .body("[0].notifiedUsers", equalTo(0))
                .body("[0].usersAboveRiskThresholdButRetentionPeriodExpired", equalTo(8))
                .body("size()", equalTo(1));

    }

    @Test
    void can_fetch_statistics_for_a_single_day_no_ws_stats() {

        webserviceStatisticsRepository.deleteAll();

        given()
                .params(
                        "fromDate", now().minusDays(1).toString(),
                        "toDate", now().minusDays(1).toString()
                )

                .when()
                .get("/internal/api/v1/kpi")

                .then()
                .statusCode(OK.value())
                .body("[0].date", equalTo(LocalDate.now().minusDays(1).toString()))
                .body("[0].nbAlertedUsers", nullValue())
                .body("[0].nbExposedButNotAtRiskUsers", nullValue())
                .body("[0].nbInfectedUsersNotNotified", nullValue())
                .body("[0].nbNotifiedUsersScoredAgain", nullValue())
                .body("[0].notifiedUsers", equalTo(0))
                .body("[0].usersAboveRiskThresholdButRetentionPeriodExpired", equalTo(8))
                .body("size()", equalTo(1));

    }

    @Test
    void can_fetch_statistics_for_a_single_day_no_batch_stats() {

        batchStatisticsRepository.deleteAll();

        given()
                .params(
                        "fromDate", now().minusDays(1).toString(),
                        "toDate", now().minusDays(1).toString()
                )

                .when()
                .get("/internal/api/v1/kpi")

                .then()
                .statusCode(OK.value())
                .body("[0].date", equalTo(LocalDate.now().minusDays(1).toString()))
                .body("[0].nbAlertedUsers", equalTo(12))
                .body("[0].nbExposedButNotAtRiskUsers", equalTo(8))
                .body("[0].nbInfectedUsersNotNotified", equalTo(6))
                .body("[0].nbNotifiedUsersScoredAgain", equalTo(5))
                .body("[0].notifiedUsers", equalTo(0))
                .body("[0].usersAboveRiskThresholdButRetentionPeriodExpired", equalTo(0))
                .body("size()", equalTo(1));

    }

    @Test
    void can_fetch_statistics_when_no_stats_exists() {
        given()
                .params(
                        "fromDate", now().minusDays(10).toString(),
                        "toDate", now().minusDays(10).toString()
                )

                .when()
                .get("/internal/api/v1/kpi")

                .then()
                .statusCode(OK.value())
                .body("[0].date", equalTo(LocalDate.now().minusDays(10).toString()))
                .body("[0].nbAlertedUsers", nullValue())
                .body("[0].nbExposedButNotAtRiskUsers", nullValue())
                .body("[0].nbInfectedUsersNotNotified", nullValue())
                .body("[0].nbNotifiedUsersScoredAgain", nullValue())
                .body("[0].notifiedUsers", equalTo(0))
                .body("[0].usersAboveRiskThresholdButRetentionPeriodExpired", equalTo(0))
                .body("size()", equalTo(1));

    }

    @Test
    void can_fetch_statistics_for_multiple_days() {
        given()
                .params(
                        "fromDate", now().minusDays(3).toString(),
                        "toDate", now().minusDays(1).toString()
                )

                .when()
                .get("/internal/api/v1/kpi")

                .then()
                .statusCode(OK.value())
                .body("[0].date", equalTo(now().minusDays(3).toString()))
                .body("[0].nbAlertedUsers", equalTo(11))
                .body("[0].nbExposedButNotAtRiskUsers", equalTo(6))
                .body("[0].nbInfectedUsersNotNotified", equalTo(4))
                .body("[0].nbNotifiedUsersScoredAgain", equalTo(3))
                .body("[0].notifiedUsers", equalTo(1))
                .body("[0].usersAboveRiskThresholdButRetentionPeriodExpired", equalTo(4))
                .body("[1].date", equalTo(now().minusDays(2).toString()))
                .body("[1].nbAlertedUsers", equalTo(12))
                .body("[1].nbExposedButNotAtRiskUsers", equalTo(7))
                .body("[1].nbInfectedUsersNotNotified", equalTo(5))
                .body("[1].nbNotifiedUsersScoredAgain", equalTo(4))
                .body("[1].notifiedUsers", equalTo(2))
                .body("[1].usersAboveRiskThresholdButRetentionPeriodExpired", equalTo(6))
                .body("[2].date", equalTo(now().minusDays(1).toString()))
                .body("[2].nbAlertedUsers", equalTo(12))
                .body("[2].nbExposedButNotAtRiskUsers", equalTo(8))
                .body("[2].nbInfectedUsersNotNotified", equalTo(6))
                .body("[2].nbNotifiedUsersScoredAgain", equalTo(5))
                .body("[2].notifiedUsers", equalTo(0))
                .body("[2].usersAboveRiskThresholdButRetentionPeriodExpired", equalTo(8))
                .body("size()", equalTo(3));

    }

    @Test
    void can_compute_kpis_for_the_first_time() {

        when()
                .get("/internal/api/v1/tasks/compute-daily-kpis")

                .then()
                .statusCode(ACCEPTED.value())
                .body(is(emptyOrNullString()));

        await("kpis to be computed")
                .pollInterval(fibonacci())
                .atMost(2, SECONDS)
                .untilAsserted(
                        () -> assertThat(webserviceStatisticsRepository.findAll())
                                .extracting(
                                        "date",
                                        "totalAlertedUsers",
                                        "totalExposedButNotAtRiskUsers",
                                        "totalInfectedUsersNotNotified",
                                        "totalNotifiedUsersScoredAgain",
                                        "notifiedUsers"
                                )
                                .contains(
                                        tuple(now().atStartOfDay(UTC).minusDays(1).toInstant(), 2L, 0L, 1L, 1L, 0L)
                                )
                );
    }

    @Test
    void can_recompute_kpis_multiple_times() {
        // given some data has already being stored by @BeforeEach for today

        when()
                .get("/internal/api/v1/tasks/compute-daily-kpis")
                .then()
                .statusCode(ACCEPTED.value())
                .body(is(emptyOrNullString()));

        await("kpis to be recomputed")
                .pollInterval(fibonacci())
                .atMost(2, SECONDS)
                .untilAsserted(
                        () -> assertThat(webserviceStatisticsRepository.findAll())
                                .extracting(
                                        "date",
                                        "totalAlertedUsers",
                                        "totalExposedButNotAtRiskUsers",
                                        "totalInfectedUsersNotNotified",
                                        "totalNotifiedUsersScoredAgain",
                                        "notifiedUsers"
                                )
                                .contains(
                                        tuple(now().atStartOfDay(UTC).minusDays(1).toInstant(), 2L, 0L, 1L, 1L, 0L)
                                )
                );
    }

    @Test
    void compute_kpis_should_produce_info_logs_for_ops(final CapturedOutput output) {

        when()
                .get("/internal/api/v1/tasks/compute-daily-kpis")
                .then()
                .statusCode(ACCEPTED.value())
                .body(is(emptyOrNullString()));

        await("logs to be produced")
                .pollInterval(fibonacci())
                .atMost(2, SECONDS)
                .untilAsserted(
                        () -> assertThat(output.getOut())
                                .contains(
                                        "queuing a KPI compute task",
                                        "starting KPIs computation",
                                        "KPIs computation successful"
                                )
                );
    }

    @Test
    void compute_kpis_should_produce_error_logs_for_ops(final CapturedOutput output) {
        givenMongodbIsOffline();

        when()
                .get("/internal/api/v1/tasks/compute-daily-kpis")
                .then()
                .statusCode(ACCEPTED.value())
                .body(is(emptyOrNullString()));

        await("logs to be produced")
                .pollInterval(fibonacci())
                .atMost(5, SECONDS)
                .untilAsserted(
                        () -> assertThat(output.getOut())
                                .contains("unable to compute KPIs")
                );
    }
}
