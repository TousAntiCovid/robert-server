package e2e.steps;

import e2e.config.ApplicationProperties;
import e2e.context.ScenarioContext;
import e2e.context.User;
import e2e.crypto.exception.RobertServerCryptoException;
import e2e.dto.CaptchaCreationRequest;
import e2e.dto.PushInfoVo;
import e2e.dto.RegisterSuccessResponse;
import e2e.dto.RegisterVo;
import e2e.model.AppMobile;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;

import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@Slf4j
@AllArgsConstructor
public class RobertClientSteps {

    public static final String CAPTCHA_BYPASS_SOLUTION = "IEDX";

    private final ApplicationProperties applicationProperties;

    private final ScenarioContext scenarioContext;

    private final Map<String, AppMobile> appMobileMap;

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
    public void resolve_captcha_challenge(String userName) {
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
        user.setCaptchaId(RandomStringUtils.random(7, true, false));

        givenRobertBaseUri()
                .when()
                .get("/api/v6/captcha/{captchaId}/image", user.getCaptchaId())
                .then()
                .statusCode(200);

        // Simulate visual resolution
        user.setCaptchaSolution(CAPTCHA_BYPASS_SOLUTION);
    }

    @Then("{word} is registered on TAC")
    public void register_user_on_TAC(String userName)
            throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, RobertServerCryptoException {
        User user = this.scenarioContext.getOrCreateUser(userName);

        AppMobile appMobile = new AppMobile(
                user.getCaptchaId(),
                user.getCaptchaSolution(),
                this.applicationProperties.getCryptoPublicKey()
        );
        user.setClientPublicECDHKey(appMobile.getPublicKey());

        RegisterVo registerVo = new RegisterVo();
        registerVo.setCaptchaId(user.getCaptchaId());
        registerVo.setCaptcha(user.getCaptchaSolution());
        registerVo.setClientPublicECDHKey(user.getClientPublicECDHKey());

        PushInfoVo pushInfo = new PushInfoVo();
        pushInfo.setToken("string");
        pushInfo.setLocale("fr");
        pushInfo.setTimezone("Europe/Paris");

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
        appMobileMap.put(user.getCaptchaId(), appMobile);
    }

    @When("{word} is near {word} during 1 hour")
    public void generate_contact_between_two_users(String firstUserName, String secondUserName) {
        User firstUser = this.scenarioContext.getOrCreateUser(firstUserName);
        Objects.requireNonNull(firstUser);
        User secondUser = this.scenarioContext.getOrCreateUser(secondUserName);
        Objects.requireNonNull(secondUser);

        List<String> limitedAppMobileIds = new ArrayList<>();
        limitedAppMobileIds.add(secondUser.getCaptchaId());

        AppMobile appMobile = appMobileMap.get(firstUser.getCaptchaId());
        Objects.requireNonNull(appMobile);
        assertThat(appMobile.numberOfContacts()).as("There is no contact").isEqualTo(0);

        appMobile.generateHelloMessageDuring(appMobile, limitedAppMobileIds, appMobileMap, 1);
        assertThat(appMobile.numberOfContacts()).as("There is only one contact").isEqualTo(1);
    }

}
