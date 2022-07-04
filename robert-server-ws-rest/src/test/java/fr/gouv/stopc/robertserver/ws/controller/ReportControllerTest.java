package fr.gouv.stopc.robertserver.ws.controller;

import fr.gouv.stopc.robertserver.database.model.Contact;
import fr.gouv.stopc.robertserver.ws.test.IntegrationTest;
import fr.gouv.stopc.robertserver.ws.test.MongodbManager;
import fr.gouv.stopc.robertserver.ws.vo.ContactVo;
import fr.gouv.stopc.robertserver.ws.vo.HelloMessageDetailVo;
import fr.gouv.stopc.robertserver.ws.vo.ReportBatchRequestVo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.IntStream;

import static fr.gouv.stopc.robertserver.ws.test.JwtKeysManager.JWT_KEYS;
import static fr.gouv.stopc.robertserver.ws.test.LogbackManager.assertThatWarnLogs;
import static fr.gouv.stopc.robertserver.ws.test.MockServerManager.verifyNoInteractionsWithSubmissionCodeServer;
import static fr.gouv.stopc.robertserver.ws.test.MongodbManager.assertThatContactsToProcess;
import static fr.gouv.stopc.robertserver.ws.test.matchers.Base64Matcher.toBase64;
import static fr.gouv.stopc.robertserver.ws.test.matchers.JwtMatcher.isJwtSignedBy;
import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.tuple;
import static org.assertj.core.api.HamcrestCondition.matching;
import static org.hamcrest.Matchers.*;
import static org.springframework.http.HttpStatus.*;

@IntegrationTest
class ReportControllerTest {

    @Test
    void can_report() {
        given()
                .contentType(JSON)
                .body(
                        ReportBatchRequestVo.builder()
                                .token("validZ")
                                .contacts(
                                        List.of(
                                                ContactVo.builder()
                                                        .ebid(toBase64("fake ebid A"))
                                                        .ecc(toBase64("fake ecc"))
                                                        .ids(
                                                                List.of(
                                                                        HelloMessageDetailVo.builder()
                                                                                .rssiCalibrated(0)
                                                                                .timeCollectedOnDevice(1L)
                                                                                .timeFromHelloMessage(2)
                                                                                .mac(toBase64("fake mac A1"))
                                                                                .build()
                                                                )
                                                        )
                                                        .build(),
                                                ContactVo.builder()
                                                        .ebid(toBase64("fake ebid B"))
                                                        .ecc(toBase64("fake ecc"))
                                                        .ids(
                                                                List.of(
                                                                        HelloMessageDetailVo.builder()
                                                                                .rssiCalibrated(10)
                                                                                .timeCollectedOnDevice(11L)
                                                                                .timeFromHelloMessage(12)
                                                                                .mac(toBase64("fake mac B1"))
                                                                                .build(),
                                                                        HelloMessageDetailVo.builder()
                                                                                .rssiCalibrated(20)
                                                                                .timeCollectedOnDevice(21L)
                                                                                .timeFromHelloMessage(22)
                                                                                .mac(toBase64("fake mac B2"))
                                                                                .build()
                                                                )
                                                        )
                                                        .build()
                                        )
                                )
                                .build()
                )

                .when()
                .post("/api/v6/report")

                .then()
                .statusCode(OK.value())
                .body("success", equalTo(true))
                .body("message", equalTo("Successful operation"))
                .body(
                        "reportValidationToken", isJwtSignedBy(JWT_KEYS)
                                .withClaim(
                                        "iat", allOf(
                                                greaterThan((int) Instant.now().minusSeconds(60).getEpochSecond()),
                                                lessThanOrEqualTo((int) Instant.now().getEpochSecond())
                                        )
                                )
                                .withClaim(
                                        "exp", allOf(
                                                greaterThan(
                                                        (int) ZonedDateTime.now().plusMinutes(4).toInstant()
                                                                .getEpochSecond()
                                                ),
                                                lessThan(
                                                        (int) ZonedDateTime.now().plusMinutes(6).toInstant()
                                                                .getEpochSecond()
                                                )
                                        )
                                )
                );

        assertThatContactsToProcess()
                .flatExtracting(MongodbManager::helloMessages)
                .extracting("ebid", "ecc", "time", "mac", "rssi", "receptionTime")
                .containsExactlyInAnyOrder(
                        tuple("fake ebid A", "fake ecc", 2, "fake mac A1", 0, 1L),
                        tuple("fake ebid B", "fake ecc", 12, "fake mac B1", 10, 11L),
                        tuple("fake ebid B", "fake ecc", 22, "fake mac B2", 20, 21L)
                );
    }

    @Test
    void can_report_one_large_contact_with_100k_hello_messages() {
        final var manyHelloMessages = IntStream.rangeClosed(1, 100_000)
                .mapToObj(
                        i -> HelloMessageDetailVo.builder()
                                .rssiCalibrated(10)
                                .timeCollectedOnDevice((long) i)
                                .timeFromHelloMessage(i + 1)
                                .mac(toBase64("fake mac" + i))
                                .build()
                )
                .collect(toList());
        final var largeContact = ContactVo.builder()
                .ebid(toBase64("fake ebid"))
                .ecc(toBase64("fake ecc"))
                .ids(manyHelloMessages)
                .build();

        given()
                .contentType(JSON)
                .body(
                        ReportBatchRequestVo.builder()
                                .token("validZ")
                                .contacts(List.of(largeContact))
                                .build()
                )

                .when()
                .post("/api/v6/report")

                .then()
                .statusCode(OK.value())
                .body("success", equalTo(true))
                .body("message", equalTo("Successful operation"))
                .body(
                        "reportValidationToken", isJwtSignedBy(JWT_KEYS)
                                .withClaim(
                                        "iat", allOf(
                                                greaterThan((int) Instant.now().minusSeconds(60).getEpochSecond()),
                                                lessThanOrEqualTo((int) Instant.now().getEpochSecond())
                                        )
                                )
                                .withClaim(
                                        "exp", allOf(
                                                greaterThan(
                                                        (int) ZonedDateTime.now().plusMinutes(4).toInstant()
                                                                .getEpochSecond()
                                                ),
                                                lessThan(
                                                        (int) ZonedDateTime.now().plusMinutes(6).toInstant()
                                                                .getEpochSecond()
                                                )
                                        )
                                )
                );

        assertThatContactsToProcess()
                .hasSize(1)
                .flatExtracting(Contact::getMessageDetails)
                .hasSize(100_000);
    }

