package fr.gouv.stopc.robertserver.ws;

import fr.gouv.stopc.robertserver.database.model.BatchStatistics;
import fr.gouv.stopc.robertserver.database.repository.BatchStatisticsRepository;
import fr.gouv.stopc.robertserver.ws.config.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.HOURS;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.http.HttpStatus.OK;

@IntegrationTest
class KpiControllerTest {

    @Autowired
    private BatchStatisticsRepository repository;

    @BeforeEach
    void beforeEach() {
        repository.deleteAll();
    }

    @Test
    void should_retrieve_a_batch_statistic_with_just_1_user() {

        final var statistic = BatchStatistics.builder()
                .batchExecution(Instant.now().truncatedTo(DAYS))
                .nbUsersAboveThresholdButNotAtRisk(1)
                .build();

        repository.save(statistic);

        given()
                .contentType(JSON)
                .params(
                        "fromDate", LocalDate.now(ZoneId.systemDefault()).minus(1, DAYS).toString(),
                        "toDate", LocalDate.now(ZoneId.systemDefault()).plus(1, DAYS).toString()
                )
                .expect()
                .statusCode(OK.value())
                .when()
                .get("/internal/api/v1/kpi")
                .then()
                .body("[0].date", equalTo(LocalDate.now(ZoneId.systemDefault()).toString()))
                .body("[0].nbUsersAboveThresholdButNotAtRisk", equalTo(1))
                .body("size()", equalTo(1));

    }

    @Test
    void should_retrieve_a_batch_statistic_with_all_0_values() {

        final var statistic = BatchStatistics.builder()
                .batchExecution(Instant.now().truncatedTo(DAYS).minus(2, DAYS))
                .nbUsersAboveThresholdButNotAtRisk(1)
                .build();

        repository.save(statistic);

        given()
                .contentType(JSON)
                .params(
                        "fromDate", LocalDate.now(ZoneId.systemDefault()).minus(1, DAYS).toString(),
                        "toDate", LocalDate.now(ZoneId.systemDefault()).plus(1, DAYS).toString()
                )
                .expect()
                .statusCode(OK.value())
                .when()
                .get("/internal/api/v1/kpi")
                .then()
                .body("[0].date", equalTo(LocalDate.now(ZoneId.systemDefault()).toString()))
                .body("[0].nbUsersAboveThresholdButNotAtRisk", equalTo(0))
                .body("size()", equalTo(1));

    }

    @Test
    void should_aggregate_all_today_batch_statistic_with_8_total_users() {

        final var oneStatistic = BatchStatistics.builder()
                .batchExecution(Instant.now().truncatedTo(DAYS))
                .nbUsersAboveThresholdButNotAtRisk(1)
                .build();

        final var anOtherStatistic = BatchStatistics.builder()
                .batchExecution(Instant.now().truncatedTo(DAYS).minus(2, HOURS))
                .nbUsersAboveThresholdButNotAtRisk(2)
                .build();

        final var aThirdStatistic = BatchStatistics.builder()
                .batchExecution(Instant.now().truncatedTo(DAYS).plus(2, HOURS))
                .nbUsersAboveThresholdButNotAtRisk(5)
                .build();

        repository.saveAll(
                List.of(
                        oneStatistic,
                        anOtherStatistic,
                        aThirdStatistic
                )
        );

        given()
                .contentType(JSON)
                .params(
                        "fromDate", LocalDate.now(ZoneId.systemDefault()).minus(1, DAYS).toString(),
                        "toDate", LocalDate.now(ZoneId.systemDefault()).plus(1, DAYS).toString()
                )
                .expect()
                .statusCode(OK.value())
                .when()
                .get("/internal/api/v1/kpi")
                .then()
                .body("[0].date", equalTo(LocalDate.now(ZoneId.systemDefault()).toString()))
                .body("[0].nbUsersAboveThresholdButNotAtRisk", equalTo(8))
                .body("size()", equalTo(1));

    }
}
