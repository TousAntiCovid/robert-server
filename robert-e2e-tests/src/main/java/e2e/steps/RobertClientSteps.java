package e2e.steps;

import e2e.config.ApplicationProperties;
import io.cucumber.java.en.Given;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

@Slf4j
@AllArgsConstructor
public class RobertClientSteps {

    private ApplicationProperties applicationProperties;

    @Given("application robert ws rest is ready")
    public void application_robert_is_ready() {
        given()
                .get(applicationProperties.getWsRestBaseUrl() + "/actuator/health")
                .then()
                .statusCode(200)
                .body("status", equalTo("UP"));
    }

}
