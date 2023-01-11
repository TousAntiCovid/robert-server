package fr.gouv.stopc.robertserver.ws.controller

import fr.gouv.stopc.robertserver.common.base64Encode
import fr.gouv.stopc.robertserver.test.MongodbManager
import fr.gouv.stopc.robertserver.test.MongodbManager.Companion.assertThatContactsToProcessCollection
import fr.gouv.stopc.robertserver.test.MongodbManager.HelloMessageToProcess
import fr.gouv.stopc.robertserver.test.MongodbManager.MongoContactToProcess
import fr.gouv.stopc.robertserver.test.MongodbManager.MongoMessageDetails
import fr.gouv.stopc.robertserver.test.assertThatInfoLogs
import fr.gouv.stopc.robertserver.test.matchers.isJwtSignedBy
import fr.gouv.stopc.robertserver.test.matchers.isUnixTimestamp
import fr.gouv.stopc.robertserver.ws.test.IntegrationTest
import fr.gouv.stopc.robertserver.ws.test.JwtKeysManager.JWT_KEYS
import fr.gouv.stopc.robertserver.ws.test.MockServerManager.Companion.verifyNoInteractionsWithSubmissionCodeServer
import fr.gouv.stopc.robertserver.ws.test.StatisticsManager.Companion.assertThatTodayStatistic
import fr.gouv.stopc.robertserver.ws.test.When
import io.restassured.RestAssured.given
import io.restassured.http.ContentType.JSON
import org.assertj.core.api.HamcrestCondition.matching
import org.hamcrest.Matchers.emptyString
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasSize
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.OK
import org.springframework.http.HttpStatus.UNAUTHORIZED
import java.time.Instant
import java.time.temporal.ChronoUnit.MINUTES
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@IntegrationTest
class ReportControllerTest {

    @Test
    fun can_report() {
        given()
            .contentType(JSON)
            .body(
                """
                    {
                      "token": "validZ",
                      "contacts": [
                        {
                          "ebid": "${"fake ebid A".base64Encode()}",
                          "ecc": "${"fake ecc".base64Encode()}",
                          "ids": [
                            {
                              "rssiCalibrated": 0,
                              "timeCollectedOnDevice": 1,
                              "timeFromHelloMessage": 2,
                              "mac": "${"fake mac A1".base64Encode()}"
                              }
                          ]
                        }, 
                        {
                          "ebid": "${"fake ebid B".base64Encode()}",
                          "ecc": "${"fake ecc".base64Encode()}",
                          "ids": [
                            {
                              "rssiCalibrated": 10,
                              "timeCollectedOnDevice": 11,
                              "timeFromHelloMessage": 12,
                              "mac": "${"fake mac B1".base64Encode()}"
                            },
                            {
                              "rssiCalibrated": 20,
                              "timeCollectedOnDevice": 21,
                              "timeFromHelloMessage": 22,
                              "mac": "${"fake mac B2".base64Encode()}"
                            }
                          ]
                        }
                      ]
                    }
                """.trimIndent()
            )
            .When()
            .post("/api/v6/report")
            .then()
            .statusCode(OK.value())
            .body("success", equalTo(true))
            .body("message", equalTo("Successful operation"))
            .body(
                "reportValidationToken",
                isJwtSignedBy(JWT_KEYS)
                    .withClaim("iat", isUnixTimestamp(Instant.now().minusSeconds(30), 30.seconds))
                    .withClaim("exp", isUnixTimestamp(Instant.now().plus(5, MINUTES), 1.minutes))
            )

        assertThatContactsToProcessCollection()
            .flatExtracting<HelloMessageToProcess>(MongodbManager::flattenHelloMessage)
            .containsExactlyInAnyOrder(
                HelloMessageToProcess("fake ebid A", "fake ecc", 2, "fake mac A1", 0, 1L),
                HelloMessageToProcess("fake ebid B", "fake ecc", 12, "fake mac B1", 10, 11L),
                HelloMessageToProcess("fake ebid B", "fake ecc", 22, "fake mac B2", 20, 21L)
            )
    }

    @Test
    fun can_report_one_large_contact_with_100k_hello_messages() {
        val manyHelloMessages = (1..100000).joinToString(",") {
            """
                {
                  "rssiCalibrated": 10,
                  "timeCollectedOnDevice": $it,
                  "timeFromHelloMessage": ${it + 1},
                  "mac": "${"mac $it".base64Encode()}"
                }
            """.trimIndent()
        }

        given()
            .contentType(JSON)
            .body(
                """
                    {
                      "token": "validZ",
                      "contacts": [
                        {
                          "ebid": "${"fake ebid".base64Encode()}",
                          "ecc": "${"fake ecc".base64Encode()}",
                          "ids": [
                            $manyHelloMessages
                          ]
                        }
                      ]
                    }
                """.trimIndent()
            )
            .When()
            .post("/api/v6/report")
            .then()
            .statusCode(OK.value())
            .body("success", equalTo(true))
            .body("message", equalTo("Successful operation"))
            .body(
                "reportValidationToken",
                isJwtSignedBy(JWT_KEYS)
                    .withClaim("iat", isUnixTimestamp(Instant.now().minusSeconds(30), 30.seconds))
                    .withClaim("exp", isUnixTimestamp(Instant.now().plus(5, MINUTES), 1.minutes))
            )

        assertThatContactsToProcessCollection()
            .hasSize(1)
            .flatExtracting<HelloMessageToProcess>(MongodbManager::flattenHelloMessage)
            .hasSize(100000)
    }

