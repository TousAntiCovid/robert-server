package fr.gouv.stopc.e2e.steps;

import fr.gouv.stopc.e2e.appmobile.AppMobile;
import fr.gouv.stopc.e2e.config.ApplicationProperties;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.restassured.specification.RequestSpecification;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;
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
    public void createAppMobile(final String userName) {
        AppMobile app = new AppMobile(applicationProperties);
        applicationMobileMap.put(userName, app);
    }

    @Given("{naturalFutureTime}, {word} will be near {word} during {duration}")
    public void generateContactsBetweenTwoUsersWithDuration(final Instant startDate,
            final String firstUserName,
            final String secondUserName,
            final Duration durationOfExchange) {
        AppMobile mainMobileApp = applicationMobileMap.get(firstUserName);
        mainMobileApp.generateContactsWithOtherApps(
                applicationMobileMap.get(secondUserName),
                startDate,
                durationOfExchange
        );
    }

    @When("{word} report himself sick")
    public void reportContacts(final String userName) {
        AppMobile mainMobileApp = applicationMobileMap.get(userName);
        mainMobileApp.reportContacts();
    }

}
