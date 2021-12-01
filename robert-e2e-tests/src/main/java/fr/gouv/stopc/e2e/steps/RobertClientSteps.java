package fr.gouv.stopc.e2e.steps;

import fr.gouv.stopc.e2e.appmobile.AppMobile;
import fr.gouv.stopc.e2e.appmobile.MobilePhonesEmulator;
import fr.gouv.stopc.e2e.config.ApplicationProperties;
import fr.gouv.stopc.robert.client.api.CaptchaApi;
import fr.gouv.stopc.robert.client.api.DefaultApi;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static fr.gouv.stopc.e2e.external.common.utils.TimeUtils.convertNTPSecondsToUnixMillis;
import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static java.lang.Long.parseLong;
import static java.time.Instant.ofEpochMilli;
import static org.assertj.core.api.Assertions.within;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.hamcrest.Matchers.equalTo;

@Slf4j
@AllArgsConstructor
public class RobertClientSteps {

    private final ApplicationProperties applicationProperties;

    private final DefaultApi robertApi;

    private final CaptchaApi captchaApi;

    private final MobilePhonesEmulator mobilePhonesEmulator;

    private Map<String, AppMobile> getMobileMap() {
        return mobilePhonesEmulator.getApplicationMobileMap();
    }

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
        getMobileMap().put(userName, mobileApp);
    }

    @Given("{naturalTime}, the users {wordList} will be near during {duration}")
    public void generateContactsBetweenTwoUsersWithDuration(final Instant startDate,
            final List<String> userList,
            final Duration durationOfExchange) {

        List<AppMobile> appMobileList = userList.stream()
                .filter(getMobileMap()::containsKey)
                .map(getMobileMap()::get)
                .collect(Collectors.toList());
        mobilePhonesEmulator.exchangeHelloMessagesWith(
                appMobileList,
                startDate,
                durationOfExchange
        );
    }

    @When("{word} report himself/herself/myself sick")
    public void reportContacts(final String userName) {
        getMobileMap().get(userName).reportContacts();
    }

    @Then("{word} is notified at risk")
    public void isNotifiedAtRisk(final String userName) {
        // In docker-compose robert-server-ws-rest must contains ESR_LIMIT=0
        // in other way we'll not be able to call status endpoint during 2 min
        final AppMobile mobileApp = getMobileMap().get(userName);
        var lastExposureStatusResponse = mobileApp.requestStatus();
        Assertions
                .assertThat(
                        ofEpochMilli(
                                convertNTPSecondsToUnixMillis(
                                        parseLong(lastExposureStatusResponse.getLastContactDate())
                                )
                        )
                )
                .isCloseTo(Instant.now(), within(2, ChronoUnit.DAYS));
        assertThat(lastExposureStatusResponse.getRiskLevel())
                .as("User risk level")
                .isEqualTo(4);
    }

    @Then("{word} has no notification")
    public void isNotNotifiedAtRisk(final String userName) {
        // In docker-compose robert-server-ws-rest must contains ESR_LIMIT=0
        // in other way we'll not be able to call status endpoint during 2 min
        final AppMobile mobileApp = getMobileMap().get(userName);
        var lastExposureStatusResponse = mobileApp.requestStatus();
        assertThat(lastExposureStatusResponse.getRiskLevel())
                .as("User risk level")
                .isEqualTo(0);
    }

    @When("{word} delete his/her/my risk exposure history")
    public void deleteExposureHistory(final String userName) {
        getMobileMap().get(userName).deleteExposureHistory();
    }

    @When("{word} does not delete his/her risk exposure history")
    public void notDeleteExposureHistory(final String userName) {
        // Nothing to do
    }

    @When("{word} unregister(s) his/her/my application")
    public void unregister(final String userName) {
        getMobileMap().get(userName).unregister();
    }
}
