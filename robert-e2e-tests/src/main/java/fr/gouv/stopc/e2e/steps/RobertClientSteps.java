package fr.gouv.stopc.e2e.steps;

import fr.gouv.stopc.e2e.config.ApplicationProperties;
import fr.gouv.stopc.e2e.mobileapplication.MobilePhonesEmulator;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.assertj.core.api.Assertions.within;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.hamcrest.Matchers.equalTo;

@Slf4j
@AllArgsConstructor
public class RobertClientSteps {

    private final ApplicationProperties applicationProperties;

    private final MobilePhonesEmulator mobilePhonesEmulator;

    private final RobertBatchSteps robertBatchSteps;

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
    public void createMobileApplication(final String userName) {
        mobilePhonesEmulator.createMobileApplication(userName);
    }

    @Given("{naturalTime}, the users {wordList} will be near during {duration}")
    public void generateContactsBetweenTwoUsersWithDuration(final Instant startDate,
            final List<String> users,
            final Duration durationOfExchange) {

        mobilePhonesEmulator.exchangeHelloMessagesBetween(
                users,
                startDate,
                durationOfExchange
        );
    }

    @Given("{naturalTime}, the users {wordList} has met during {duration} then {word} report himself/herself sick")
    public void generateContactsBetweenTwoUsersWithDurationInThePast(final Instant startDate,
            final List<String> users,
            final Duration durationOfExchange,
            final String userNameReporter) {

        mobilePhonesEmulator.exchangeHelloMessagesBetween(
                users,
                Instant.now(),
                durationOfExchange
        );

        mobilePhonesEmulator.getMobileApplication(userNameReporter).reportContacts();
        robertBatchSteps.launchBatch();
        // We modify exposed epochs and we re-launch batch in order to re-compute
        // latestRiskEpoch
        users.stream()
                .filter(
                        otherUser -> !otherUser.equals(userNameReporter)
                )
                .forEach(
                        user -> mobilePhonesEmulator.getMobileApplication(user)
                                .changeExposedEpochsDatesStartingAt(startDate)
                );
        robertBatchSteps.launchBatch();
    }

    @When("{word} report himself/herself/myself sick")
    public void reportContacts(final String userName) {
        mobilePhonesEmulator.getMobileApplication(userName).reportContacts();
    }

    @Then("{word} is notified at risk")
    public void isNotifiedAtRisk(final String userName) {
        // In docker-compose robert-server-ws-rest must contains ESR_LIMIT=0
        // in other way we'll not be able to call status endpoint during 2 min
        final var exposureStatus = mobilePhonesEmulator
                .getMobileApplication(userName)
                .requestStatus();
        assertThat(exposureStatus.getRiskLevel())
                .as("User risk level")
                .isEqualTo(4);
    }

    @Then("{word} is not notified at risk")
    public void isNotNotifiedAtRisk(final String userName) {
        // In docker-compose robert-server-ws-rest must contains ESR_LIMIT=0
        // in other way we'll not be able to call status endpoint during 2 min
        final var exposureStatus = mobilePhonesEmulator
                .getMobileApplication(userName)
                .requestStatus();
        assertThat(exposureStatus.getRiskLevel())
                .as("User risk level")
                .isEqualTo(0);
    }

    @Then("all {word}'s contact and risk data older than 15 days were deleted")
    public void dataWasDeleted(final String userName) {
        // In docker-compose robert-server-ws-rest must contains ESR_LIMIT=0
        // in other way we'll not be able to call status endpoint during 2 min
        var mobile = mobilePhonesEmulator.getMobileApplication(userName);
        final var exposureStatus = mobile.requestStatus();
        assertThat(exposureStatus.getLastContactDate()).isNull();
        assertThat(exposureStatus.getRiskLevel())
                .as("User risk level")
                .isEqualTo(0);
        assertThat(mobile.getRegistration().getExposedEpochs().size())
                .as("Exposed epochs list")
                .isEqualTo(0);
    }

    /**
     * Note : Dont use that function twice in the same scenario because the second
     * pass will override the firsts exposedEpochsDates (we cannot differentiate
     * them from each other)
     */
    @SneakyThrows
    @Then("{naturalTime}, {wordList} met and {word} was/were at risk following {word} report")
    public void falsifyExposedEpochs(final Instant startDate,
            List<String> users,
            final String userNameAtRisk,
            final String userNameReporter) {
        mobilePhonesEmulator.exchangeHelloMessagesBetween(
                users,
                Instant.now(),
                Duration.of(60, ChronoUnit.MINUTES)
        );
        mobilePhonesEmulator.getMobileApplication(userNameReporter).reportContacts();
        robertBatchSteps.launchBatch();
        // We modify exposed epochs and we re-launch batch in order to re-compute
        // latestRiskEpoch
        mobilePhonesEmulator.getMobileApplication(userNameAtRisk).changeExposedEpochsDatesStartingAt(startDate);
        robertBatchSteps.launchBatch();
    }

    @Then("{word} last contact is now near {naturalTime}")
    public void verifyLastContactValid(final String userName,
            final Instant startDate) {
        var mobile = mobilePhonesEmulator.getMobileApplication(userName);
        final var exposureStatus = mobile.requestStatus();
        assertThat(
                exposureStatus.getLastContactDate()
        )
                .isCloseTo(startDate, within(1, ChronoUnit.DAYS));
        assertThat(
                exposureStatus.getCnamLastContactDate()
        )
                .isCloseTo(startDate, within(1, ChronoUnit.DAYS));
    }

    @When("{word} delete his/her/my risk exposure history")
    public void deleteExposureHistory(final String userName) {
        mobilePhonesEmulator.getMobileApplication(userName).deleteExposureHistory();
    }

    @When("{word} does not delete his/her risk exposure history")
    public void notDeleteExposureHistory(final String userName) {
        // Nothing to do
    }

    @When("{word} unregister(s) his/her/my application")
    public void unregister(final String userName) {
        mobilePhonesEmulator.getMobileApplication(userName).unregister();
    }
}
