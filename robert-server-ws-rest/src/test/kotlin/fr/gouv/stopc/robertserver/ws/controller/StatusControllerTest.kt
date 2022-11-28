package fr.gouv.stopc.robertserver.ws.controller

import fr.gouv.stopc.robertserver.common.RobertClock
import fr.gouv.stopc.robertserver.common.RobertClock.ROBERT_EPOCH
import fr.gouv.stopc.robertserver.common.base64Encode
import fr.gouv.stopc.robertserver.test.LogbackManager.Companion.assertThatInfoLogs
import fr.gouv.stopc.robertserver.test.MongodbManager.Companion.assertThatRegistrationForIdA
import fr.gouv.stopc.robertserver.test.MongodbManager.Companion.givenRegistrationExistsForIdA
import fr.gouv.stopc.robertserver.test.matchers.isBase64Encoded
import fr.gouv.stopc.robertserver.test.matchers.isJwtSignedBy
import fr.gouv.stopc.robertserver.test.matchers.isNtpTimestamp
import fr.gouv.stopc.robertserver.test.matchers.isUnixTimestamp
import fr.gouv.stopc.robertserver.ws.test.AuthDataManager.AuthRequestData
import fr.gouv.stopc.robertserver.ws.test.AuthDataManager.Companion.acceptableAuthParametersForEach
import fr.gouv.stopc.robertserver.ws.test.GrpcMockManager.Companion.givenCryptoServerRaiseMissingDailyKeyForEbid
import fr.gouv.stopc.robertserver.ws.test.IntegrationTest
import fr.gouv.stopc.robertserver.ws.test.JwtKeysManager.JWT_KEYS_ANALYTICS
import fr.gouv.stopc.robertserver.ws.test.JwtKeysManager.JWT_KEYS_DECLARATION
import fr.gouv.stopc.robertserver.ws.test.MockServerManager.Companion.verifyNoInteractionsWithPushNotifServer
import fr.gouv.stopc.robertserver.ws.test.MockServerManager.Companion.verifyPushNotifServerReceivedRegisterForToken
import fr.gouv.stopc.robertserver.ws.test.StatisticsManager.Companion.assertThatTodayStatistic
import fr.gouv.stopc.robertserver.ws.test.When
import fr.gouv.stopc.robertserver.ws.test.matchers.timeDriftCloseTo
import fr.gouv.stopc.robertserver.ws.test.matchers.verifyExceededTimeDriftIsProperlyHandled
import io.restassured.RestAssured.given
import io.restassured.http.ContentType.JSON
import org.hamcrest.Matchers.anyOf
import org.hamcrest.Matchers.emptyString
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.matchesRegex
import org.hamcrest.Matchers.nullValue
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.HttpStatus.OK
import org.springframework.test.context.TestPropertySource
import java.time.temporal.ChronoUnit.HOURS
import kotlin.time.Duration.Companion.seconds

@IntegrationTest
internal class StatusControllerTest(@Autowired private val clock: RobertClock) {

    companion object {

        @JvmStatic
        fun cant_make_too_close_exposure_status_requests(): List<Arguments> {
            return acceptableAuthParametersForEach(
                listOf(1, 0)
            )
        }
    }

    @ParameterizedTest
    @MethodSource("fr.gouv.stopc.robertserver.ws.test.AuthDataManager#acceptableAuthParameters")
    fun can_request_exposure_status_no_risk(auth: AuthRequestData) {
        givenRegistrationExistsForIdA("idA_1")

        val now = clock.now()
        given()
            .contentType(JSON)
            .body(
                """
                    {
                      "ebid":    "${"idA_1eb1".base64Encode()}",
                      "epochId": "${auth.epochId()}",
                      "time":    "${auth.base64Time32()}",
                      "mac":     "${auth.base64Mac()}",
                      "pushInfo": ${auth.pushInfo}
                    }
                """.trimIndent()
            )
            .When()
            .post("/api/v6/status")
            .then()
            .statusCode(OK.value())
            .body("riskLevel", equalTo(0))
            .body("tuples", isBase64Encoded(equalTo("fake encrypted tuples for 'idA_1'")))
            .body("config.size()", equalTo(0))
            .body("lastContactDate", nullValue())
            .body("lastRiskScoringDate", nullValue())
            .body("declarationToken", nullValue())
            .body(
                "analyticsToken",
                isJwtSignedBy(JWT_KEYS_ANALYTICS)
                    .withClaim("iat", isUnixTimestamp(now.asInstant(), offset = 60.seconds))
                    .withClaim(
                        "exp",
                        isUnixTimestamp(
                            now.asInstant().plus(6, HOURS),
                            offset = 60.seconds
                        )
                    )
                    .withClaim("iss", equalTo("robert-server"))
            )
        assertThatRegistrationForIdA("idA_1")
            .hasFieldOrPropertyWithValue("lastStatusRequestEpoch", now.asEpochId())
            .hasEntrySatisfying("lastTimestampDrift", timeDriftCloseTo(auth.timeDrift))
        assertThatTodayStatistic("notifiedUsers")
            .isEqualTo(0)
        if (auth.pushInfo == null) {
            verifyNoInteractionsWithPushNotifServer()
        } else {
            verifyPushNotifServerReceivedRegisterForToken(auth.pushInfo)
        }
    }

