package fr.gouv.stopc.e2e.steps;

import fr.gouv.stopc.e2e.config.ApplicationProperties;
import fr.gouv.stopc.e2e.external.database.mongodb.model.EpochExposition;
import fr.gouv.stopc.e2e.external.database.mongodb.repository.RegistrationRepository;
import fr.gouv.stopc.e2e.mobileapplication.EpochClock;
import fr.gouv.stopc.e2e.mobileapplication.MobileApplication;
import fr.gouv.stopc.e2e.mobileapplication.MobilePhonesEmulator;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.bson.internal.Base64;

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

    private RegistrationRepository registrationRepository;

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
        assertThat(
                exposureStatus.getLastContactDate()
        )
                .isCloseTo(Instant.now(), within(1, ChronoUnit.DAYS));
        assertThat(exposureStatus.getRiskLevel())
                .as("User risk level")
                .isEqualTo(4);
    }

    @Then("{word} data was deleted")
    public void dataWasDeleted(final String userName) {
        // In docker-compose robert-server-ws-rest must contains ESR_LIMIT=0
        // in other way we'll not be able to call status endpoint during 2 min
        var mobile = mobilePhonesEmulator.getMobileApplication(userName);
        final var exposureStatus = mobile.requestStatus();
        assertThat(exposureStatus.getLastContactDate()).isNull();
        assertThat(exposureStatus.getRiskLevel())
                .as("User risk level")
                .isEqualTo(0);
        var optRegistration = this.registrationRepository.findById(Base64.decode(mobile.getApplicationId()));
        if (optRegistration.isPresent()) {
            assertThat(optRegistration.get().getExposedEpochs().isEmpty()).isTrue();
        }
    }

    @SneakyThrows
    @Then("changes last contact date to {naturalTime} for user {word}")
    public void falsifyExposedEpochs(final Instant startDate, final String userName) {
        var mobile = mobilePhonesEmulator.getMobileApplication(userName);
        var optRegistration = this.registrationRepository.findById(Base64.decode(mobile.getApplicationId()));
        if (optRegistration.isPresent()) {
            var registration = optRegistration.get();
            var clock = new EpochClock(3799958400L);// 01/06/2020
            var epochDate = clock.at(startDate);
            registration.setLatestRiskEpoch(epochDate.asEpochId());
            int index = 0;
            for (EpochExposition epochExposition : registration.getExposedEpochs()) {
                epochExposition.setEpochId(epochDate.plusEpochs(index++).asEpochId());
            }
            this.registrationRepository.save(registration);
        }
    }

    @Then("{word} has no notification")
    public void isNotNotifiedAtRisk(final String userName) {
        // In docker-compose robert-server-ws-rest must contains ESR_LIMIT=0
        // in other way we'll not be able to call status endpoint during 2 min
        final MobileApplication mobileApp = mobilePhonesEmulator.getMobileApplication(userName);
        var exposureStatus = mobileApp.requestStatus();
        assertThat(exposureStatus.getRiskLevel())
                .as("User risk level")
                .isEqualTo(0);
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
