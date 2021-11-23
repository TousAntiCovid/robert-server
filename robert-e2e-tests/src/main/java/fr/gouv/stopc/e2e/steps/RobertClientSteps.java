package fr.gouv.stopc.e2e.steps;

import fr.gouv.stopc.e2e.appmobile.AppMobile;
import fr.gouv.stopc.e2e.config.ApplicationProperties;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
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
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
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
        mainMobileApp.generateContactsWithOtherApp(
                applicationMobileMap.get(secondUserName),
                startDate,
                durationOfExchange
        );
    }

    @When("{word} report himself/herself sick")
    public void reportContacts(final String userName) {
        AppMobile mainMobileApp = applicationMobileMap.get(userName);
        mainMobileApp.reportContacts();
    }

    @Then("{word} is notified at risk")
    public void isNotifiedAtRisk(String userName) {
        // In docker-compose robert-server-ws-rest must contains ESR_LIMIT=0
        // in other way we'll not be able to call status endpoint during 2 min
        AppMobile mainMobileApp = applicationMobileMap.get(userName);
        mainMobileApp.requestStatus();
        assertThat(mainMobileApp.getLastExposureStatusResponse().getRiskLevel())
                .as("User is at risk")
                .isEqualTo(4);
    }

    @Then("{word} has no notification")
    public void isNotNotifiedAtRisk(String userName) {
        // In docker-compose robert-server-ws-rest must contains ESR_LIMIT=0
        // in other way we'll not be able to call status endpoint during 2 min
        AppMobile mainMobileApp = applicationMobileMap.get(userName);
        mainMobileApp.requestStatus();
        assertThat(mainMobileApp.getLastExposureStatusResponse().getRiskLevel())
                .as("User is not at risk")
                .isEqualTo(0);
    }

}
