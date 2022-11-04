package fr.gouv.stopc.robertserver.ws.controller.crosscuttingconcerns

import fr.gouv.stopc.robertserver.common.base64Encode
import fr.gouv.stopc.robertserver.test.LogbackManager.Companion.assertThatInfoLogs
import fr.gouv.stopc.robertserver.test.LogbackManager.Companion.assertThatWarnLogs
import fr.gouv.stopc.robertserver.test.MongodbManager.Companion.givenRegistrationExistsForIdA
import fr.gouv.stopc.robertserver.ws.test.AuthDataManager
import fr.gouv.stopc.robertserver.ws.test.AuthDataManager.AuthRequestData
import fr.gouv.stopc.robertserver.ws.test.GrpcMockManager.Companion.givenCryptoServerRaiseMissingIdentityForEbid
import fr.gouv.stopc.robertserver.ws.test.IntegrationTest
import fr.gouv.stopc.robertserver.ws.test.When
import io.restassured.RestAssured.given
import io.restassured.http.ContentType.JSON
import org.assertj.core.api.HamcrestCondition.matching
import org.hamcrest.Matchers.emptyString
import org.hamcrest.Matchers.matchesPattern
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.junitpioneer.jupiter.cartesian.ArgumentSets
import org.junitpioneer.jupiter.cartesian.CartesianTest
import org.springframework.http.HttpStatus.BAD_REQUEST

@IntegrationTest
class AuthenticationErrorsTest {

    private val validAuth = AuthDataManager.acceptableAuthParameters().random()

    companion object {

        @JvmStatic
        fun unacceptable_auth_parameters() = ArgumentSets.create()
            .argumentsForNextParameter("/status", "/deleteExposureHistory", "/unregister")
            .argumentsForNextParameter(AuthDataManager.unacceptableAuthParameters())
    }

    @ParameterizedTest
    @ValueSource(strings = ["/status", "/deleteExposureHistory", "/unregister"])
    fun http_status_430_on_unknown_credentials(apiResource: String) {
        givenRegistrationExistsForIdA("idA____1")
        givenCryptoServerRaiseMissingIdentityForEbid("idA____1")
        given()
            .contentType(JSON)
            .body(
                """
                    {
                      "ebid":    "${"idA____1".base64Encode()}",
                      "epochId": "${validAuth.epochId()}",
                      "time":    "${validAuth.base64Time32()}",
                      "mac":     "${validAuth.base64Mac()}"
                    }
                """.trimIndent()
            )
            .When()
            .post("/api/v6$apiResource")
            .then()
            .statusCode(430)
            .body(emptyString())
    }

    @CartesianTest
    @CartesianTest.MethodFactory("unacceptable_auth_parameters")
    fun bad_request_on_request_with_too_much_time_drift(apiResource: String, auth: AuthRequestData) {
        givenRegistrationExistsForIdA("idA____1")
        givenCryptoServerRaiseMissingIdentityForEbid("idA____1")
        given()
            .contentType(JSON)
            .body(
                """
                    {
                      "ebid":    "${"idA____1".base64Encode()}",
                      "epochId": "${auth.epochId()}",
                      "time":    "${auth.base64Time32()}",
                      "mac":     "${auth.base64Mac()}"
                    }
                """.trimIndent()
            )
            .When()
            .post("/api/v6$apiResource")
            .then()
            .statusCode(BAD_REQUEST.value())
            .body(emptyString())

        assertThatWarnLogs()
            .areExactly(
                1,
                matching(
                    matchesPattern(
                        "Witnessing abnormal time difference ${-auth.timeDrift.inWholeSeconds} between client: [0-9-T:.Z=E]+ and server: [0-9-T:.Z=E]+"
                    )
                )
            )
        assertThatInfoLogs()
            .contains("Discarding authenticated request because provided time is too far from current server time")
    }
}
