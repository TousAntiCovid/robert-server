package e2e.steps;

import e2e.captcha.CaptchaCreationRequest;
import e2e.config.ApplicationProperties;
import e2e.context.ScenarioContext;
import e2e.context.User;
import e2e.external.crypto.exception.RobertServerCryptoException;
import e2e.phone.AppMobile;
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
    public void application_robert_is_ready() {
        givenRobertBaseUri()
                .get("/actuator/health")
                .then()
                .statusCode(200)
                .body("status", equalTo("UP"));
    }

    @Given("{word} has the application TAC")
    public void generate_user(String userName) {
        this.scenarioContext.getOrCreateUser(userName);
    }

    @Given("{word} resolve the captcha challenge")
    public void resolve_captcha_challenge(String userName)
            throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, RobertServerCryptoException {
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

        User user = this.scenarioContext.getOrCreateUser(userName);

        // We generate a fake Captcha id which will be used to differentiate mobile apps
        user.setAppMobile(new AppMobile(RandomStringUtils.random(7, true, false)));
        AppMobile appMobile = user.getAppMobile();

        givenRobertBaseUri()
                .when()
                .get("/api/v6/captcha/{captchaId}/image", appMobile.getCaptchaId())
                .then()
                .statusCode(200);

        // Simulate visual resolution
        appMobile.setCaptchaSolution(CAPTCHA_BYPASS_SOLUTION);
    }

    @Then("{word} is registered on TAC")
    public void register_user_on_TAC(String userName)
            throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, RobertServerCryptoException {
        AppMobile appMobile = this.scenarioContext.getOrCreateUser(userName).getAppMobile();
        appMobile.setRobertPublicKey(this.applicationProperties.getCryptoPublicKey());
        appMobile.generateUsefullData();

        RegisterVo registerVo = RegisterVo.builder()
                .captcha(appMobile.getCaptchaSolution())
                .captchaId(appMobile.getCaptchaId())
                .clientPublicECDHKey(appMobile.getPublicKey())
                .build();

        PushInfoVo pushInfo = PushInfoVo.builder()
                .token("string")
                .locale("fr")
                .timezone("Europe/Paris")
                .build();

        registerVo.setPushInfo(pushInfo);

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
