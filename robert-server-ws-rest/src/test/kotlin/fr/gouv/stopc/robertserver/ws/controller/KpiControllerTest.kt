package fr.gouv.stopc.robertserver.ws.controller

import fr.gouv.stopc.robertserver.test.MongodbManager.Companion.givenKpiExists
import fr.gouv.stopc.robertserver.ws.test.IntegrationTest
import fr.gouv.stopc.robertserver.ws.test.When
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus.OK

@IntegrationTest
class KpiControllerTest {

    @BeforeEach
    fun initialize_kpis() {
        givenKpiExists("alertedUsers", 10L)
        givenKpiExists("exposedButNotAtRiskUsers", 5L)
        givenKpiExists("infectedUsersNotNotified", 3L)
        givenKpiExists("notifiedUsersScoredAgain", 2L)
        givenKpiExists("notifiedUsers", 1L)
        givenKpiExists("reportsCount", 7L)
        givenKpiExists("usersAboveRiskThresholdButRetentionPeriodExpired", 90L)
    }

    @Test
    fun can_fetch_kpis() {
        When()
            .get("/internal/api/v2/kpis")
            .then()
            .statusCode(OK.value())
            .body("alertedUsers", equalTo(10))
            .body("exposedButNotAtRiskUsers", equalTo(5))
            .body("infectedUsersNotNotified", equalTo(3))
            .body("notifiedUsersScoredAgain", equalTo(2))
            .body("notifiedUsers", equalTo(1))
            .body("usersAboveRiskThresholdButRetentionPeriodExpired", equalTo(90))
            .body("reportsCount", equalTo(7))
            .body("size()", equalTo(7))
    }
}
