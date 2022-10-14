package fr.gouv.stopc.robertserver.ws.controller.crosscuttingconcerns;

import fr.gouv.stopc.robertserver.common.RobertClock;
import fr.gouv.stopc.robertserver.ws.test.AuthDataManager.AuthRequestData;
import fr.gouv.stopc.robertserver.ws.test.IntegrationTest;
import fr.gouv.stopc.robertserver.ws.vo.DeleteHistoryRequestVo;
import fr.gouv.stopc.robertserver.ws.vo.StatusVo;
import fr.gouv.stopc.robertserver.ws.vo.UnregisterRequestVo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import static fr.gouv.stopc.robertserver.ws.test.GrpcMockManager.givenCryptoServerRaiseMissingIdentityForEbid;
import static fr.gouv.stopc.robertserver.ws.test.MongodbManager.givenRegistrationExistsForUser;
import static fr.gouv.stopc.robertserver.ws.test.matchers.Base64Matcher.toBase64;
import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.Matchers.*;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

@IntegrationTest
class AuthenticationErrorsTest {

    @Autowired
    private RobertClock clock;

    @Test
    void http_status_430_on_status_with_unknown_credentials() {
        givenRegistrationExistsForUser("user___1");
        givenCryptoServerRaiseMissingIdentityForEbid("user___1");

        given()
                .contentType(JSON)
                .body(
                        StatusVo.builder()
                                .ebid(toBase64("user___1"))
                                .epochId(clock.now().asEpochId())
                                .time(toBase64(clock.now().asTime32()))
                                .mac(toBase64("fake mac having a length of exactly 44 characters", 32))
                                .build()
                )

                .when()
                .post("/api/v6/status")

                .then()
                .statusCode(430)
                .body(emptyString());
    }

    @ParameterizedTest
    @MethodSource("fr.gouv.stopc.robertserver.ws.test.AuthDataManager#unacceptableAuthParameters")
    void bad_request_on_status_with_unknown_credentials_and_too_much_time_drift(AuthRequestData auth) {
        givenRegistrationExistsForUser("user___1");
        givenCryptoServerRaiseMissingIdentityForEbid("user___1");

        given()
                .contentType(JSON)
                .body(
                        StatusVo.builder()
                                .ebid(toBase64("user___1"))
                                .epochId(auth.epochId())
                                .time(auth.base64Time32())
                                .mac(auth.base64Mac())
                                .build()
                )

                .when()
                .post("/api/v6/status")

                .then()
                .statusCode(BAD_REQUEST.value())
                .body(emptyString());
    }

    @Test
    void http_status_430_on_deleteExposureHistory_with_unknown_credentials() {
        givenRegistrationExistsForUser("user___1");
        givenCryptoServerRaiseMissingIdentityForEbid("user___1");

        given()
                .contentType(JSON)
                .body(
                        DeleteHistoryRequestVo.builder()
                                .ebid(toBase64("user___1"))
                                .epochId(clock.now().asEpochId())
                                .time(toBase64(clock.now().asTime32()))
                                .mac(toBase64("fake mac having a length of exactly 44 characters", 32))
                                .build()
                )

                .when()
                .post("/api/v6/deleteExposureHistory")

                .then()
                .statusCode(430)
                .body(emptyString());
    }

    @ParameterizedTest
    @MethodSource("fr.gouv.stopc.robertserver.ws.test.AuthDataManager#unacceptableAuthParameters")
    void bad_request_on_deleteExposureHistory_with_unknown_credentials_and_too_much_time_drift(AuthRequestData auth) {
        givenRegistrationExistsForUser("user___1");
        givenCryptoServerRaiseMissingIdentityForEbid("user___1");

        given()
                .contentType(JSON)
                .body(
                        DeleteHistoryRequestVo.builder()
                                .ebid(toBase64("user___1"))
                                .epochId(auth.epochId())
                                .time(auth.base64Time32())
                                .mac(auth.base64Mac())
                                .build()
                )

                .when()
                .post("/api/v6/deleteExposureHistory")

                .then()
                .statusCode(BAD_REQUEST.value())
                .body(emptyString());
    }

    @Test
    void http_status_430_on_unregister_with_unknown_credentials() {
        givenRegistrationExistsForUser("user___1");
        givenCryptoServerRaiseMissingIdentityForEbid("user___1");

        given()
                .contentType(JSON)
                .body(
                        UnregisterRequestVo.builder()
                                .ebid(toBase64("user___1"))
                                .epochId(clock.now().asEpochId())
                                .time(toBase64(clock.now().asTime32()))
                                .mac(toBase64("fake mac having a length of exactly 44 characters", 32))
                                .build()
                )

                .when()
                .post("/api/v6/unregister")

                .then()
                .statusCode(430)
                .body(emptyString());
    }

    @ParameterizedTest
    @MethodSource("fr.gouv.stopc.robertserver.ws.test.AuthDataManager#unacceptableAuthParameters")
    void bad_request_on_unregister_with_unknown_credentials_and_too_much_time_drift(AuthRequestData auth) {
        givenRegistrationExistsForUser("user___1");
        givenCryptoServerRaiseMissingIdentityForEbid("user___1");

        given()
                .contentType(JSON)
                .body(
                        UnregisterRequestVo.builder()
                                .ebid(toBase64("user___1"))
                                .epochId(auth.epochId())
                                .time(auth.base64Time32())
                                .mac(auth.base64Mac())
                                .build()
                )

                .when()
                .post("/api/v6/unregister")

                .then()
                .statusCode(BAD_REQUEST.value())
                .body(emptyString());
    }
}