    @Test
    void can_report_10k_contacts_with_10_hello_messages_each() {
        final var manyContacts = IntStream.rangeClosed(1, 10_000)
                .mapToObj(
                        i -> ContactVo.builder()
                                .ebid(toBase64("fake ebid " + i))
                                .ecc(toBase64("fake ecc"))
                                .ids(
                                        IntStream.rangeClosed(1, 10)
                                                .mapToObj(
                                                        j -> HelloMessageDetailVo.builder()
                                                                .rssiCalibrated(10)
                                                                .timeCollectedOnDevice((long) j)
                                                                .timeFromHelloMessage(j + 1)
                                                                .mac(toBase64(format("fake mac %d%d", i, j)))
                                                                .build()
                                                )
                                                .collect(toList())
                                )
                                .build()
                )
                .collect(toList());

        given()
                .contentType(JSON)
                .body(
                        ReportBatchRequestVo.builder()
                                .token("validZ")
                                .contacts(manyContacts)
                                .build()
                )

                .when()
                .post("/api/v6/report")

                .then()
                .statusCode(OK.value())
                .body("success", equalTo(true))
                .body("message", equalTo("Successful operation"))
                .body(
                        "reportValidationToken", isJwtSignedBy(JWT_KEYS)
                                .withClaim(
                                        "iat", allOf(
                                                greaterThan((int) Instant.now().minusSeconds(60).getEpochSecond()),
                                                lessThanOrEqualTo((int) Instant.now().getEpochSecond())
                                        )
                                )
                                .withClaim(
                                        "exp", allOf(
                                                greaterThan(
                                                        (int) ZonedDateTime.now().plusMinutes(4).toInstant()
                                                                .getEpochSecond()
                                                ),
                                                lessThan(
                                                        (int) ZonedDateTime.now().plusMinutes(6).toInstant()
                                                                .getEpochSecond()
                                                )
                                        )
                                )
                );

        assertThatContactsToProcess()
                .extracting(Contact::getMessageDetails)
                .areExactly(10_000, matching(hasSize(10)));
    }

    @Test
    void can_report_empty_contact_list() {
        given()
                .contentType(JSON)
                .body(
                        ReportBatchRequestVo.builder()
                                .token("validZ")
                                .contacts(emptyList())
                                .build()
                )

                .when()
                .post("/api/v6/report")

                .then()
                .statusCode(OK.value())
                .body("success", equalTo(true))
                .body("message", equalTo("Successful operation"))
                .body(
                        "reportValidationToken", isJwtSignedBy(JWT_KEYS)
                                .withClaim(
                                        "iat", allOf(
                                                greaterThan((int) Instant.now().minusSeconds(60).getEpochSecond()),
                                                lessThanOrEqualTo((int) Instant.now().getEpochSecond())
                                        )
                                )
                                .withClaim(
                                        "exp", allOf(
                                                greaterThan(
                                                        (int) ZonedDateTime.now().plusMinutes(4).toInstant()
                                                                .getEpochSecond()
                                                ),
                                                lessThan(
                                                        (int) ZonedDateTime.now().plusMinutes(6).toInstant()
                                                                .getEpochSecond()
                                                )
                                        )
                                )
                );

        assertThatContactsToProcess()
                .isEmpty();
    }

    @Test
    void cant_report_null_contact_list() {
        given()
                .contentType(JSON)
                .body(
                        ReportBatchRequestVo.builder()
                                .token("validZ")
                                .contacts(null)
                                .build()
                )

                .when()
                .post("/api/v6/report")

                .then()
                .statusCode(BAD_REQUEST.value())
                .body(emptyString());

        verifyNoInteractionsWithSubmissionCodeServer();
    }

    @Test
    void cant_report_without_token() {
        given()
                .contentType(JSON)
                .body(
                        ReportBatchRequestVo.builder()
                                .token(null)
                                .contacts(emptyList())
                                .build()
                )

                .when()
                .post("/api/v6/report")

                .then()
                .statusCode(BAD_REQUEST.value())
                .body("message", equalTo("Invalid data"));

        verifyNoInteractionsWithSubmissionCodeServer();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "err",
            "short0",
            "test00test00",
            "long0000-0000-0000-0000-000000000000"
    })
    void cant_report_with_invalid_token(String invalidToken) {
        given()
                .contentType(JSON)
                .body(
                        ReportBatchRequestVo.builder()
                                .token(invalidToken)
                                .contacts(emptyList())
                                .build()
                )

                .when()
                .post("/api/v6/report")

                .then()
                .statusCode(UNAUTHORIZED.value())
                .body("message", equalTo("Unrecognized token of length: " + invalidToken.length()));

        assertThatWarnLogs()
                .containsOnlyOnce("Unrecognized token of length: " + invalidToken.length());
    }
}