    @Test
    fun can_report_10k_contacts_with_10_hello_messages_each() {
        val manyContacts = (1..10000).joinToString(",") { i ->
            val helloMessageDetails = (1..10).joinToString(",") { j ->
                """
                    {
                      "rssiCalibrated": 10,
                      "timeCollectedOnDevice": $j,
                      "timeFromHelloMessage": ${j + 1},
                      "mac": "${"fake mac $i $j".base64Encode()}"
                    }
                """.trimIndent()
            }
            """
                {
                  "ebid": "",
                  "ecc": "",
                  "ids": [
                    $helloMessageDetails
                  ]
                }
            """.trimIndent()
        }
        given()
            .contentType(JSON)
            .body(
                """
                    {
                      "token": "validZ",
                      "contacts": [
                        $manyContacts
                      ]
                    }
                """.trimIndent()
            )
            .When()
            .post("/api/v6/report")
            .then()
            .statusCode(OK.value())
            .body("success", equalTo(true))
            .body("message", equalTo("Successful operation"))
            .body(
                "reportValidationToken",
                isJwtSignedBy(JWT_KEYS)
                    .withClaim("iat", isUnixTimestamp(Instant.now().minusSeconds(30), 30.seconds))
                    .withClaim("exp", isUnixTimestamp(Instant.now().plus(5, MINUTES), 1.minutes))
            )

        assertThatContactsToProcessCollection()
            .extracting<List<MongoMessageDetails>>(MongoContactToProcess::messageDetails)
            .areExactly(10000, matching(hasSize<List<MongoMessageDetails>>(10)))
    }

    @Test
    fun can_report_empty_contact_list() {
        given()
            .contentType(JSON)
            .body(
                """
                    {
                      "token": "validZ",
                      "contacts": []
                    }
                """.trimIndent()
            )
            .When()
            .post("/api/v6/report")
            .then()
            .statusCode(OK.value())
            .body("success", equalTo(true))
            .body("message", equalTo("Successful operation"))
            .body(
                "reportValidationToken",
                isJwtSignedBy(JWT_KEYS)
                    .withClaim("iat", isUnixTimestamp(Instant.now().minusSeconds(30), 30.seconds))
                    .withClaim("exp", isUnixTimestamp(Instant.now().plus(5, MINUTES), 1.minutes))
            )

        assertThatContactsToProcessCollection()
            .isEmpty()
    }

    @Test
    fun cant_report_null_contact_list() {
        given()
            .contentType(JSON)
            .body(
                """
                    {
                      "token": "validZ",
                      "contacts": null
                    }
                """.trimIndent()
            )
            .When()
            .post("/api/v6/report")
            .then()
            .statusCode(BAD_REQUEST.value())
            .body(emptyString())

        verifyNoInteractionsWithSubmissionCodeServer()
    }

    @Test
    fun cant_report_without_token() {
        given()
            .contentType(JSON)
            .body(
                """
                    {
                      "token": null,
                      "contacts": []
                    }
                """.trimIndent()
            )
            .When()
            .post("/api/v6/report")
            .then()
            .statusCode(BAD_REQUEST.value())
            .body(emptyString())

        assertThatInfoLogs()
            .contains("Request validation failed on POST /api/v6/report: field 'token' rejected value [null] should not be null (NotNull)")
        verifyNoInteractionsWithSubmissionCodeServer()
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "err", "short0", "test00test00", "long0000-0000-0000-0000-000000000000"])
    fun cant_report_with_invalid_token(invalidToken: String) {
        given()
            .contentType(JSON)
            .body(
                """
                    {
                      "token": "$invalidToken",
                      "contacts": []
                    }
                """.trimIndent()
            )
            .When()
            .post("/api/v6/report")
            .then()
            .statusCode(UNAUTHORIZED.value())
            .body(emptyString())

        assertThatInfoLogs()
            .containsOnlyOnce("Invalid report token of length ${invalidToken.length}")
    }

    @IntegrationTest
    internal class StatisticsTest {
        @Test
        fun increment_reportsCount_stat_when_report() {
            given()
                .contentType(JSON)
                .body(
                    """
                    {
                      "token": "validZ",
                      "contacts": []
                    }
                    """.trimIndent()
                )
                .When()
                .post("/api/v6/report")
                .then()
                .statusCode(OK.value())
                .body("success", equalTo(true))
                .body("message", equalTo("Successful operation"))

            assertThatTodayStatistic("reportsCount")
                .isEqualTo(1)
        }
    }
}
