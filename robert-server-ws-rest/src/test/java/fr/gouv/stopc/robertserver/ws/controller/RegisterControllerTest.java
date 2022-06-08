package fr.gouv.stopc.robertserver.ws.controller;

import fr.gouv.stopc.robertserver.ws.test.GrpcMockManager;
import fr.gouv.stopc.robertserver.ws.test.IntegrationTest;
import fr.gouv.stopc.robertserver.ws.vo.PushInfoVo;
import fr.gouv.stopc.robertserver.ws.vo.RegisterVo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static fr.gouv.stopc.robertserver.ws.test.GrpcMockManager.verifyNoInteractionsWithCryptoServer;
import static fr.gouv.stopc.robertserver.ws.test.MockServerManager.verifyNoInteractionsWithPushNotifServer;
import static fr.gouv.stopc.robertserver.ws.test.MockServerManager.verifyPushNotifServerReceivedRegisterForToken;
import static fr.gouv.stopc.robertserver.ws.test.MongodbManager.givenMongodbIsOffline;
import static fr.gouv.stopc.robertserver.ws.test.RestAssuredManager.equalToServiceStartNtpTimeStamp;
import static fr.gouv.stopc.robertserver.ws.test.matchers.Base64Matcher.isBase64Encoded;
import static fr.gouv.stopc.robertserver.ws.test.matchers.Base64Matcher.toBase64;
import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.Matchers.*;
import static org.springframework.http.HttpStatus.*;

@IntegrationTest
class RegisterControllerTest {

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

    @Test
    void cant_register_with_wrong_captcha_answer() {
        given()
                .contentType(JSON)
                .body(
                        RegisterVo.builder()
                                .captcha("wrong challenge answer")
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
                .statusCode(UNAUTHORIZED.value())
                .body(emptyString());

        verifyNoInteractionsWithCryptoServer();
        verifyNoInteractionsWithPushNotifServer();
    }

    @ParameterizedTest
    @CsvSource(nullValues = { "NULL" }, value = {
            "captcha-id,NULL",
            "NULL,valid challenge answer",
            ",valid challenge answer",
            "captcha-id,"
    })
    void cant_register_with_invalid_captcha_details(String captchaId, String captchaChallengeResponse) {
        given()
                .contentType(JSON)
                .body(
                        RegisterVo.builder()
                                .captcha(captchaChallengeResponse)
                                .captchaId(captchaId)
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
                .statusCode(BAD_REQUEST.value())
                .body("message", equalTo("Invalid data"));

        verifyNoInteractionsWithCryptoServer();
        verifyNoInteractionsWithPushNotifServer();
    }

    @Test
    void cant_register_when_database_is_offline() {
        givenMongodbIsOffline();

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
                .statusCode(INTERNAL_SERVER_ERROR.value())
                .body("message", equalTo("An error occured"));

        verifyNoInteractionsWithPushNotifServer();
    }

    @Test
    void cant_register_when_crypto_server_is_offline() {
        GrpcMockManager.givenCryptoServerIsOffline();

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
                .statusCode(INTERNAL_SERVER_ERROR.value())
                .body("message", equalTo("An error occured"));

        verifyNoInteractionsWithPushNotifServer();
    }
}
