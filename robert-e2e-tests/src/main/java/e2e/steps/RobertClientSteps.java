package e2e.steps;

import e2e.config.ApplicationProperties;
import e2e.context.ScenarioContext;
import e2e.context.User;
import e2e.dto.CaptchaCreationRequest;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.restassured.specification.RequestSpecification;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

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

    @Given("{word} application request a captcha challenge id")
    public void request_captcha_challenge_id(String userName) {
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

        // We generate a fake Captcha id which will be used to differentiate the mobile
        // apps
        user.setCaptchaId(RandomStringUtils.random(7, true, false));
    }

    @Given("{word} application request an image captcha challenge with the previously received id")
    public void request_image_captcha_challenge_with_id(String userName) {
        User user = this.scenarioContext.getOrCreateUser(userName);

        givenRobertBaseUri()
                .when()
                .get("/api/v6/captcha/{captchaId}/image", user.getCaptchaId())
                .then()
                .statusCode(200);
    }

    @When("{word} resolve the Captcha image into the application")
    public void resolve_captcha_image_challenge(String userName) {
        // Simulate visual resolution
        this.scenarioContext.getOrCreateUser(userName).setCaptchaSolution(CAPTCHA_BYPASS_SOLUTION);
    }

}
