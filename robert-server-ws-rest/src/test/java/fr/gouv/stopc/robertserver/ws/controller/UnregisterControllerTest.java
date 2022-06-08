package fr.gouv.stopc.robertserver.ws.controller;

import fr.gouv.stopc.robert.server.common.service.RobertClock;
import fr.gouv.stopc.robertserver.ws.test.IntegrationTest;
import fr.gouv.stopc.robertserver.ws.vo.UnregisterRequestVo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static fr.gouv.stopc.robertserver.ws.test.GrpcMockManager.givenCryptoServerRaiseError430ForEbid;
import static fr.gouv.stopc.robertserver.ws.test.GrpcMockManager.givenCryptoServerRaiseErrorForMacStartingWith;
import static fr.gouv.stopc.robertserver.ws.test.MongodbManager.assertThatRegistrations;
import static fr.gouv.stopc.robertserver.ws.test.MongodbManager.givenRegistrationExistsForUser;
import static fr.gouv.stopc.robertserver.ws.test.matchers.Base64Matcher.toBase64;
import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.http.HttpStatus.*;

@IntegrationTest
class UnregisterControllerTest {

    @Autowired
    private RobertClock clock;

    @Test
    void can_unregister() {
        givenRegistrationExistsForUser("user___1");

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
                .statusCode(OK.value())
                .body("success", equalTo(true))
                .body("message", nullValue());

        assertThatRegistrations()
                .isEmpty();
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
                .statusCode(430);
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
                .statusCode(BAD_REQUEST.value());
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
                .statusCode(NOT_FOUND.value());
    }

}
