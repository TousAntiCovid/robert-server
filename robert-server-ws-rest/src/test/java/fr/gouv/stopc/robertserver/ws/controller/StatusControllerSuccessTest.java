package fr.gouv.stopc.robertserver.ws.controller;

import fr.gouv.stopc.robert.server.common.service.RobertClock;
import fr.gouv.stopc.robertserver.ws.test.IntegrationTest;
import fr.gouv.stopc.robertserver.ws.vo.StatusVo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.time.Instant;

import static fr.gouv.stopc.robert.server.common.service.RobertClock.EPOCH;
import static fr.gouv.stopc.robertserver.ws.test.JwtKeysManager.JWT_KEYS_ANALYTICS;
import static fr.gouv.stopc.robertserver.ws.test.JwtKeysManager.JWT_KEYS_DECLARATION;
import static fr.gouv.stopc.robertserver.ws.test.MockServerManager.verifyNoInteractionsWithPushNotifServer;
import static fr.gouv.stopc.robertserver.ws.test.MongodbManager.assertThatRegistrationForUser;
import static fr.gouv.stopc.robertserver.ws.test.MongodbManager.givenRegistrationExistsForUser;
import static fr.gouv.stopc.robertserver.ws.test.matchers.Base64Matcher.isBase64Encoded;
import static fr.gouv.stopc.robertserver.ws.test.matchers.Base64Matcher.toBase64;
import static fr.gouv.stopc.robertserver.ws.test.matchers.DateTimeMatcher.*;
import static fr.gouv.stopc.robertserver.ws.test.matchers.JwtMatcher.isJwtSignedBy;
import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static java.time.temporal.ChronoUnit.HOURS;
import static org.hamcrest.Matchers.*;
import static org.springframework.http.HttpStatus.OK;

@IntegrationTest
class StatusControllerSuccessTest {

    @Autowired
    private RobertClock clock;

    @Test
    void can_request_no_risk_status() {
        givenRegistrationExistsForUser("user___1");

        given()
                .contentType(JSON)
                .body(
                        StatusVo.builder()
                                .ebid(toBase64("user___1"))
                                .epochId(clock.now().asEpochId())
                                .time(toBase64(clock.now().asTime32()))
                                .mac(toBase64("fake mac having a length of exactly 44 characters", 32))
                                .build()
                )

                .when()
                .post("/api/v6/status")

                .then()
                .statusCode(OK.value())
                .body("riskLevel", equalTo(0))
                .body("tuples", isBase64Encoded(equalTo("fake encrypted tuples for user___1")))
                .body("config.size()", equalTo(0))
                .body("lastContactDate", nullValue())
                .body("lastRiskScoringDate", nullValue())
                .body("declarationToken", nullValue())
                .body(
                        "analyticsToken", isJwtSignedBy(JWT_KEYS_ANALYTICS)
                                .withClaim("iat", isUnixTimestampNear(Instant.now(), Duration.ofSeconds(60)))
                                .withClaim(
                                        "exp", isUnixTimestampNear(Instant.now().plus(6, HOURS), Duration.ofSeconds(60))
                                )
                                .withClaim("iss", equalTo("robert-server"))
                );

        assertThatRegistrationForUser("user___1")
                .hasFieldOrPropertyWithValue("lastStatusRequestEpoch", clock.now().asEpochId());

        verifyNoInteractionsWithPushNotifServer();
    }

    @Test
    void can_request_at_risk_status() {
        givenRegistrationExistsForUser(
                "user___1", r -> r
                        .atRisk(true)
                        .lastContactTimestamp(888)
                        .latestRiskEpoch(999)
        );

        given()
                .contentType(JSON)
                .body(
                        StatusVo.builder()
                                .ebid(toBase64("user___1"))
                                .epochId(clock.now().asEpochId())
                                .time(toBase64(clock.now().asTime32()))
                                .mac(toBase64("fake mac having a length of exactly 44 characters", 32))
                                .build()
                )

                .when()
                .post("/api/v6/status")

                .then()
                .statusCode(OK.value())
                .body("riskLevel", equalTo(4))
                .body("tuples", isBase64Encoded(equalTo("fake encrypted tuples for user___1")))
                .body("config.size()", equalTo(0))
                .body("lastContactDate", equalTo("888"))
                .body("lastRiskScoringDate", isNtpTimestamp(clock.atEpoch(999).asInstant()))
                .body(
                        "declarationToken", isJwtSignedBy(JWT_KEYS_DECLARATION)
                                .withClaim("jti", matchesRegex("\\w{64}"))
                                .withClaim("iat", isUnixTimestampNear(Instant.now(), Duration.ofSeconds(60)))
                                .withClaim("iss", equalTo("tac"))
                                .withClaim(
                                        "notificationDateTimestamp",
                                        isNtpTimestamp(clock.now().truncatedTo(EPOCH).asInstant())
                                )
                                .withClaim("lastContactDateTimestamp", equalTo(888))
                )
                .body(
                        "analyticsToken", isJwtSignedBy(JWT_KEYS_ANALYTICS)
                                .withClaim("iat", isUnixTimestampNear(Instant.now(), Duration.ofSeconds(60)))
                                .withClaim(
                                        "exp", isUnixTimestampNear(Instant.now().plus(6, HOURS), Duration.ofSeconds(60))
                                )
                                .withClaim("iss", equalTo("robert-server"))
                );

        assertThatRegistrationForUser("user___1")
                .hasFieldOrPropertyWithValue("isNotified", true)
                .hasFieldOrPropertyWithValue("lastStatusRequestEpoch", clock.now().asEpochId());

        verifyNoInteractionsWithPushNotifServer();
    }
}
