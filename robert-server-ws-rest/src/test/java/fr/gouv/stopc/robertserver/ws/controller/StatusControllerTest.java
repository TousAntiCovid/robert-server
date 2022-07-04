package fr.gouv.stopc.robertserver.ws.controller;

import fr.gouv.stopc.robert.server.common.service.RobertClock;
import fr.gouv.stopc.robertserver.ws.test.AuthDataManager;
import fr.gouv.stopc.robertserver.ws.test.AuthDataManager.AuthRequestData;
import fr.gouv.stopc.robertserver.ws.test.IntegrationTest;
import fr.gouv.stopc.robertserver.ws.vo.PushInfoVo;
import fr.gouv.stopc.robertserver.ws.vo.StatusVo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;
import java.time.Instant;
import java.util.stream.Stream;

import static fr.gouv.stopc.robert.server.common.service.RobertClock.ROBERT_EPOCH;
import static fr.gouv.stopc.robertserver.ws.test.GrpcMockManager.givenCryptoServerRaiseError430ForEbid;
import static fr.gouv.stopc.robertserver.ws.test.JwtKeysManager.JWT_KEYS_ANALYTICS;
import static fr.gouv.stopc.robertserver.ws.test.JwtKeysManager.JWT_KEYS_DECLARATION;
import static fr.gouv.stopc.robertserver.ws.test.LogbackManager.assertThatInfoLogs;
import static fr.gouv.stopc.robertserver.ws.test.LogbackManager.assertThatWarnLogs;
import static fr.gouv.stopc.robertserver.ws.test.MockServerManager.verifyNoInteractionsWithPushNotifServer;
import static fr.gouv.stopc.robertserver.ws.test.MockServerManager.verifyPushNotifServerReceivedRegisterForToken;
import static fr.gouv.stopc.robertserver.ws.test.MongodbManager.*;
import static fr.gouv.stopc.robertserver.ws.test.StatisticsManager.assertThatTodayStatistic;
import static fr.gouv.stopc.robertserver.ws.test.matchers.Base64Matcher.isBase64Encoded;
import static fr.gouv.stopc.robertserver.ws.test.matchers.Base64Matcher.toBase64;
import static fr.gouv.stopc.robertserver.ws.test.matchers.DateTimeMatcher.isNtpTimestamp;
import static fr.gouv.stopc.robertserver.ws.test.matchers.DateTimeMatcher.isUnixTimestampNear;
import static fr.gouv.stopc.robertserver.ws.test.matchers.JwtMatcher.isJwtSignedBy;
import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static java.lang.String.format;
import static java.time.temporal.ChronoUnit.HOURS;
import static org.assertj.core.api.HamcrestCondition.matching;
import static org.assertj.core.data.Offset.offset;
import static org.hamcrest.Matchers.*;
import static org.springframework.http.HttpStatus.*;

@IntegrationTest
class StatusControllerTest {

    @Autowired
    private RobertClock clock;

    static Stream<Arguments> acceptable_epoch_and_time_drift() {
        return AuthDataManager.acceptableAuthParametersForEach(
                Stream.of(
                        null,
                        PushInfoVo.builder()
                                .token("valid-token")
                                .locale("fr-FR")
                                .timezone("Europe/Paris")
                                .build()
                )
        );
    }

