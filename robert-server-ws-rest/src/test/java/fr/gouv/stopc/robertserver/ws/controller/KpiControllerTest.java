package fr.gouv.stopc.robertserver.ws.controller;

import fr.gouv.stopc.robertserver.database.model.*;
import fr.gouv.stopc.robertserver.database.repository.BatchStatisticsRepository;
import fr.gouv.stopc.robertserver.database.repository.RegistrationRepository;
import fr.gouv.stopc.robertserver.database.repository.WebserviceKpiRepository;
import fr.gouv.stopc.robertserver.ws.test.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.time.LocalDate;
import java.util.List;

import static io.restassured.RestAssured.given;
import static java.time.LocalDate.now;
import static java.time.ZoneOffset.UTC;
import static org.hamcrest.Matchers.*;
import static org.springframework.http.HttpStatus.OK;

@IntegrationTest
@ExtendWith(OutputCaptureExtension.class)
class KpiControllerTest {

    @Autowired
    private RegistrationRepository registrationRepository;

    @Autowired
    private BatchStatisticsRepository batchStatisticsRepository;

    @Autowired
    private WebserviceKpiRepository webserviceKpiRepository;

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
        webserviceKpiRepository.deleteAll();
        webserviceKpiRepository.saveAll(
                List.of(
                        WebserviceKpi.builder().name("alertedUsers").value(10L).build(),
                        WebserviceKpi.builder().name("exposedButNotAtRiskUsers").value(5L).build(),
                        WebserviceKpi.builder().name("infectedUsersNotNotified").value(3L).build(),
                        WebserviceKpi.builder().name("notifiedUsersScoredAgain").value(2L).build(),
                        WebserviceKpi.builder().name("notifiedUsers").value(1L).build(),
                        WebserviceKpi.builder().name("reportsCount").value(7L).build()
                )
        );
    }

    @Test
    void can_fetch_kpis() {
        given()
                .when()
                .get("/internal/api/v2/kpis")
                .then()
                .statusCode(OK.value())
                .body("date", equalTo(LocalDate.now().toString()))
                .body("alertedUsers", equalTo(10))
                .body("exposedButNotAtRiskUsers", equalTo(5))
                .body("infectedUsersNotNotified", equalTo(3))
                .body("notifiedUsersScoredAgain", equalTo(2))
                .body("notifiedUsers", equalTo(1))
                .body("usersAboveRiskThresholdButRetentionPeriodExpired", equalTo(90))
                .body("reportsCount", equalTo(7))
                .body("size()", equalTo(8));

    }

    @Test
    void can_fetch_kpis_no_ws_stats() {

        webserviceKpiRepository.deleteAll();

        given()
                .when()
                .get("/internal/api/v2/kpis")
                .then()
                .statusCode(OK.value())
                .body("date", equalTo(LocalDate.now().toString()))
                .body("alertedUsers", equalTo(0))
                .body("exposedButNotAtRiskUsers", equalTo(0))
                .body("infectedUsersNotNotified", equalTo(0))
                .body("notifiedUsersScoredAgain", equalTo(0))
                .body("notifiedUsers", equalTo(0))
                .body("usersAboveRiskThresholdButRetentionPeriodExpired", equalTo(90))
                .body("reportsCount", equalTo(0))
                .body("size()", equalTo(8));

    }

    @Test
    void can_fetch_kpis_no_batch_stats() {

        batchStatisticsRepository.deleteAll();

        given()
                .when()
                .get("/internal/api/v2/kpis")
                .then()
                .statusCode(OK.value())
                .body("date", equalTo(LocalDate.now().toString()))
                .body("alertedUsers", equalTo(10))
                .body("exposedButNotAtRiskUsers", equalTo(5))
                .body("infectedUsersNotNotified", equalTo(3))
                .body("notifiedUsersScoredAgain", equalTo(2))
                .body("notifiedUsers", equalTo(1))
                .body("usersAboveRiskThresholdButRetentionPeriodExpired", equalTo(0))
                .body("reportsCount", equalTo(7))
                .body("size()", equalTo(8));

    }
}
