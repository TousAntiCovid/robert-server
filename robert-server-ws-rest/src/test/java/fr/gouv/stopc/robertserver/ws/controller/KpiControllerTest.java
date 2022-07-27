package fr.gouv.stopc.robertserver.ws.controller;

import fr.gouv.stopc.robertserver.database.model.*;
import fr.gouv.stopc.robertserver.database.repository.KpiRepository;
import fr.gouv.stopc.robertserver.ws.test.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.time.LocalDate;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.springframework.http.HttpStatus.OK;

@IntegrationTest
@ExtendWith(OutputCaptureExtension.class)
class KpiControllerTest {

    @Autowired
    private KpiRepository kpiRepository;

    @BeforeEach
    void initialize_webservice_statistics() {
        kpiRepository.deleteAll();
        kpiRepository.saveAll(
                List.of(
                        Kpi.builder().name("alertedUsers").value(10L).build(),
                        Kpi.builder().name("exposedButNotAtRiskUsers").value(5L).build(),
                        Kpi.builder().name("infectedUsersNotNotified").value(3L).build(),
                        Kpi.builder().name("notifiedUsersScoredAgain").value(2L).build(),
                        Kpi.builder().name("notifiedUsers").value(1L).build(),
                        Kpi.builder().name("reportsCount").value(7L).build(),
                        Kpi.builder().name("usersAboveRiskThresholdButRetentionPeriodExpired").value(90L).build()
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

}
