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
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
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
                "ebid" to "non_base64_value",
                "time" to null,
                "time" to "non_base64_value",
                "mac" to null,
                "mac" to "non_base64_value"
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

    @ParameterizedTest(name = "when {0} with {1}=null, then Bad request")
    @CsvSource(
        value = [
            "/status,                ebid",
            "/status,                time",
            "/status,                mac",
            "/deleteExposureHistory, ebid",
            "/deleteExposureHistory, time",
            "/deleteExposureHistory, mac",
            "/unregister,            ebid",
            "/unregister,            time",
            "/unregister,            mac"
        ]
    )
    fun bad_request_on_null_auth_parameters(apiResource: String, paramName: String) {
        given()
            .contentType(JSON)
            .body(validAuth + (paramName to null))
            .When()
            .post("/api/v6$apiResource")
            .then()
            .statusCode(BAD_REQUEST.value())
            .body(emptyString())

        assertThatInfoLogs()
            .contains("Request validation failed on POST /api/v6$apiResource: field '$paramName' rejected value [null] should not be null (NotNull)")
    }

    @ParameterizedTest(name = "when {0} with malformed base64 value for {1}, then Bad request")
    @CsvSource(
        value = [
            "/status,                ebid",
            "/status,                time",
            "/status,                mac",
            "/deleteExposureHistory, ebid",
            "/deleteExposureHistory, time",
            "/deleteExposureHistory, mac",
            "/unregister,            ebid",
            "/unregister,            time",
            "/unregister,            mac"
        ]
    )
    fun bad_request_on_malformed_base64_auth_parameters(apiResource: String, paramName: String) {
        given()
            .contentType(JSON)
            .body(validAuth + (paramName to "malformed_base64"))
            .When()
            .post("/api/v6$apiResource")
            .then()
            .statusCode(BAD_REQUEST.value())
            .body(emptyString())

        assertThatInfoLogs()
            .contains("""Request validation failed on POST /api/v6$apiResource: field '$paramName' rejected value [null] Cannot deserialize value of type `byte[]` from String "base64": Failed to decode VALUE_STRING as base64 (MIME-NO-LINEFEEDS): Illegal character '_' (code 0x5f) in base64 content (InvalidFormatException)""")
    }
}
