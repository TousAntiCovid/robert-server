package fr.gouv.stopc.robertserver.ws.controller.crosscuttingconcerns

import fr.gouv.stopc.robertserver.common.RobertClock
import fr.gouv.stopc.robertserver.common.base64Encode
import fr.gouv.stopc.robertserver.test.LogbackManager.Companion.assertThatInfoLogs
import fr.gouv.stopc.robertserver.ws.test.AuthDataManager
import fr.gouv.stopc.robertserver.ws.test.IntegrationTest
import fr.gouv.stopc.robertserver.ws.test.When
import io.restassured.RestAssured.given
import io.restassured.http.ContentType.JSON
import org.assertj.core.api.HamcrestCondition.matching
import org.hamcrest.Matchers.emptyString
import org.hamcrest.Matchers.stringContainsInOrder
import org.junit.jupiter.api.DisplayName
import org.junitpioneer.jupiter.cartesian.ArgumentSets
import org.junitpioneer.jupiter.cartesian.CartesianTest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus.BAD_REQUEST

@IntegrationTest
@DisplayName("Authentication: ebid should be 12 base64 chars, time 8 base64 chars and mac 44 base64 chars")
class AuthenticationParamValidationTest(@Autowired private val clock: RobertClock) {

    private val validAuth = AuthDataManager.acceptableAuthParameters()
        .random()
        .let {
            mapOf(
                "ebid" to "idA____1".base64Encode(),
                "epochId" to it.epochId(),
                "time" to it.base64Time32(),
                "mac" to it.base64Mac()
            )
        }

    companion object {
        @JvmStatic
        fun invalid_length_auth_parameters(): ArgumentSets = ArgumentSets.create()
            .argumentsForNextParameter("/status", "/deleteExposureHistory", "/unregister")
            .argumentsForNextParameter(
                "ebid" to "too short ebid".toByteArray().take(7).toByteArray().base64Encode(),
                "ebid" to "too long ebid".toByteArray().take(9).toByteArray().base64Encode(),
                "time" to "too short time32".toByteArray().take(3).toByteArray().base64Encode(),
                "time" to "too long time32".toByteArray().take(5).toByteArray().base64Encode(),
                "mac" to "too short mac value that is rejected by the server before even attempting any check"
                    .toByteArray().take(31).toByteArray().base64Encode(),
                "mac" to "too long mac value that is rejected by the server before even attempting any check"
                    .toByteArray().take(33).toByteArray().base64Encode()
            )

        @JvmStatic
        fun malformed_auth_parameters(): ArgumentSets = ArgumentSets.create()
            .argumentsForNextParameter("/status", "/deleteExposureHistory", "/unregister")
            .argumentsForNextParameter(
                "ebid" to null,
                "ebid" to "non_base64_ebid",
                "time" to null,
                "time" to "non_base64_time",
                "mac" to null,
                "mac" to "non_base64_mac"
            )
    }

    @CartesianTest(name = "when {0} with {1}, then Bad request")
    @CartesianTest.MethodFactory("invalid_length_auth_parameters")
    fun bad_request_on_invalid_length_auth_parameters(
        apiResource: String,
        invalidAuthParam: Pair<String, String?>
    ) {
        given()
            .contentType(JSON)
            .body(validAuth + invalidAuthParam)
            .When()
            .post("/api/v6$apiResource")
            .then()
            .statusCode(BAD_REQUEST.value())
            .body(emptyString())

        assertThatInfoLogs()
            .areExactly(
                1,
                matching(
                    stringContainsInOrder(
                        "Request validation failed on POST /api/v6$apiResource: field '${invalidAuthParam.first}' rejected value [${invalidAuthParam.second}] must be ",
                        " bytes long (Size)"
                    )
                )
            )
    }

    @CartesianTest(name = "when {0} with {1}, then Bad request")
    @CartesianTest.MethodFactory("malformed_auth_parameters")
    fun bad_request_on_malformed_auth_parameters(
        apiResource: String,
        invalidAuthParam: Pair<String, String?>
    ) {
        given()
            .contentType(JSON)
            .body(validAuth + invalidAuthParam)
            .When()
            .post("/api/v6$apiResource")
            .then()
            .statusCode(BAD_REQUEST.value())
            .body(emptyString())

        assertThatInfoLogs()
            .areExactly(
                1,
                matching(
                    stringContainsInOrder(
                        "Unacceptable request on POST /api/v6$apiResource: ServerWebInputException 400 BAD_REQUEST \"Failed to read HTTP message\";"
                    )
                )
            )
    }
}
