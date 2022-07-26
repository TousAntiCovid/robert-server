package fr.gouv.stopc.robertserver.ws.controller;

import fr.gouv.stopc.robertserver.ws.test.IntegrationTest;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static fr.gouv.stopc.robert.server.common.utils.TimeUtils.EPOCH_DURATION_SECS;
import static fr.gouv.stopc.robertserver.ws.test.matchers.DateTimeMatcher.isoDateTimeNear;
import static io.restassured.RestAssured.when;
import static java.time.Instant.now;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.http.HttpStatus.OK;

@IntegrationTest
public class ClockControllerTest {

    @Test
    public void clock_endpoint_returns_service_start_time_current_time_and_expected_epoch_number() {

        final var secondsSinceServiceStartTime = (int) Instant.parse("2020-06-01T00:00:00Z").until(now(), SECONDS);

        when()
                .get("/api/v6/clock")
                .then()
                .statusCode(OK.value())
                .body("serviceStartDate", equalTo("2020-06-01"))
                .body("time", isoDateTimeNear(Instant.now(), Duration.ofSeconds(5)))
                .body("epoch", equalTo(secondsSinceServiceStartTime / EPOCH_DURATION_SECS));
    }
}