    @ParameterizedTest
    @MethodSource("acceptable_epoch_and_time_drift")
    void can_request_exposure_status_no_risk(PushInfoVo pushInfo, AuthRequestData auth) {
        givenRegistrationExistsForUser("user___1");

        final var now = clock.now();

        given()
                .contentType(JSON)
                .body(
                        StatusVo.builder()
                                .ebid(toBase64("user___1"))
                                .epochId(auth.epochId())
                                .time(auth.base64Time32())
                                .mac(auth.base64Mac())
                                .pushInfo(pushInfo)
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
                                .withClaim("iat", isUnixTimestampNear(now.asInstant(), Duration.ofSeconds(60)))
                                .withClaim(
                                        "exp",
                                        isUnixTimestampNear(now.asInstant().plus(6, HOURS), Duration.ofSeconds(60))
                                )
                                .withClaim("iss", equalTo("robert-server"))
                );

        assertThatRegistrationForUser("user___1")
                .hasFieldOrPropertyWithValue("lastStatusRequestEpoch", now.asEpochId());

        assertThatRegistrationTimeDriftForUser("user___1")
                .isCloseTo(-auth.getTimeDrift().getSeconds(), offset(2L));

        if (null == pushInfo) {
            verifyNoInteractionsWithPushNotifServer();
        } else {
            verifyPushNotifServerReceivedRegisterForToken(pushInfo);
        }

        assertThatTodayStatistic("notifiedUsers")
                .isEqualTo(0);
    }

    @ParameterizedTest
    @MethodSource("acceptable_epoch_and_time_drift")
    void can_request_exposure_status_at_risk(PushInfoVo pushInfo, AuthRequestData auth) {
        givenRegistrationExistsForUser(
                "user___1", r -> r
                        .atRisk(true)
                        .lastContactTimestamp(888)
                        .latestRiskEpoch(999)
        );

        final var now = Instant.now();

        given()
                .contentType(JSON)
                .body(
                        StatusVo.builder()
                                .ebid(toBase64("user___1"))
                                .epochId(auth.epochId())
                                .time(auth.base64Time32())
                                .mac(auth.base64Mac())
                                .pushInfo(pushInfo)
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
                                .withClaim("iat", isUnixTimestampNear(now, Duration.ofSeconds(60)))
                                .withClaim("iss", equalTo("tac"))
                                .withClaim(
                                        "notificationDateTimestamp",
                                        isNtpTimestamp(clock.now().truncatedTo(ROBERT_EPOCH).asInstant())
                                )
                                .withClaim("lastContactDateTimestamp", equalTo(888))
                )
                .body(
                        "analyticsToken", isJwtSignedBy(JWT_KEYS_ANALYTICS)
                                .withClaim("iat", isUnixTimestampNear(now, Duration.ofSeconds(60)))
                                .withClaim(
                                        "exp", isUnixTimestampNear(now.plus(6, HOURS), Duration.ofSeconds(60))
                                )
                                .withClaim("iss", equalTo("robert-server"))
                );

        assertThatRegistrationForUser("user___1")
                .hasFieldOrPropertyWithValue("isNotified", true)
                .hasFieldOrPropertyWithValue("lastStatusRequestEpoch", clock.now().asEpochId());

        assertThatRegistrationTimeDriftForUser("user___1")
                .isCloseTo(-auth.getTimeDrift().getSeconds(), offset(2L));

        if (null == pushInfo) {
            verifyNoInteractionsWithPushNotifServer();
        } else {
            verifyPushNotifServerReceivedRegisterForToken(pushInfo);
        }
        assertThatTodayStatistic("notifiedUsers")
                .isEqualTo(1);
    }

    @ParameterizedTest
    @MethodSource("fr.gouv.stopc.robertserver.ws.test.AuthDataManager#unacceptableAuthParameters")
    void cant_request_exposure_status_with_too_much_time_drift(AuthRequestData auth) {
        givenRegistrationExistsForUser("user___1");

        given()
                .contentType(JSON)
                .body(
                        StatusVo.builder()
                                .ebid(toBase64("user___1"))
                                .epochId(auth.epochId())
                                .time(auth.base64Time32())
                                .mac(auth.base64Mac())
                                .build()
                )

                .when()
                .post("/api/v6/status")

                .then()
                .statusCode(BAD_REQUEST.value())
                .body(emptyString());

        verifyNoInteractionsWithPushNotifServer();

        assertThatRegistrationTimeDriftForUser("user___1")
                .isCloseTo(-auth.getTimeDrift().getSeconds(), offset(2L));

        assertThatInfoLogs()
                .containsOnlyOnce(
                        "Discarding authenticated request because provided time is too far from current server time"
                );
        assertThatWarnLogs()
                .areExactly(
                        1, matching(
                                matchesRegex(
                                        "Witnessing abnormal time difference -?\\d+ between client: \\d+ and server: \\d+"
                                )
                        )
                );
    }

