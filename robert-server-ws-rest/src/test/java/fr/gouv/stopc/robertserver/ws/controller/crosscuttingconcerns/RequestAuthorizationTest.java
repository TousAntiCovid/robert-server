package fr.gouv.stopc.robertserver.ws.controller.crosscuttingconcerns;

import fr.gouv.stopc.robert.server.common.service.RobertClock;
import fr.gouv.stopc.robertserver.ws.test.IntegrationTest;
import fr.gouv.stopc.robertserver.ws.vo.StatusVo;
import fr.gouv.stopc.robertserver.ws.vo.UnregisterRequestVo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.stream.Stream;

import static fr.gouv.stopc.robertserver.ws.test.matchers.Base64Matcher.toBase64;
import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

@IntegrationTest
@DisplayName("ebid should be 12 base64 chars, time 8 base64 chars and mac 44 base64 chars")
class RequestAuthorizationTest {

    @Autowired
    private RobertClock clock;

    static Stream<Arguments> invalid_auth_params() {
        return Stream.of(
                Arguments.of("ebid is null", null, 8, 44),
                Arguments.of("ebid is too short", 11, 8, 44),
                Arguments.of("ebid is too long", 13, 8, 44),
                Arguments.of("time is null", 12, null, 44),
                Arguments.of("time is too short", 12, 7, 44),
                Arguments.of("time is too long", 12, 9, 44),
                Arguments.of("mac is null", 12, 8, null),
                Arguments.of("mac is too short", 12, 8, 43),
                Arguments.of("mac is too long", 12, 8, 45)
        );
    }

    @ParameterizedTest(name = "when {0}")
    @MethodSource("invalid_auth_params")
    void bad_request_on_status_with_invalid_authentication(String name, Integer ebidBase64Length,
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
                .statusCode(BAD_REQUEST.value());
    }

    @ParameterizedTest(name = "when {0}")
    @MethodSource("invalid_auth_params")
    void bad_request_on_deleteExposureHistory_with_invalid_authentication(String name, Integer ebidBase64Length,
            Integer timeBase64Length, Integer macBase64Length) {
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
                .statusCode(BAD_REQUEST.value());
    }

    @ParameterizedTest(name = "when {0}")
    @MethodSource("invalid_auth_params")
    void bad_request_on_unregister_with_invalid_authentication(String name, Integer ebidBase64Length,
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
                .statusCode(BAD_REQUEST.value());
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
