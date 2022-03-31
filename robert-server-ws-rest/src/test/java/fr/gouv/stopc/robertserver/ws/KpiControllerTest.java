package fr.gouv.stopc.robertserver.ws;

import fr.gouv.stopc.robertserver.database.model.WebserviceStatistic;
import fr.gouv.stopc.robertserver.database.repository.WebserviceStatisticsRepository;
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
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.http.HttpStatus.OK;

@IntegrationTest
class KpiControllerTest {

    @Autowired
    private WebserviceStatisticsRepository repository;

    @BeforeEach
    void beforeEach() {
        repository.deleteAll();
    }

    @Test
    void should_retrieve_a_statistic_with_just_1_notification() {

        final var statistic = WebserviceStatistic.builder()
                .date(Instant.now().truncatedTo(DAYS))
                .notifiedTotal(1)
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
                .body("[0].nbNotifiedTotal", equalTo(1));

    }

    @Test
    void should_retrieve_a_statistic_with_all_0_values() {

        final var statistic = WebserviceStatistic.builder()
                .date(Instant.now().minus(2, DAYS))
                .notifiedTotal(1)
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
                .body("[0].nbNotifiedTotal", equalTo(0));

    }

    @Test
    void should_aggregate_yesterday_and_today_statistic_with_3_total_notifications() {

        final var oneStatistic = WebserviceStatistic.builder()
                .date(Instant.now().minus(1, DAYS))
                .notifiedTotal(1)
                .build();

        final var anOtherStatistic = WebserviceStatistic.builder()
                .date(Instant.now())
                .notifiedTotal(2)
                .build();

        final var aThirdStatistic = WebserviceStatistic.builder()
                .date(Instant.now().plus(1, DAYS))
                .notifiedTotal(5)
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
                .body("[0].nbNotifiedTotal", equalTo(3));

    }

}
