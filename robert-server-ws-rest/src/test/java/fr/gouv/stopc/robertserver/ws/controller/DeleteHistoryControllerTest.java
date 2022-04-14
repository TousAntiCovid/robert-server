package fr.gouv.stopc.robertserver.ws.controller;

import fr.gouv.stopc.robert.server.common.service.RobertClock;
import fr.gouv.stopc.robertserver.database.model.EpochExposition;
import fr.gouv.stopc.robertserver.ws.test.IntegrationTest;
import fr.gouv.stopc.robertserver.ws.vo.DeleteHistoryRequestVo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static fr.gouv.stopc.robertserver.ws.test.MongodbManager.assertThatRegistrationForUser;
import static fr.gouv.stopc.robertserver.ws.test.MongodbManager.givenRegistrationExistsForUser;
import static fr.gouv.stopc.robertserver.ws.test.matchers.Base64Matcher.toBase64;
import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.assertj.core.api.HamcrestCondition.matching;
import static org.hamcrest.Matchers.*;
import static org.springframework.http.HttpStatus.OK;

@IntegrationTest
class DeleteHistoryControllerTest {

    @Autowired
    private RobertClock clock;

    @Test
    void can_unregister() {
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
                                .epochId(clock.now().asEpochId())
                                .time(toBase64(clock.now().asTime32()))
                                .mac(toBase64("fake mac having a length of exactly 44 characters", 32))
                                .build()
                )

                .when()
                .post("/api/v6/deleteExposureHistory")

                .then()
                .statusCode(OK.value())
                .body("success", equalTo(true))
                .body("message", nullValue());

        assertThatRegistrationForUser("user___1")
                .as("exposed epochs for 'user___1' is empty")
                .is(matching(hasProperty("exposedEpochs", emptyIterable())));
    }
}
