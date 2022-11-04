package fr.gouv.stopc.robertserver.ws.controller

import fr.gouv.stopc.robertserver.common.RobertClock
import fr.gouv.stopc.robertserver.common.base64Encode
import fr.gouv.stopc.robertserver.test.MongodbManager.Companion.assertThatRegistrationForIdA
import fr.gouv.stopc.robertserver.test.MongodbManager.Companion.givenRegistrationExistsForIdA
import fr.gouv.stopc.robertserver.test.MongodbManager.MongoEpochExposition
import fr.gouv.stopc.robertserver.ws.test.AuthDataManager
import fr.gouv.stopc.robertserver.ws.test.AuthDataManager.AuthRequestData
import fr.gouv.stopc.robertserver.ws.test.GrpcMockManager.Companion.givenCryptoServerRaiseError400ForEbid
import fr.gouv.stopc.robertserver.ws.test.GrpcMockManager.Companion.givenCryptoServerRaiseMissingDailyKeyForEbid
import fr.gouv.stopc.robertserver.ws.test.IntegrationTest
import fr.gouv.stopc.robertserver.ws.test.When
import fr.gouv.stopc.robertserver.ws.test.matchers.verifyExceededTimeDriftIsProperlyHandled
import io.restassured.RestAssured.given
import io.restassured.http.ContentType.JSON
import org.assertj.core.api.HamcrestCondition.matching
import org.hamcrest.Matchers.contains
import org.hamcrest.Matchers.emptyIterable
import org.hamcrest.Matchers.emptyString
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.not
import org.hamcrest.Matchers.nullValue
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.HttpStatus.OK

@IntegrationTest
class DeleteHistoryControllerTest(@Autowired private val clock: RobertClock) {

    companion object {

        @JvmStatic
        fun acceptableAuthParameters() = AuthDataManager.acceptableAuthParameters()
            .filter { it.pushInfo == null }
    }

    @ParameterizedTest
    @MethodSource("acceptableAuthParameters")
    fun can_delete_history(auth: AuthRequestData) {
        givenRegistrationExistsForIdA("idA_1", MongoEpochExposition(clock.now().asEpochId(), listOf(1.0, 1.2)))

        given()
            .contentType(JSON)
            .body(
                """
                    {
                      "ebid":    "${"idA_1eb1".base64Encode()}",
                      "epochId": "${auth.epochId()}",
                      "time":    "${auth.base64Time32()}",
                      "mac":     "${auth.base64Mac()}"
                    }
                """.trimIndent()
            )
            .When()
            .post("/api/v6/deleteExposureHistory")
            .then()
            .statusCode(OK.value())
            .body("success", equalTo(true))
            .body("message", nullValue())

        assertThatRegistrationForIdA("idA_1")
            .hasEntrySatisfying("exposedEpochs", matching(emptyIterable<Any>()))
    }

    @ParameterizedTest
    @MethodSource("acceptableAuthParameters")
    fun can_delete_empty_history(auth: AuthRequestData) {
        givenRegistrationExistsForIdA("idA_1")

        given()
            .contentType(JSON)
            .body(
                """
                    {
                      "ebid":    "${"idA_1eb1".base64Encode()}",
                      "epochId": "${auth.epochId()}",
                      "time":    "${auth.base64Time32()}",
                      "mac":     "${auth.base64Mac()}"
                    }
                """.trimIndent()
            )
            .When()
            .post("/api/v6/deleteExposureHistory")
            .then()
            .statusCode(OK.value())
            .body("success", equalTo(true))
            .body("message", nullValue())

        assertThatRegistrationForIdA("idA_1")
            .hasEntrySatisfying("exposedEpochs", matching(emptyIterable<Any>()))
    }

    @ParameterizedTest
    @MethodSource("fr.gouv.stopc.robertserver.ws.test.AuthDataManager#unacceptableAuthParameters")
    fun cant_delete_history_with_too_much_time_drift(auth: AuthRequestData) {
        givenRegistrationExistsForIdA("idA_1", MongoEpochExposition(433, listOf(1.1, 1.3)))

        given()
            .contentType(JSON)
            .body(
                """
                    {
                      "ebid":    "${"idA_1eb1".base64Encode()}",
                      "epochId": "${auth.epochId()}",
                      "time":    "${auth.base64Time32()}",
                      "mac":     "${auth.base64Mac()}"
                    }
                """.trimIndent()
            )
            .When()
            .post("/api/v6/deleteExposureHistory")
            .then()
            .statusCode(BAD_REQUEST.value())
            .body(emptyString())

        verifyExceededTimeDriftIsProperlyHandled("idA_1", auth.timeDrift)
        assertThatRegistrationForIdA("idA_1")
            .hasEntrySatisfying("exposedEpochs", matching(not(emptyIterable<Any>())))
    }

    @ParameterizedTest
    @MethodSource("acceptableAuthParameters")
    fun cant_delete_history_for_a_key_unknown_by_crypto_server(auth: AuthRequestData) {
        givenRegistrationExistsForIdA("idA_1", MongoEpochExposition(444, listOf(1.2, 1.4)))
        givenCryptoServerRaiseMissingDailyKeyForEbid("idA_1eb1")

        given()
            .contentType(JSON)
            .body(
                """
                    {
                      "ebid":    "${"idA_1eb1".base64Encode()}",
                      "epochId": "${auth.epochId()}",
                      "time":    "${auth.base64Time32()}",
                      "mac":     "${auth.base64Mac()}"
                    }
                """.trimIndent()
            )
            .When()
            .post("/api/v6/deleteExposureHistory")
            .then()
            .statusCode(430)
            .body(emptyString())

        assertThatRegistrationForIdA("idA_1")
            .hasEntrySatisfying("exposedEpochs", matching(not(emptyIterable<Any>())))
    }

    @ParameterizedTest
    @MethodSource("acceptableAuthParameters")
    fun bad_request_on_mac_validation_error_raised_by_crypto_server(auth: AuthRequestData) {
        givenRegistrationExistsForIdA("idA_1", MongoEpochExposition(455, listOf(1.3, 1.5)))
        givenCryptoServerRaiseError400ForEbid("idA_1eb1")

        given()
            .contentType(JSON)
            .body(
                """
                    {
                      "ebid":    "${"idA_1eb1".base64Encode()}",
                      "epochId": "${auth.epochId()}",
                      "time":    "${auth.base64Time32()}",
                      "mac":     "${auth.base64Mac()}"
                    }
                """.trimIndent()
            )
            .When()
            .post("/api/v6/deleteExposureHistory")
            .then()
            .statusCode(BAD_REQUEST.value())
            .body(emptyString())

        assertThatRegistrationForIdA("idA_1")
            .hasEntrySatisfying("exposedEpochs", matching(not(emptyIterable<Any>())))
    }

    @ParameterizedTest
    @MethodSource("acceptableAuthParameters")
    fun not_found_for_an_unexisting_registration(auth: AuthRequestData) {
        given()
            .contentType(JSON)
            .body(
                """
                    {
                      "ebid":    "${"idA_1eb1".base64Encode()}",
                      "epochId": "${auth.epochId()}",
                      "time":    "${auth.base64Time32()}",
                      "mac":     "${auth.base64Mac()}"
                    }
                """.trimIndent()
            )
            .When()
            .post("/api/v6/deleteExposureHistory")
            .then()
            .statusCode(NOT_FOUND.value())
            .body(emptyString())
    }
}
