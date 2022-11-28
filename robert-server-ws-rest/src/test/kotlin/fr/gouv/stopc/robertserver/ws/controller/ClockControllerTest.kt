package fr.gouv.stopc.robertserver.ws.controller

import fr.gouv.stopc.robertserver.common.RobertClock.ROBERT_EPOCH
import fr.gouv.stopc.robertserver.test.matchers.isoDateTimeNear
import fr.gouv.stopc.robertserver.ws.test.IntegrationTest
import io.restassured.RestAssured
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus.OK
import java.time.Instant
import java.time.temporal.ChronoUnit.SECONDS
import kotlin.time.Duration.Companion.seconds

@IntegrationTest
class ClockControllerTest {
    @Test
    fun clock_endpoint_returns_service_start_time_current_time_and_expected_epoch_number() {
        val secondsSinceServiceStartTime =
            Instant.parse("2020-06-01T00:00:00Z").until(Instant.now(), SECONDS)
        RestAssured.`when`()["/api/v6/clock"]
            .then()
            .statusCode(OK.value())
            .body("serviceStartDate", equalTo("2020-06-01"))
            .body("time", isoDateTimeNear(Instant.now(), 5.seconds))
            .body("epoch", equalTo((secondsSinceServiceStartTime / ROBERT_EPOCH.duration.seconds).toInt()))
    }
}
