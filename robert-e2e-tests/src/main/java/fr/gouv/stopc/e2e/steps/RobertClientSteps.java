package fr.gouv.stopc.e2e.steps;

import fr.gouv.stopc.e2e.appmobile.AppMobile;
import fr.gouv.stopc.e2e.config.ApplicationProperties;
import fr.gouv.stopc.robert.client.api.CaptchaApi;
import fr.gouv.stopc.robert.client.api.DefaultApi;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
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

    private final DefaultApi robertApi;

    private final CaptchaApi captchaApi;

    private final Map<String, AppMobile> applicationMobileMap = new HashMap<>();

    @Given("application robert ws rest is ready")
    public void applicationRobertIsReady() {
        given()
                .baseUri(applicationProperties.getWsRestBaseUrl().toString())
                .contentType(JSON)

                .when().get("/actuator/health")

                .then()
                .statusCode(200)
                .body("status", equalTo("UP"));
    }

    @Given("{word} install(s) the application TAC")
    public void createAppMobile(final String userName) {
        final var mobileApp = new AppMobile(userName, applicationProperties, captchaApi, robertApi);
        applicationMobileMap.put(userName, mobileApp);
    }

    @Given("{naturalFutureTime}, {word} will be near {word} during {duration}")
    public void generateContactsBetweenTwoUsersWithDuration(final Instant startDate,
            final String firstUserName,
            final String secondUserName,
            final Duration durationOfExchange) {
        final AppMobile mobileApp = applicationMobileMap.get(firstUserName);
        mobileApp.exchangeHelloMessagesWith(
                applicationMobileMap.get(secondUserName),
                startDate,
                durationOfExchange
        );
    }

    @When("{word} report himself/herself/myself sick")
    public void reportContacts(final String userName) {
        final AppMobile mobileApp = applicationMobileMap.get(userName);
        mobileApp.reportContacts();
    }

    @Then("{word} is notified at risk")
    public void isNotifiedAtRisk(final String userName) {
        // In docker-compose robert-server-ws-rest must contains ESR_LIMIT=0
        // in other way we'll not be able to call status endpoint during 2 min
        final AppMobile mobileApp = applicationMobileMap.get(userName);
        assertThat(mobileApp.requestStatus())
                .as("User risk level")
                .isEqualTo(4);
    }

    @Then("{word} has no notification")
    public void isNotNotifiedAtRisk(final String userName) {
        // In docker-compose robert-server-ws-rest must contains ESR_LIMIT=0
        // in other way we'll not be able to call status endpoint during 2 min
        final AppMobile mobileApp = applicationMobileMap.get(userName);
        assertThat(mobileApp.requestStatus())
                .as("User risk level")
                .isEqualTo(0);
    }

    @When("{word} delete his/her/my risk exposure history")
    public void deleteExposureHistory(final String userName) {
        applicationMobileMap.get(userName).deleteExposureHistory();
    }

    @When("{word} does not delete his/her risk exposure history")
    public void notDeleteExposureHistory(final String userName) {
        // Nothing to do
    }

    @When("{word} unregister(s) his/her/my application")
    public void unregister(final String userName) {
        applicationMobileMap.get(userName).unregister();
    }
}
