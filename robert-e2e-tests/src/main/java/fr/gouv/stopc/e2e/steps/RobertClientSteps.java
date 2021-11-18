package fr.gouv.stopc.e2e.steps;

import fr.gouv.stopc.e2e.appmobile.AppMobile;
import fr.gouv.stopc.e2e.config.ApplicationProperties;
import io.cucumber.java.en.Given;
import io.restassured.specification.RequestSpecification;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.Matchers.equalTo;

@Slf4j
@AllArgsConstructor
public class RobertClientSteps {

    private final ApplicationProperties applicationProperties;

    private final Map<String, AppMobile> applicationMobileMap = new HashMap<>();

    public AppMobile createApplication(String name, ApplicationProperties applicationProperties) {
        AppMobile app = new AppMobile(applicationProperties);
        applicationMobileMap.put(name, app);
        return app;
    }

    private RequestSpecification givenRobertBaseUri() {
        return given()
                .baseUri(applicationProperties.getWsRestBaseUrl())
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

    @Given("{word} install the application TAC")
    public void createAppMobile(String userName) {
        createApplication(userName, applicationProperties);
    }

}
