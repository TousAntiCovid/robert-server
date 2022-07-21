package fr.gouv.stopc.e2e.steps;

import fr.gouv.stopc.e2e.config.ApplicationProperties;
import fr.gouv.stopc.e2e.mobileapplication.MobilePhonesEmulator;
import io.cucumber.java.en.When;
import io.cucumber.java.fr.Alors;
import io.cucumber.java.fr.Etantdonnéque;
import io.cucumber.java.fr.Lorsque;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static java.time.temporal.ChronoUnit.DAYS;
import static org.assertj.core.api.Assertions.within;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.hamcrest.Matchers.equalTo;

@Slf4j
@AllArgsConstructor
public class RobertClientSteps {

    private final ApplicationProperties applicationProperties;

    private final MobilePhonesEmulator mobilePhonesEmulator;

    private final RobertBatchSteps robertBatchSteps;

    private final PlatformTimeSteps platformTimeSteps;

    @Etantdonnéque("l'application robert ws rest est démarrée")
    public void applicationRobertIsReady() {
        given()
                .baseUri(applicationProperties.getWsRestBaseUrl().toString())
                .contentType(JSON)

                .when().get("/actuator/health")

                .then()
                .statusCode(200)
                .body("status", equalTo("UP"));
    }

    @Etantdonnéque("{wordList} a/ont l'application TAC")
    public void createMobileApplication(final List<String> users) {
        users.forEach(mobilePhonesEmulator::createMobileApplication);
    }

    @Etantdonnéque("{wordList} a/ont l'application TAC depuis {duration}")
    public void createMobileApplication(final List<String> users, final Duration installationBackInTime) {
        platformTimeSteps.changeSystemTimeTo(installationBackInTime);
        users.forEach(mobilePhonesEmulator::createMobileApplication);
    }

    @Etantdonnéque("{wordList} sont à proximité {duration}")
    public void generateContactsBetweenTwoUsersWithDuration(final List<String> users,
            final Duration durationOfExchange) {
        mobilePhonesEmulator.exchangeHelloMessagesBetween(
                users,
                platformTimeSteps.getPlatformTime().toInstant(),
                durationOfExchange
        );
    }

    @Etantdonnéque("{wordList} étaient à proximité {duration} il y a {duration} et que {word} s'est déclaré/déclarée malade")
    public void pastContactAndReport(final List<String> users, final Duration proximityExpositionDuration,
            final Duration durationBackInTime, final String userNameReporter) {
        platformTimeSteps.changeSystemTimeTo(durationBackInTime);
        generateContactsBetweenTwoUsersWithDuration(users, proximityExpositionDuration);
        reportContacts(userNameReporter);
        robertBatchSteps.launchBatch();
    }

    @Etantdonnéque("{wordList} étaient à proximité {duration} il y a {duration} {duration} {duration} et que {word} s'est déclaré/déclarée malade")
    public void pastContactAndReport(final List<String> users, final Duration proximityExpositionDuration,
            final Duration daysBackInTime, final Duration hoursBackInTime, final Duration minutesBackInTime,
            final String userNameReporter) {
        final var durationBackInTime = daysBackInTime.plus(hoursBackInTime).plus(minutesBackInTime);
        pastContactAndReport(users, proximityExpositionDuration, durationBackInTime, userNameReporter);
    }

    @Etantdonnéque("{word} se déclare malade")
    public void reportContacts(final String userName) {
        mobilePhonesEmulator.getMobileApplication(userName).reportContacts();
    }

    @Etantdonnéque("{word} est à risque")
    public void isNotifiedAtRisk(final String userName) {
        final var exposureStatus = mobilePhonesEmulator
                .getMobileApplication(userName)
                .requestStatus();
        assertThat(exposureStatus.getRiskLevel())
                .as("User risk level")
                .isEqualTo(4);
    }

    @Etantdonnéque("{word} n'est pas à risque")
    public void isNotNotifiedAtRisk(final String userName) {
        final var exposureStatus = mobilePhonesEmulator
                .getMobileApplication(userName)
                .requestStatus();
        assertThat(exposureStatus.getRiskLevel())
                .as("User risk level")
                .isEqualTo(0);
    }

    @Alors("les données de {word} n'existent plus")
    public void dataWasDeleted(final String userName) {
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

    @Alors("le token CNAM de {word} est proche de {relativeTime}")
    public void assertLastContactDateIsNear(final String userName, final Instant startDate) {
        final var exposureStatus = mobilePhonesEmulator.getMobileApplication(userName)
                .requestStatus();
        assertThat(exposureStatus.getLastContactDate())
                .as("last contact date")
                .isCloseTo(startDate, within(1, DAYS));
        assertThat(exposureStatus.getCnamLastContactDate())
                .as("CNAM token last contact date")
                .isCloseTo(startDate, within(1, DAYS));
    }

    @Lorsque("{word} supprime son historique d'exposition")
    public void deleteExposureHistory(final String userName) {
        mobilePhonesEmulator.getMobileApplication(userName).deleteExposureHistory();
    }

    @When("{word} se désinscrit")
    public void unregister(final String userName) {
        mobilePhonesEmulator.getMobileApplication(userName).unregister();
    }

}
