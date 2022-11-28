package fr.gouv.stopc.robertserver.ws.controller

import fr.gouv.stopc.robertserver.common.base64Encode
import fr.gouv.stopc.robertserver.test.MongodbManager.Companion.assertThatRegistrationCollection
import fr.gouv.stopc.robertserver.test.MongodbManager.Companion.givenRegistrationExistsForIdA
import fr.gouv.stopc.robertserver.ws.test.AuthDataManager.AuthRequestData
import fr.gouv.stopc.robertserver.ws.test.GrpcMockManager.Companion.givenCryptoServerRaiseError400ForEbid
import fr.gouv.stopc.robertserver.ws.test.GrpcMockManager.Companion.givenCryptoServerRaiseMissingDailyKeyForEbid
import fr.gouv.stopc.robertserver.ws.test.IntegrationTest
import fr.gouv.stopc.robertserver.ws.test.MockServerManager.Companion.verifyNoInteractionsWithPushNotifServer
import fr.gouv.stopc.robertserver.ws.test.When
import fr.gouv.stopc.robertserver.ws.test.matchers.verifyExceededTimeDriftIsProperlyHandled
import io.restassured.RestAssured.given
import io.restassured.http.ContentType.JSON
import org.hamcrest.Matchers.emptyString
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.nullValue
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.HttpStatus.OK

@IntegrationTest
class UnregisterControllerTest {

    @ParameterizedTest
    @MethodSource("fr.gouv.stopc.robertserver.ws.test.AuthDataManager#acceptableAuthParameters")
    fun can_unregister(auth: AuthRequestData) {
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
            .post("/api/v6/unregister")
            .then()
            .statusCode(OK.value())
            .body("success", equalTo(true))
            .body("message", nullValue())

        assertThatRegistrationCollection()
            .isEmpty()
        verifyNoInteractionsWithPushNotifServer()
    }

    @ParameterizedTest
    @MethodSource("fr.gouv.stopc.robertserver.ws.test.AuthDataManager#unacceptableAuthParameters")
    fun cant_unregister_with_too_much_time_drift(auth: AuthRequestData) {
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
            .post("/api/v6/unregister")
            .then()
            .statusCode(BAD_REQUEST.value())
            .body(emptyString())

        verifyExceededTimeDriftIsProperlyHandled("idA_1", auth.timeDrift)
        assertThatRegistrationCollection()
            .extracting("permanentIdentifier")
            .containsExactly("idA_1")
        verifyNoInteractionsWithPushNotifServer()
    }

    @ParameterizedTest
    @MethodSource("fr.gouv.stopc.robertserver.ws.test.AuthDataManager#acceptableAuthParameters")
    fun cant_unregister_for_a_key_unknown_by_crypto_server(auth: AuthRequestData) {
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
            .post("/api/v6/unregister")
            .then()
            .statusCode(430)
            .body(emptyString())
    }

    @ParameterizedTest
    @MethodSource("fr.gouv.stopc.robertserver.ws.test.AuthDataManager#acceptableAuthParameters")
    fun bad_request_on_mac_validation_error_raised_by_crypto_server(auth: AuthRequestData) {
        givenRegistrationExistsForIdA("idA_1")
        givenCryptoServerRaiseError400ForEbid("idA_1eb1")

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
            .post("/api/v6/unregister")
            .then()
            .statusCode(BAD_REQUEST.value())
            .body(emptyString())

        assertThatRegistrationCollection()
            .extracting("permanentIdentifier")
            .containsExactly("idA_1")
    }

    @ParameterizedTest
    @MethodSource("fr.gouv.stopc.robertserver.ws.test.AuthDataManager#acceptableAuthParameters")
    fun not_found_for_an_unexisting_registration(auth: AuthRequestData) {
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
            .post("/api/v6/unregister")
            .then()
            .statusCode(NOT_FOUND.value())
            .body(emptyString())
    }
}
