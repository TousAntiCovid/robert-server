package e2e.steps;

import e2e.appmobile.AppMobile;
import e2e.captcha.CaptchaCreationRequest;
import e2e.config.ApplicationProperties;
import e2e.context.ScenarioContext;
import e2e.external.crypto.exception.RobertServerCryptoException;
import e2e.robert.ws.rest.PushInfoVo;
import e2e.robert.ws.rest.RegisterSuccessResponse;
import e2e.robert.ws.rest.RegisterVo;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;

import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@Slf4j
@AllArgsConstructor
public class RobertClientSteps {

    public static final String CAPTCHA_BYPASS_SOLUTION = "IEDX";

    private final ApplicationProperties applicationProperties;

    private final ScenarioContext scenarioContext;

    private RequestSpecification givenRobertBaseUri() {
        return given()
                .baseUri(this.applicationProperties.getWsRestBaseUrl())
                .contentType(JSON);
    }

    @Given("application robert ws rest is ready")
    public void applicationRobertIsReady() {
        givenRobertBaseUri()
                .get("/actuator/health")
                .then()
                .statusCode(200)
                .body("status", equalTo("UP"));
    }

    @Given("{word} has the application TAC")
    public void createAppMobile(String userName) {
        this.scenarioContext.getOrCreateApplication(userName);
    }

    @Given("{word} resolve the captcha challenge")
    public void resolveCaptchaChallenge(String userName) {
        givenRobertBaseUri()
                .body(
                        CaptchaCreationRequest.builder()
                                .locale("fr")
                                .type("IMAGE")
                                .build()
                )
                .when()
                .post("/api/v6/captcha")
                .then()
                .statusCode(200)
                .body("captchaId", notNullValue());

        AppMobile appMobile = this.scenarioContext.getOrCreateApplication(userName);

        // We generate a fake Captcha id which we will use to differentiate mobile apps
        appMobile.setCaptchaId(RandomStringUtils.random(7, true, false));

        givenRobertBaseUri()
                .when()
                .get("/api/v6/captcha/{captchaId}/image", appMobile.getCaptchaId())
                .then()
                .statusCode(200);

        // Simulate visual resolution
        appMobile.setCaptchaSolution(CAPTCHA_BYPASS_SOLUTION);
    }

    @Then("{word} is registered on TAC")
    public void registerOnTAC(String userName)
            throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, RobertServerCryptoException {
        AppMobile appMobile = this.scenarioContext.getOrCreateApplication(userName);
        appMobile.setRobertPublicKey(this.applicationProperties.getCryptoPublicKey());
        appMobile.generateApplicationMobileEngineData();

        RegisterVo registerVo = RegisterVo.builder()
                .captcha(appMobile.getCaptchaSolution())
                .captchaId(appMobile.getCaptchaId())
                .clientPublicECDHKey(appMobile.getPublicKey())
                .pushInfo(
                        PushInfoVo.builder()
                                .token("string")
                                .locale("fr")
                                .timezone("Europe/Paris")
                                .build()
                )
                .build();

        RegisterSuccessResponse registerSuccessResponse = given()
                .contentType(ContentType.JSON)
                .body(registerVo)
                .when()
                .post(this.applicationProperties.getWsRestBaseUrl().concat("/api/v5/register"))
                .then()
                .statusCode(201)
                .extract()
                .as(RegisterSuccessResponse.class);

        assertDoesNotThrow(() -> appMobile.decryptRegisterResponse(registerSuccessResponse));
    }

}