    @ParameterizedTest
    @MethodSource("fr.gouv.stopc.robertserver.ws.test.AuthDataManager#acceptableAuthParameters")
    fun can_request_exposure_status_at_risk(auth: AuthRequestData) {
        givenRegistrationExistsForIdA(
            "idA_1",
            mapOf(
                "atRisk" to true,
                "lastContactTimestamp" to 3849984900L,
                "latestRiskEpoch" to 999
            )
        )

        val now = clock.now()
        given()
            .contentType(JSON)
            .body(
                """
                    {
                      "ebid":    "${"idA_1eb1".base64Encode()}",
                      "epochId": "${auth.epochId()}",
                      "time":    "${auth.base64Time32()}",
                      "mac":     "${auth.base64Mac()}",
                      "pushInfo": ${auth.pushInfo}
                    }
                """.trimIndent()
            )
            .When()
            .post("/api/v6/status")
            .then()
            .statusCode(OK.value())
            .body("riskLevel", equalTo(4))
            .body("tuples", isBase64Encoded(equalTo("fake encrypted tuples for 'idA_1'")))
            .body("config.size()", equalTo(0))
            .body("lastContactDate", equalTo("3849984900"))
            .body("lastRiskScoringDate", isNtpTimestamp(clock.atEpoch(999).asInstant()))
            .body(
                "declarationToken",
                isJwtSignedBy(JWT_KEYS_DECLARATION)
                    .withClaim("jti", matchesRegex("\\w{64}"))
                    .withClaim("iat", isUnixTimestamp(now.asInstant(), offset = 60.seconds))
                    .withClaim("iss", equalTo("tac"))
                    .withClaim(
                        "notificationDateTimestamp",
                        anyOf(
                            isNtpTimestamp(now.truncatedTo(ROBERT_EPOCH).minus(1, ROBERT_EPOCH).asInstant()),
                            isNtpTimestamp(now.truncatedTo(ROBERT_EPOCH).asInstant())
                        )
                    )
                    .withClaim("lastContactDateTimestamp", equalTo(3849984900L))
            )
            .body(
                "analyticsToken",
                isJwtSignedBy(JWT_KEYS_ANALYTICS)
                    .withClaim("iat", isUnixTimestamp(now.asInstant(), 60.seconds))
                    .withClaim(
                        "exp",
                        isUnixTimestamp(
                            now.plus(6, HOURS).asInstant(),
                            60.seconds
                        )
                    )
                    .withClaim("iss", equalTo("robert-server"))
            )
        assertThatRegistrationForIdA("idA_1")
            .hasFieldOrPropertyWithValue("isNotified", true)
            .hasFieldOrPropertyWithValue("lastStatusRequestEpoch", now.asEpochId())
            .hasEntrySatisfying("lastTimestampDrift", timeDriftCloseTo(auth.timeDrift))
        assertThatTodayStatistic("notifiedUsers")
            .isEqualTo(1)
        if (auth.pushInfo == null) {
            verifyNoInteractionsWithPushNotifServer()
        } else {
            verifyPushNotifServerReceivedRegisterForToken(auth.pushInfo)
        }
    }

    @ParameterizedTest
    @MethodSource("fr.gouv.stopc.robertserver.ws.test.AuthDataManager#unacceptableAuthParameters")
    fun cant_request_exposure_status_with_too_much_time_drift(auth: AuthRequestData) {
        givenRegistrationExistsForIdA("idA_1")

        given()
            .contentType(JSON)
            .body(
                """
                    {
                      "ebid":    "${"idA_1eb1".base64Encode()}",
                      "epochId": "${auth.epochId()}",
                      "time":    "${auth.base64Time32()}",
                      "mac":     "${auth.base64Mac()}",
                      "pushInfo": ${auth.pushInfo}
                    }
                """.trimIndent()
            )
            .When()
            .post("/api/v6/status")
            .then()
            .statusCode(BAD_REQUEST.value())
            .body(emptyString())

        verifyNoInteractionsWithPushNotifServer()
        verifyExceededTimeDriftIsProperlyHandled("idA_1", auth.timeDrift)
    }

