package fr.gouv.stopc.robertserver.ws.controller;

import fr.gouv.stopc.robert.server.common.service.RobertClock;
import fr.gouv.stopc.robertserver.database.model.Registration;
import fr.gouv.stopc.robertserver.ws.test.AuthDataManager.AuthRequestData;
import fr.gouv.stopc.robertserver.ws.test.IntegrationTest;
import fr.gouv.stopc.robertserver.ws.vo.UnregisterRequestVo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import static fr.gouv.stopc.robertserver.ws.test.GrpcMockManager.givenCryptoServerRaiseError430ForEbid;
import static fr.gouv.stopc.robertserver.ws.test.GrpcMockManager.givenCryptoServerRaiseErrorForMacStartingWith;
import static fr.gouv.stopc.robertserver.ws.test.MockServerManager.verifyNoInteractionsWithPushNotifServer;
import static fr.gouv.stopc.robertserver.ws.test.MockServerManager.verifyPushNotifServerReceivedUnregisterForToken;
import static fr.gouv.stopc.robertserver.ws.test.MongodbManager.assertThatRegistrations;
import static fr.gouv.stopc.robertserver.ws.test.MongodbManager.givenRegistrationExistsForUser;
import static fr.gouv.stopc.robertserver.ws.test.matchers.Base64Matcher.toBase64;
import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.Matchers.*;
import static org.springframework.http.HttpStatus.*;

@IntegrationTest
class UnregisterControllerTest {

    @Autowired
    private RobertClock clock;

    @ParameterizedTest
    @MethodSource("fr.gouv.stopc.robertserver.ws.test.AuthDataManager#acceptableAuthParameters")
    void can_unregister(AuthRequestData auth) {
        givenRegistrationExistsForUser("user___1");

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
                .statusCode(OK.value())
                .body("success", equalTo(true))
                .body("message", nullValue());

        verifyNoInteractionsWithPushNotifServer();

        assertThatRegistrations()
                .isEmpty();
    }

    @ParameterizedTest
    @MethodSource("fr.gouv.stopc.robertserver.ws.test.AuthDataManager#acceptableAuthParameters")
    void can_unregister_apple_device(AuthRequestData auth) {
        givenRegistrationExistsForUser("user___1");

        given()
                .contentType(JSON)
                .body(
                        UnregisterRequestVo.builder()
                                .ebid(toBase64("user___1"))
                                .epochId(auth.epochId())
                                .time(auth.base64Time32())
                                .mac(auth.base64Mac())
                                .pushToken("valid-token-user___1")
                                .build()
                )

                .when()
                .post("/api/v6/unregister")

                .then()
                .statusCode(OK.value())
                .body("success", equalTo(true))
                .body("message", nullValue());

        verifyPushNotifServerReceivedUnregisterForToken("valid-token-user___1");

        assertThatRegistrations()
                .isEmpty();
    }

    @ParameterizedTest
    @MethodSource("fr.gouv.stopc.robertserver.ws.test.AuthDataManager#unacceptableAuthParameters")
    void cant_unregister_with_too_much_time_drift(AuthRequestData auth) {
        givenRegistrationExistsForUser("user___1");

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

        verifyNoInteractionsWithPushNotifServer();

        assertThatRegistrations()
                .extracting(Registration::getPermanentIdentifier)
                .containsExactly("idA for user___1".getBytes());
    }

    @Test
    void cant_unregister_for_a_key_unknown_by_crypto_server() {
        givenCryptoServerRaiseError430ForEbid("miss-key");

        given()
                .contentType(JSON)
                .body(
                        UnregisterRequestVo.builder()
                                .ebid(toBase64("miss-key"))
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

    @Test
    void bad_request_on_mac_validation_error_raised_by_crypto_server() {
        givenRegistrationExistsForUser("user___1");
        givenCryptoServerRaiseErrorForMacStartingWith("invalid");

        given()
                .contentType(JSON)
                .body(
                        UnregisterRequestVo.builder()
                                .ebid(toBase64("user___1"))
                                .epochId(clock.now().asEpochId())
                                .time(toBase64(clock.now().asTime32()))
                                .mac(toBase64("invalid fake mac having a length of exactly 44 characters", 32))
                                .build()
                )

                .when()
                .post("/api/v6/unregister")

                .then()
                .statusCode(BAD_REQUEST.value())
                .body(emptyString());

        assertThatRegistrations()
                .extracting(Registration::getPermanentIdentifier)
                .containsExactly("idA for user___1".getBytes());
    }

    @Test
    void not_found_for_an_unexisting_registration() {
        given()
                .contentType(JSON)
                .body(
                        UnregisterRequestVo.builder()
                                .ebid(toBase64("no-regis"))
                                .epochId(clock.now().asEpochId())
                                .time(toBase64(clock.now().asTime32()))
                                .mac(toBase64("fake mac having a length of exactly 44 characters", 32))
                                .build()
                )

                .when()
                .post("/api/v6/unregister")

                .then()
                .statusCode(NOT_FOUND.value())
                .body(emptyString());
    }

}
