package fr.gouv.stopc.robertserver.ws.controller;

import fr.gouv.stopc.robertserver.ws.test.IntegrationTest;
import fr.gouv.stopc.robertserver.ws.vo.PushInfoVo;
import fr.gouv.stopc.robertserver.ws.vo.RegisterVo;
import org.junit.jupiter.api.Test;

import static fr.gouv.stopc.robertserver.ws.test.MockServerManager.verifyPushNotifServerReceivedRegisterForToken;
import static fr.gouv.stopc.robertserver.ws.test.RestAssuredManager.equalToServiceStartNtpTimeStamp;
import static fr.gouv.stopc.robertserver.ws.test.matchers.Base64Matcher.isBase64Encoded;
import static fr.gouv.stopc.robertserver.ws.test.matchers.Base64Matcher.toBase64;
import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.http.HttpStatus.CREATED;

@IntegrationTest
class RegisterControllerSuccessTest {

    @Test
    void can_register() {
        given()
                .contentType(JSON)
                .body(
                        RegisterVo.builder()
                                .captcha("valid challenge answer")
                                .captchaId("captcha-id")
                                .clientPublicECDHKey(toBase64("fake public key"))
                                .build()
                )

                .when()
                .post("/api/v6/register")

                .then()
                .statusCode(CREATED.value())
                .body("timeStart", equalToServiceStartNtpTimeStamp())
                .body("message", nullValue())
                .body("config.size()", equalTo(0))
                .body("tuples", isBase64Encoded(equalTo("fake encrypted tuples for fake public key")));
    }

    @Test
    void can_register_apple_device() {
        given()
                .contentType(JSON)
                .body(
                        RegisterVo.builder()
                                .captcha("valid challenge answer")
                                .captchaId("captcha-id")
                                .clientPublicECDHKey(toBase64("fake public key"))
                                .pushInfo(
                                        PushInfoVo.builder()
                                                .token("valid-device-id")
                                                .locale("fr_FR")
                                                .timezone("Europe/Paris")
                                                .build()
                                )
                                .build()
                )

                .when()
                .post("/api/v6/register")

                .then()
                .statusCode(CREATED.value())
                .body("timeStart", equalToServiceStartNtpTimeStamp())
                .body("message", nullValue())
                .body("config.size()", equalTo(0))
                .body("tuples", isBase64Encoded(equalTo("fake encrypted tuples for fake public key")));

        verifyPushNotifServerReceivedRegisterForToken(
                PushInfoVo.builder()
                        .token("valid-device-id")
                        .locale("fr_FR")
                        .timezone("Europe/Paris")
                        .build()
        );
    }

}
