package fr.gouv.stopc.robertserver.ws.controller.crosscuttingconcerns;

import fr.gouv.stopc.robertserver.common.RobertClock;
import fr.gouv.stopc.robertserver.ws.test.IntegrationTest;
import fr.gouv.stopc.robertserver.ws.vo.DeleteHistoryRequestVo;
import fr.gouv.stopc.robertserver.ws.vo.StatusVo;
import fr.gouv.stopc.robertserver.ws.vo.UnregisterRequestVo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.stream.Stream;

import static fr.gouv.stopc.robertserver.ws.test.LogbackManager.assertThatErrorLogs;
import static fr.gouv.stopc.robertserver.ws.test.matchers.Base64Matcher.toBase64;
import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static java.lang.String.format;
import static org.assertj.core.api.HamcrestCondition.matching;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

@IntegrationTest
@DisplayName("ebid should be 12 base64 chars, time 8 base64 chars and mac 44 base64 chars")
class AuthenticationParamLenghtValidationTest {

    @Autowired
    private RobertClock clock;

    static Stream<Arguments> invalid_auth_params() {
        return Stream.of(
                arguments("ebid", "is null", null, 8, 44),
                arguments("ebid", "is too short", 11, 8, 44),
                arguments("ebid", "is too long", 13, 8, 44),
                arguments("time", "is null", 12, null, 44),
                arguments("time", "is too short", 12, 7, 44),
                arguments("time", "is too long", 12, 9, 44),
                arguments("mac", "is null", 12, 8, null),
                arguments("mac", "is too short", 12, 8, 43),
                arguments("mac", "is too long", 12, 8, 45)
        );
    }

    @ParameterizedTest(name = "when {0} {1}")
    @MethodSource("invalid_auth_params")
    void bad_request_on_status_with_invalid_authentication(String field, String cause,
            Integer ebidBase64Length, Integer timeBase64Length, Integer macBase64Length) {
        given()
                .contentType(JSON)
                .body(
                        StatusVo.builder()
                                .ebid(generateEbidOfLength(ebidBase64Length))
                                .epochId(clock.now().asEpochId())
                                .time(generateTimeOfLength(timeBase64Length))
                                .mac(generateMacOfLength(macBase64Length))
                                .build()
                )

                .when()
                .post("/api/v6/deleteExposureHistory")

                .then()
                .statusCode(BAD_REQUEST.value())
                .body("message", equalTo("Invalid data"));

        assertThatErrorLogs()
                .areExactly(
                        1,
                        matching(
                                startsWith(
                                        format(
                                                "Validation failed for argument [0] in public org.springframework.http.ResponseEntity<fr.gouv.stopc.robertserver.ws.dto.DeleteHistoryResponseDto> fr.gouv.stopc.robertserver.ws.controller.impl.DeleteHistoryControllerImpl.deleteHistory(fr.gouv.stopc.robertserver.ws.vo.DeleteHistoryRequestVo): [Field error in object 'deleteHistoryRequestVo' on field '%s':",
                                                field
                                        )
                                )
                        )
                );
    }

    @ParameterizedTest(name = "when {0} {1}")
    @MethodSource("invalid_auth_params")
    void bad_request_on_deleteExposureHistory_with_invalid_authentication(String field, String cause,
            Integer ebidBase64Length,
            Integer timeBase64Length, Integer macBase64Length) {
        given()
                .contentType(JSON)
                .body(
                        DeleteHistoryRequestVo.builder()
                                .ebid(generateEbidOfLength(ebidBase64Length))
                                .epochId(clock.now().asEpochId())
                                .time(generateTimeOfLength(timeBase64Length))
                                .mac(generateMacOfLength(macBase64Length))
                                .build()
                )

                .when()
                .post("/api/v6/deleteExposureHistory")

                .then()
                .statusCode(BAD_REQUEST.value())
                .body("message", equalTo("Invalid data"));

        assertThatErrorLogs()
                .areExactly(
                        1,
                        matching(
                                startsWith(
                                        format(
                                                "Validation failed for argument [0] in public org.springframework.http.ResponseEntity<fr.gouv.stopc.robertserver.ws.dto.DeleteHistoryResponseDto> fr.gouv.stopc.robertserver.ws.controller.impl.DeleteHistoryControllerImpl.deleteHistory(fr.gouv.stopc.robertserver.ws.vo.DeleteHistoryRequestVo): [Field error in object 'deleteHistoryRequestVo' on field '%s'",
                                                field
                                        )
                                )
                        )
                );
    }

    @ParameterizedTest(name = "when {0} {1}")
    @MethodSource("invalid_auth_params")
    void bad_request_on_unregister_with_invalid_authentication(String field, String cause, Integer ebidBase64Length,
            Integer timeBase64Length, Integer macBase64Length) {
        given()
                .contentType(JSON)
                .body(
                        UnregisterRequestVo.builder()
                                .ebid(generateEbidOfLength(ebidBase64Length))
                                .epochId(clock.now().asEpochId())
                                .time(generateTimeOfLength(timeBase64Length))
                                .mac(generateMacOfLength(macBase64Length))
                                .build()
                )

                .when()
                .post("/api/v6/unregister")

                .then()
                .statusCode(BAD_REQUEST.value())
                .body("message", equalTo("Invalid data"));

        assertThatErrorLogs()
                .areExactly(
                        1,
                        matching(
                                startsWith(
                                        format(
                                                "Validation failed for argument [0] in public org.springframework.http.ResponseEntity<fr.gouv.stopc.robertserver.ws.dto.UnregisterResponseDto> fr.gouv.stopc.robertserver.ws.controller.impl.UnregisterControllerImpl.unregister(fr.gouv.stopc.robertserver.ws.vo.UnregisterRequestVo): [Field error in object 'unregisterRequestVo' on field '%s':",
                                                field
                                        )
                                )
                        )
                );
    }

    private String generateEbidOfLength(Integer length) {
        if (length == null) {
            return null;
        }
        return toBase64("user___1___").substring(0, length);
    }

    private String generateTimeOfLength(Integer length) {
        if (length == null) {
            return null;
        }
        return toBase64(clock.now().asTime32()).concat("___").substring(0, length);
    }

    private String generateMacOfLength(Integer length) {
        if (length == null) {
            return null;
        }
        return toBase64("fake mac having a long length that will be truncated")
                .substring(0, length);
    }
}
