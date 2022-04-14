package fr.gouv.stopc.robertserver.ws.controller;

import fr.gouv.stopc.robertserver.ws.test.IntegrationTest;
import fr.gouv.stopc.robertserver.ws.test.MongodbManager;
import fr.gouv.stopc.robertserver.ws.vo.ContactVo;
import fr.gouv.stopc.robertserver.ws.vo.HelloMessageDetailVo;
import fr.gouv.stopc.robertserver.ws.vo.ReportBatchRequestVo;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;

import static fr.gouv.stopc.robertserver.ws.test.JwtKeysManager.JWT_KEYS;
import static fr.gouv.stopc.robertserver.ws.test.MongodbManager.assertThatContactsToProcess;
import static fr.gouv.stopc.robertserver.ws.test.matchers.Base64Matcher.toBase64;
import static fr.gouv.stopc.robertserver.ws.test.matchers.JwtMatcher.isJwtSignedBy;
import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.assertj.core.api.Assertions.tuple;
import static org.hamcrest.Matchers.*;
import static org.springframework.http.HttpStatus.OK;

@IntegrationTest
class ReportControllerSuccessTest {

    @Test
    void can_report() {
        given()
                .contentType(JSON)
                .body(
                        ReportBatchRequestVo.builder()
                                .token("validZ")
                                .contacts(
                                        List.of(
                                                ContactVo.builder()
                                                        .ebid(toBase64("fake ebid A"))
                                                        .ecc(toBase64("fake ecc"))
                                                        .ids(
                                                                List.of(
                                                                        HelloMessageDetailVo.builder()
                                                                                .rssiCalibrated(0)
                                                                                .timeCollectedOnDevice(1L)
                                                                                .timeFromHelloMessage(2)
                                                                                .mac(toBase64("fake mac A1"))
                                                                                .build()
                                                                )
                                                        )
                                                        .build(),
                                                ContactVo.builder()
                                                        .ebid(toBase64("fake ebid B"))
                                                        .ecc(toBase64("fake ecc"))
                                                        .ids(
                                                                List.of(
                                                                        HelloMessageDetailVo.builder()
                                                                                .rssiCalibrated(10)
                                                                                .timeCollectedOnDevice(11L)
                                                                                .timeFromHelloMessage(12)
                                                                                .mac(toBase64("fake mac B1"))
                                                                                .build(),
                                                                        HelloMessageDetailVo.builder()
                                                                                .rssiCalibrated(20)
                                                                                .timeCollectedOnDevice(21L)
                                                                                .timeFromHelloMessage(22)
                                                                                .mac(toBase64("fake mac B2"))
                                                                                .build()
                                                                )
                                                        )
                                                        .build()
                                        )
                                )
                                .build()
                )

                .when()
                .post("/api/v6/report")

                .then()
                .statusCode(OK.value())
                .body("success", equalTo(true))
                .body("message", equalTo("Successful operation"))
                .body(
                        "reportValidationToken", isJwtSignedBy(JWT_KEYS)
                                .withClaim(
                                        "iat", allOf(
                                                greaterThan((int) Instant.now().minusSeconds(60).getEpochSecond()),
                                                lessThanOrEqualTo((int) Instant.now().getEpochSecond())
                                        )
                                )
                                .withClaim(
                                        "exp", allOf(
                                                greaterThan(
                                                        (int) ZonedDateTime.now().plusMinutes(4).toInstant()
                                                                .getEpochSecond()
                                                ),
                                                lessThan(
                                                        (int) ZonedDateTime.now().plusMinutes(6).toInstant()
                                                                .getEpochSecond()
                                                )
                                        )
                                )
                );

        assertThatContactsToProcess()
                .flatExtracting(MongodbManager::helloMessages)
                .extracting("ebid", "ecc", "time", "mac", "rssi", "receptionTime")
                .containsExactlyInAnyOrder(
                        tuple("fake ebid A", "fake ecc", 2, "fake mac A1", 0, 1L),
                        tuple("fake ebid B", "fake ecc", 12, "fake mac B1", 10, 11L),
                        tuple("fake ebid B", "fake ecc", 22, "fake mac B2", 20, 21L)
                );
    }
}
