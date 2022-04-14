package fr.gouv.stopc.robertserver.ws;

import fr.gouv.stopc.robertserver.database.model.BatchStatistics;
import fr.gouv.stopc.robertserver.database.model.WebserviceStatistics;
import fr.gouv.stopc.robertserver.database.repository.BatchStatisticsRepository;
import fr.gouv.stopc.robertserver.database.repository.WebserviceStatisticsRepository;
import fr.gouv.stopc.robertserver.ws.test.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.List;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
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
class KpiControllerTest {

    @Autowired
    private BatchStatisticsRepository batchStatisticsRepository;

    @Autowired
    private WebserviceStatisticsRepository webserviceStatisticsRepository;

    @BeforeEach
    void beforeEach() {
        batchStatisticsRepository.deleteAll();
        batchStatisticsRepository.saveAll(
                List.of(
                        BatchStatistics.builder()
                                .batchExecution(LocalDate.now().minusDays(4).atStartOfDay().toInstant(UTC))
                                .usersAboveRiskThresholdButRetentionPeriodExpired(1L)
                                .build(),
                        BatchStatistics.builder()
                                .batchExecution(LocalDate.now().minusDays(3).atStartOfDay().toInstant(UTC))
                                .usersAboveRiskThresholdButRetentionPeriodExpired(2L)
                                .build(),
                        BatchStatistics.builder()
                                .batchExecution(LocalDate.now().minusDays(2).atStartOfDay().toInstant(UTC))
                                .usersAboveRiskThresholdButRetentionPeriodExpired(3L)
                                .build(),
                        BatchStatistics.builder()
                                .batchExecution(LocalDate.now().minusDays(1).atStartOfDay().toInstant(UTC))
                                .usersAboveRiskThresholdButRetentionPeriodExpired(4L)
                                .build(),
                        BatchStatistics.builder()
                                .batchExecution(LocalDate.now().atStartOfDay().toInstant(UTC))
                                .usersAboveRiskThresholdButRetentionPeriodExpired(90L)
                                .build()
                )
        );
        webserviceStatisticsRepository.deleteAll();
        webserviceStatisticsRepository.saveAll(
                List.of(
                        WebserviceStatistics.builder()
                                .date(LocalDate.now().minusDays(4).atStartOfDay().toInstant(UTC))
                                .totalAlertedUsers(10L)
                                .totalExposedButNotAtRiskUsers(5L)
                                .totalInfectedUsersNotNotified(3L)
                                .totalNotifiedUsersScoredAgain(2L)
                                .notifiedUsers(1L)
                                .build(),
                        WebserviceStatistics.builder()
                                .date(LocalDate.now().minusDays(3).atStartOfDay().toInstant(UTC))
                                .totalAlertedUsers(11L)
                                .totalExposedButNotAtRiskUsers(6L)
                                .totalInfectedUsersNotNotified(4L)
                                .totalNotifiedUsersScoredAgain(3L)
                                .notifiedUsers(1L)
                                .build(),
                        WebserviceStatistics.builder()
                                .date(LocalDate.now().minusDays(2).atStartOfDay().toInstant(UTC))
                                .totalAlertedUsers(12L)
                                .totalExposedButNotAtRiskUsers(7L)
                                .totalInfectedUsersNotNotified(5L)
                                .totalNotifiedUsersScoredAgain(4L)
                                .notifiedUsers(2L)
                                .build(),
                        WebserviceStatistics.builder()
                                .date(LocalDate.now().minusDays(1).atStartOfDay().toInstant(UTC))
                                .totalAlertedUsers(12L)
                                .totalExposedButNotAtRiskUsers(8L)
                                .totalInfectedUsersNotNotified(6L)
                                .totalNotifiedUsersScoredAgain(5L)
                                .notifiedUsers(0L)
                                .build(),
                        WebserviceStatistics.builder()
                                .date(LocalDate.now().atStartOfDay().toInstant(UTC))
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
                        "fromDate", LocalDate.now().minusDays(1).toString(),
                        "toDate", LocalDate.now().minusDays(1).toString()
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
                .body("[0].nbNotifiedUsers", equalTo(0))
                .body("[0].usersAboveRiskThresholdButRetentionPeriodExpired", equalTo(4))
                .body("size()", equalTo(1));

    }

    @Test
    void can_fetch_statistics_for_multiple_days() {
        given()
                .params(
                        "fromDate", LocalDate.now().minusDays(3).toString(),
                        "toDate", LocalDate.now().minusDays(1).toString()
                )

                .when()
                .get("/internal/api/v1/kpi")

                .then()
                .statusCode(OK.value())
                .body("[0].date", equalTo(LocalDate.now().minusDays(3).toString()))
                .body("[0].nbAlertedUsers", equalTo(11))
                .body("[0].nbExposedButNotAtRiskUsers", equalTo(6))
                .body("[0].nbInfectedUsersNotNotified", equalTo(4))
                .body("[0].nbNotifiedUsersScoredAgain", equalTo(3))
                .body("[0].nbNotifiedUsers", equalTo(1))
                .body("[0].usersAboveRiskThresholdButRetentionPeriodExpired", equalTo(2))
                .body("[1].date", equalTo(LocalDate.now().minusDays(2).toString()))
                .body("[1].nbAlertedUsers", equalTo(12))
                .body("[1].nbExposedButNotAtRiskUsers", equalTo(7))
                .body("[1].nbInfectedUsersNotNotified", equalTo(5))
                .body("[1].nbNotifiedUsersScoredAgain", equalTo(4))
                .body("[1].nbNotifiedUsers", equalTo(2))
                .body("[1].usersAboveRiskThresholdButRetentionPeriodExpired", equalTo(3))
                .body("[2].date", equalTo(LocalDate.now().minusDays(1).toString()))
                .body("[2].nbAlertedUsers", equalTo(12))
                .body("[2].nbExposedButNotAtRiskUsers", equalTo(8))
                .body("[2].nbInfectedUsersNotNotified", equalTo(6))
                .body("[2].nbNotifiedUsersScoredAgain", equalTo(5))
                .body("[2].nbNotifiedUsers", equalTo(0))
                .body("[2].usersAboveRiskThresholdButRetentionPeriodExpired", equalTo(4))
                .body("size()", equalTo(3));

    }

    @Test
    void can_compute_kpis_for_the_first_time() {
        webserviceStatisticsRepository.deleteAll();

        when()
                .get("/internal/api/v1/tasks/compute-daily-kpis")
                .then()
                .statusCode(ACCEPTED.value())
                .body(is(emptyOrNullString()));

        await("kpis to be computed")
                .pollInterval(fibonacci())
                .atMost(5, SECONDS)
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
                                .containsExactly(
                                        tuple(LocalDate.now().atStartOfDay(UTC).toInstant(), 0L, 0L, 0L, 0L, 0L)
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
                .atMost(5, SECONDS)
                .untilAsserted(
                        () -> assertThat(
                                webserviceStatisticsRepository.findById(LocalDate.now().atStartOfDay().toInstant(UTC))
                        )
                                .contains(
                                        WebserviceStatistics.builder()
                                                .date(LocalDate.now().atStartOfDay().toInstant(UTC))
                                                .totalAlertedUsers(0L)
                                                .totalExposedButNotAtRiskUsers(0L)
                                                .totalInfectedUsersNotNotified(0L)
                                                .totalNotifiedUsersScoredAgain(0L)
                                                .notifiedUsers(95L)
                                                .build()
                                )
                );
    }

}