    @ParameterizedTest
    @MethodSource("cant_make_too_close_exposure_status_requests")
    fun cant_make_too_close_exposure_status_requests(lastStatusEpochSkew: Int, auth: AuthRequestData) {
        val lastStatusRequestEpoch = clock.now().asEpochId() - lastStatusEpochSkew
        givenRegistrationExistsForIdA(
            "idA_1",
            mapOf(
                "lastStatusRequestEpoch" to lastStatusRequestEpoch
            )
        )

        val now = clock.now()
        given()
            .contentType(JSON)
            .body(
                """
                    {
                      "ebid":    "${"idA_1eb1".base64Encode()}",
                      "epochId": "${auth.epochId()}",
                      "time":    "${auth.base64Time32()}",
                      "mac":     "${auth.base64Mac()}",
                      "pushInfo": ${auth.pushInfo}
                    }
                """.trimIndent()
            )
            .When()
            .post("/api/v6/status")
            .then()
            .statusCode(BAD_REQUEST.value())
            .body(emptyString())

        assertThatInfoLogs()
            .contains("Rejected POST /api/v6/status: Number of requests exceeded the limit of 1 over time window of 2 epochs")
        assertThatRegistrationForIdA("idA_1")
            .hasFieldOrPropertyWithValue("lastFailedStatusRequestEpoch", clock.now().asEpochId())
            .hasFieldOrPropertyWithValue(
                "lastFailedStatusRequestMessage",
                "Discarding ESR request because it is too close to the previous one: previous ESR request epoch $lastStatusRequestEpoch vs now ${now.asEpochId()} < 2 epochs"
            )
        verifyNoInteractionsWithPushNotifServer()
    }

    @ParameterizedTest
    @MethodSource("fr.gouv.stopc.robertserver.ws.test.AuthDataManager#acceptableAuthParameters")
    fun http_404_on_missing_registration(auth: AuthRequestData) {
        given()
            .contentType(JSON)
            .body(
                """
                    {
                      "ebid":    "${"idA_1eb1".base64Encode()}",
                      "epochId": "${auth.epochId()}",
                      "time":    "${auth.base64Time32()}",
                      "mac":     "${auth.base64Mac()}",
                      "pushInfo": ${auth.pushInfo}
                    }
                """.trimIndent()
            )
            .When()
            .post("/api/v6/status")
            .then()
            .statusCode(NOT_FOUND.value())
            .body(emptyString())

        assertThatInfoLogs()
            .containsOnlyOnce("Missing registration on POST /api/v6/status: ${"idA_1".base64Encode()}")
        verifyNoInteractionsWithPushNotifServer()
    }

    @ParameterizedTest
    @MethodSource("fr.gouv.stopc.robertserver.ws.test.AuthDataManager#acceptableAuthParameters")
    fun http_430_on_unknown_key(auth: AuthRequestData) {
        givenRegistrationExistsForIdA("idA_1")
        givenCryptoServerRaiseMissingDailyKeyForEbid("idA_1eb1")

        given()
            .contentType(JSON)
            .body(
                """
                    {
                      "ebid":    "${"idA_1eb1".base64Encode()}",
                      "epochId": "${auth.epochId()}",
                      "time":    "${auth.base64Time32()}",
                      "mac":     "${auth.base64Mac()}",
                      "pushInfo": ${auth.pushInfo}
                    }
                """.trimIndent()
            )
            .When()
            .post("/api/v6/status")
            .then()
            .statusCode(430)
            .body(emptyString())

        verifyNoInteractionsWithPushNotifServer()
    }

    @IntegrationTest
    @TestPropertySource(properties = ["robert-ws.min-epochs-between-status-requests=0"])
    class StatisticsTest() {

        @ParameterizedTest
        @MethodSource("fr.gouv.stopc.robertserver.ws.test.AuthDataManager#acceptableAuthParameters")
        fun dont_increment_notifiedUsers_statistics_when_already_notified(auth: AuthRequestData) {
            // given idA____1 is "at risk"
            givenRegistrationExistsForIdA(
                "idA_1",
                mapOf(
                    "atRisk" to true,
                    "lastContactTimestamp" to 3849984900L,
                    "latestRiskEpoch" to 999
                )
            )

            // and idA____1 successfully requested ESR
            // so idA____1 has been notified
            given()
                .contentType(JSON)
                .body(
                    """
                    {
                      "ebid":    "${"idA_1eb1".base64Encode()}",
                      "epochId": "${auth.epochId()}",
                      "time":    "${auth.base64Time32()}",
                      "mac":     "${auth.base64Mac()}",
                      "pushInfo": ${auth.pushInfo}
                    }
                    """.trimIndent()
                )
                .post("/api/v6/status")

            // when idA____1 successfully requests another ESR
            given()
                .contentType(JSON)
                .body(
                    """
                    {
                      "ebid":    "${"idA_1eb1".base64Encode()}",
                      "epochId": "${auth.epochId()}",
                      "time":    "${auth.base64Time32()}",
                      "mac":     "${auth.base64Mac()}",
                      "pushInfo": ${auth.pushInfo}
                    }
                    """.trimIndent()
                )
                .When()
                .post("/api/v6/status")
                .then()
                .statusCode(OK.value())

            // then statistics count 1 single new notified user
            assertThatTodayStatistic("notifiedUsers")
                .isEqualTo(1)
        }
    }
}
