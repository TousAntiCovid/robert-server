package fr.gouv.stopc.robertserver.ws.controller;

import fr.gouv.stopc.robert.server.common.service.RobertClock;
import fr.gouv.stopc.robertserver.database.model.EpochExposition;
import fr.gouv.stopc.robertserver.ws.test.AuthDataManager.AuthRequestData;
import fr.gouv.stopc.robertserver.ws.test.IntegrationTest;
import fr.gouv.stopc.robertserver.ws.vo.DeleteHistoryRequestVo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static fr.gouv.stopc.robertserver.ws.test.GrpcMockManager.givenCryptoServerRaiseError430ForEbid;
import static fr.gouv.stopc.robertserver.ws.test.GrpcMockManager.givenCryptoServerRaiseErrorForMacStartingWith;
import static fr.gouv.stopc.robertserver.ws.test.MongodbManager.assertThatRegistrationForUser;
import static fr.gouv.stopc.robertserver.ws.test.MongodbManager.givenRegistrationExistsForUser;
import static fr.gouv.stopc.robertserver.ws.test.matchers.Base64Matcher.toBase64;
import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.HamcrestCondition.matching;
import static org.hamcrest.Matchers.*;
import static org.springframework.http.HttpStatus.*;

@IntegrationTest
class DeleteHistoryControllerTest {

    @Autowired
    private RobertClock clock;

    @ParameterizedTest
    @MethodSource("fr.gouv.stopc.robertserver.ws.test.AuthDataManager#acceptableAuthParameters")
    void can_delete_history(AuthRequestData auth) {
        givenRegistrationExistsForUser(
                "user___1", r -> r.exposedEpochs(
                        List.of(
                                EpochExposition.builder()
                                        .epochId(clock.now().asEpochId())
                                        .expositionScores(List.of(1.0, 1.2))
                                        .build()
                        )
                )
        );

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
                .statusCode(OK.value())
                .body("success", equalTo(true))
                .body("message", nullValue());

        assertThatRegistrationForUser("idA for user___1")
                .as("exposed epochs for 'user___1' is empty")
                .is(matching(hasProperty("exposedEpochs", emptyIterable())));
    }

    @ParameterizedTest
    @MethodSource("fr.gouv.stopc.robertserver.ws.test.AuthDataManager#acceptableAuthParameters")
    void can_delete_empty_history(AuthRequestData auth) {
        givenRegistrationExistsForUser("user___1", r -> r.exposedEpochs(emptyList()));

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
                .statusCode(OK.value())
                .body("success", equalTo(true))
                .body("message", nullValue());

        assertThatRegistrationForUser("idA for user___1")
                .as("exposed epochs for 'user___1' is empty")
                .is(matching(hasProperty("exposedEpochs", emptyIterable())));
    }

    @ParameterizedTest
    @MethodSource("fr.gouv.stopc.robertserver.ws.test.AuthDataManager#unacceptableAuthParameters")
    void cant_delete_history_with_too_much_time_drift(AuthRequestData auth) {
        givenRegistrationExistsForUser("user___1");

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
    void cant_delete_history_for_a_key_unknown_by_crypto_server() {
        givenCryptoServerRaiseError430ForEbid("miss-key");

        given()
                .contentType(JSON)
                .body(
                        DeleteHistoryRequestVo.builder()
                                .ebid(toBase64("miss-key"))
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
        ;
    }

    @Test
    void bad_request_on_mac_validation_error_raised_by_crypto_server() {
        givenRegistrationExistsForUser("user___1");
        givenCryptoServerRaiseErrorForMacStartingWith("invalid");

        given()
                .contentType(JSON)
                .body(
                        DeleteHistoryRequestVo.builder()
                                .ebid(toBase64("user___1"))
                                .epochId(clock.now().asEpochId())
                                .time(toBase64(clock.now().asTime32()))
                                .mac(toBase64("invalid fake mac having a length of exactly 44 characters", 32))
                                .build()
                )

                .when()
                .post("/api/v6/deleteExposureHistory")

                .then()
                .statusCode(BAD_REQUEST.value())
                .body(emptyString());
        ;
    }

    @Test
    void not_found_for_an_unexisting_registration() {
        given()
                .contentType(JSON)
                .body(
                        DeleteHistoryRequestVo.builder()
                                .ebid(toBase64("no-regis"))
                                .epochId(clock.now().asEpochId())
                                .time(toBase64(clock.now().asTime32()))
                                .mac(toBase64("fake mac having a length of exactly 44 characters", 32))
                                .build()
                )

                .when()
                .post("/api/v6/deleteExposureHistory")

                .then()
                .statusCode(NOT_FOUND.value())
                .body(emptyString());
        ;
    }
}