    @ParameterizedTest
    @ValueSource(ints = { 1, 0 })
    void cant_make_too_close_exposure_status_requests(int lastStatusEpochSkew) {
        final var lastStatusRequestEpoch = clock.now().asEpochId() - lastStatusEpochSkew;
        givenRegistrationExistsForUser(
                "user___1",
                r -> r.lastStatusRequestEpoch(lastStatusRequestEpoch)
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
                .statusCode(BAD_REQUEST.value())
                .body(emptyString());

        assertThatInfoLogs()
                .containsOnlyOnce(
                        format(
                                "Discarding ESR request because epochs are too close: %d < 2 (tolerance)",
                                lastStatusEpochSkew
                        )
                );

        assertThatRegistrationForUser("user___1")
                .hasFieldOrPropertyWithValue("lastFailedStatusRequestEpoch", clock.now().asEpochId())
                .hasFieldOrPropertyWithValue(
                        "lastFailedStatusRequestMessage",
                        format(
                                "Discarding ESR request because epochs are too close: last ESR request epoch %d vs current epoch %d => %d < 2 (tolerance)",
                                lastStatusRequestEpoch, clock.now().asEpochId(), lastStatusEpochSkew
                        )
                );
    }

    @ParameterizedTest
    @MethodSource("acceptable_epoch_and_time_drift")
    void http_404_on_missing_registration(PushInfoVo pushInfo, AuthRequestData auth) {
        given()
                .contentType(JSON)
                .body(
                        StatusVo.builder()
                                .ebid(toBase64("user___1"))
                                .epochId(auth.epochId())
                                .time(auth.base64Time32())
                                .mac(auth.base64Mac())
                                .pushInfo(pushInfo)
                                .build()
                )

                .when()
                .post("/api/v6/status")

                .then()
                .statusCode(NOT_FOUND.value())
                .body(emptyString());

        assertThatInfoLogs()
                .containsOnlyOnce("Discarding status request because id unknown (fake or was deleted)");

        if (pushInfo != null) {
            verifyNoInteractionsWithPushNotifServer();
        }
    }

    @ParameterizedTest
    @MethodSource("acceptable_epoch_and_time_drift")
    void http_430_on_unknown_key(PushInfoVo pushInfo, AuthRequestData auth) {
        givenRegistrationExistsForUser("user___1");
        givenCryptoServerRaiseError430ForEbid("user___1");

        given()
                .contentType(JSON)
                .body(
                        StatusVo.builder()
                                .ebid(toBase64("user___1"))
                                .epochId(auth.epochId())
                                .time(auth.base64Time32())
                                .mac(auth.base64Mac())
                                .pushInfo(pushInfo)
                                .build()
                )

                .when()
                .post("/api/v6/status")

                .then()
                .statusCode(430)
                .body(emptyString());

        if (pushInfo != null) {
            verifyNoInteractionsWithPushNotifServer();
        }
    }

    @IntegrationTest
    @TestPropertySource(properties = "robert.esr.limit=0")
    public static class StatisticsTest {

        @Autowired
        private RobertClock clock;

        @Test
        void dont_increment_notifiedUsers_statistics_when_already_notified() {
            // given user___1 is "at risk"
            givenRegistrationExistsForUser(
                    "user___1", r -> r
                            .atRisk(true)
                            .lastContactTimestamp(888)
                            .latestRiskEpoch(999)
            );

            // and user___1 successfully requested ESR
            // so user___1 has been notified
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
                    .post("/api/v6/status");

            // when user___1 successfully requests another ESR
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
                    .statusCode(OK.value());

            // then statistics count 1 single new notified user
            assertThatTodayStatistic("notifiedUsers")
                    .isEqualTo(1);
        }
    }
}
