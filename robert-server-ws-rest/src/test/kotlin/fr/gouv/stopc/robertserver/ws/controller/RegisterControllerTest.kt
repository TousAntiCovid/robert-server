package fr.gouv.stopc.robertserver.ws.controller

import fr.gouv.stopc.robertserver.common.RobertClock
import fr.gouv.stopc.robertserver.common.base64Encode
import fr.gouv.stopc.robertserver.test.LogbackManager.Companion.assertThatErrorLogs
import fr.gouv.stopc.robertserver.test.MongodbManager.Companion.givenMongodbIsOffline
import fr.gouv.stopc.robertserver.test.matchers.isBase64Encoded
import fr.gouv.stopc.robertserver.ws.test.GrpcMockManager.Companion.givenCryptoServerIsOffline
import fr.gouv.stopc.robertserver.ws.test.GrpcMockManager.Companion.verifyNoInteractionsWithCryptoServer
import fr.gouv.stopc.robertserver.ws.test.IntegrationTest
import fr.gouv.stopc.robertserver.ws.test.MockServerManager.Companion.verifyNoInteractionsWithPushNotifServer
import fr.gouv.stopc.robertserver.ws.test.MockServerManager.Companion.verifyPushNotifServerReceivedRegisterForToken
import fr.gouv.stopc.robertserver.ws.test.When
import io.restassured.RestAssured.given
import io.restassured.http.ContentType.JSON
import org.hamcrest.Matchers.emptyString
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.nullValue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus.CREATED
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.http.HttpStatus.UNAUTHORIZED

@IntegrationTest
class RegisterControllerTest(@Autowired private val clock: RobertClock) {

    @Test
    fun can_register() {
        given()
            .contentType(JSON)
            .body(
                """
                    {
                      "captcha": "valid challenge answer",
                      "captchaId": "captcha-id",
                      "clientPublicECDHKey": "${"public key for idA_1".base64Encode()}"
                    }
                """.trimIndent()
            )
            .When()
            .post("/api/v6/register")
            .then()
            .statusCode(CREATED.value())
            .body("timeStart", equalTo(clock.atEpoch(0).asNtpTimestamp()))
            .body("message", nullValue())
            .body("config.size()", equalTo(0))
            .body("tuples", isBase64Encoded(equalTo("fake encrypted tuples for 'idA_1'")))

        verifyNoInteractionsWithPushNotifServer()
    }

    @Test
    fun can_register_apple_device() {
        given()
            .contentType(JSON)
            .body(
                """
                    {
                      "captcha": "valid challenge answer",
                      "captchaId": "captcha-id",
                      "clientPublicECDHKey": "${"public key for idA_1".base64Encode()}",
                      "pushInfo": {
                        "token": "valid-device-id",
                        "locale": "fr-FR",
                        "timezone": "Europe/Paris"
                      }
                    }
                """.trimIndent()
            )
            .When()
            .post("/api/v6/register")
            .then()
            .statusCode(CREATED.value())
            .body("timeStart", equalTo(clock.atEpoch(0).asNtpTimestamp()))
            .body("message", nullValue())
            .body("config.size()", equalTo(0))
            .body("tuples", isBase64Encoded(equalTo("fake encrypted tuples for 'idA_1'")))

        verifyPushNotifServerReceivedRegisterForToken(
            token = "valid-device-id",
            locale = "fr-FR",
            timezone = "Europe/Paris"
        )
    }

    @Test
    fun cant_register_with_wrong_captcha_answer() {
        given()
            .contentType(JSON)
            .body(
                """
                    {
                      "captcha": "wrong challenge answer",
                      "captchaId": "captcha-id",
                      "clientPublicECDHKey": "${"public key for idA_1".base64Encode()}",
                      "pushInfo": {
                        "token": "valid-device-id",
                        "locale": "fr-FR",
                        "timezone": "Europe/Paris"
                      }
                    }
                """.trimIndent()
            )
            .When()
            .post("/api/v6/register")
            .then()
            .statusCode(UNAUTHORIZED.value())
            .body(emptyString())

        verifyNoInteractionsWithCryptoServer()
        verifyNoInteractionsWithPushNotifServer()
    }

    @ParameterizedTest
    @CsvSource(
        nullValues = ["NULL"],
        value = ["captcha-id,NULL", "NULL,some challenge answer", ",some challenge answer", "captcha-id,"]
    )
    fun cant_register_with_invalid_captcha_details(captchaId: String?, captchaChallengeResponse: String?) {
        given()
            .contentType(JSON)
            .body(
                """
                    {
                      "captcha": "$captchaChallengeResponse",
                      "captchaId": "$captchaId",
                      "clientPublicECDHKey": "${"public key for idA_1".base64Encode()}",
                      "pushInfo": {
                        "token": "valid-device-id",
                        "locale": "fr-FR",
                        "timezone": "Europe/Paris"
                      }
                    }
                """.trimIndent()
            )
            .When()
            .post("/api/v6/register")
            .then()
            .statusCode(UNAUTHORIZED.value())

        verifyNoInteractionsWithCryptoServer()
        verifyNoInteractionsWithPushNotifServer()
    }

    @Test
    fun cant_register_when_database_is_offline() {
        givenMongodbIsOffline()

        given()
            .contentType(JSON)
            .body(
                """
                    {
                      "captcha": "valid challenge answer",
                      "captchaId": "captcha-id",
                      "clientPublicECDHKey": "${"public key for idA_1".base64Encode()}",
                      "pushInfo": {
                        "token": "valid-device-id",
                        "locale": "fr-FR",
                        "timezone": "Europe/Paris"
                      }
                    }
                """.trimIndent()
            )
            .When()
            .post("/api/v6/register")
            .then()
            .statusCode(INTERNAL_SERVER_ERROR.value())
            .body("message", equalTo("An error occured"))

        assertThatErrorLogs()
            .contains("Timeout while receiving message; nested exception is com.mongodb.MongoSocketReadTimeoutException: Timeout while receiving message")

        verifyNoInteractionsWithPushNotifServer()
    }

    @Test
    fun cant_register_when_crypto_server_is_offline() {
        givenCryptoServerIsOffline()

        given()
            .contentType(JSON)
            .body(
                """
                    {
                      "captcha": "valid challenge answer",
                      "captchaId": "captcha-id",
                      "clientPublicECDHKey": "${"public key for idA_1".base64Encode()}",
                      "pushInfo": {
                        "token": "valid-device-id",
                        "locale": "fr-FR",
                        "timezone": "Europe/Paris"
                      }
                    }
                """.trimIndent()
            )
            .When()
            .post("/api/v6/register")
            .then()
            .statusCode(INTERNAL_SERVER_ERROR.value())
            .body("message", equalTo("An error occured"))

        assertThatErrorLogs()
            .containsSequence(
                "RPC failed: UNKNOWN",
                "Unable to generate an identity for the client"
            )

        verifyNoInteractionsWithPushNotifServer()
    }
}
