package fr.gouv.stopc.e2e.steps;

import fr.gouv.stopc.e2e.config.ApplicationProperties;
import fr.gouv.stopc.e2e.mobileapplication.timemachine.repository.ClientIdentifierRepository;
import io.cucumber.java.fr.Alors;
import io.cucumber.java.fr.Etantdonnéque;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.StringJoiner;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static java.time.temporal.ChronoUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.http.HttpHeaders.DATE;
import static org.assertj.core.api.Assertions.within;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.awaitility.Awaitility.await;
import static org.awaitility.pollinterval.FibonacciPollInterval.fibonacci;
import static org.hamcrest.Matchers.is;

@Slf4j
@AllArgsConstructor
public class RobertPlatformTime {

    private final ApplicationProperties applicationProperties;

    private ClientIdentifierRepository clientIdentifierRepository;

    @Etantdonnéque("l'on est aujourd'hui")
    public void resetFakeTimeToNow() throws IOException, InterruptedException {
        new ProcessBuilder()
                .command(execInContainer("ws-rest", "rm -f /etc/faketime.d/faketime"))
                .start()
                .waitFor(3, TimeUnit.SECONDS);
    }

    @Etantdonnéque("l'on est il y a {naturalTime}")
    public void changeSystemDateTo(final Instant dateInPast) throws IOException, InterruptedException {

        final var secondsBetweenNowAndDate = Duration.between(Instant.now(), dateInPast).toSeconds();

        new ProcessBuilder()
                .command(execInContainer("ws-rest", "echo " + secondsBetweenNowAndDate + " > /etc/faketime.d/faketime"))
                .start()
                .waitFor(3, TimeUnit.SECONDS);

        // assertThat(getPostgresqlServiceInstant()).isEqualTo(dateInPast.truncatedTo(ChronoUnit.MINUTES));
        // TODO: dockerise if time management is needed
    }

    @Alors("l'horloge de robert-ws est à il y a {naturalTime}")
    public void verifyRobertWSClock(final Instant dateInPast) throws IOException {
        assertThat(containerDateTimeAsInstant("ws-rest")).isCloseTo(dateInPast, within(1, MINUTES));
    }

    @Alors("l'horloge de crypto-server est à il y a {naturalTime}")
    public void verifyRobertCryptoServerClock(final Instant dateInPast) throws IOException {
        assertThat(containerDateTimeAsInstant("crypto-server")).isCloseTo(dateInPast, within(1, MINUTES));
    }

    private List<String> execInContainer(final String containerName, final String command) {
        return List.of("docker-compose", "exec", "-T", containerName, "bash", "-c", command);
    }

    public Instant getPostgresqlServiceInstant() {
        return LocalDateTime.parse(clientIdentifierRepository.getDate().replace(" ", "T"))
                .atZone(ZoneId.of("Europe/Paris")).withZoneSameInstant(ZoneId.of("UTC")).toInstant()
                .truncatedTo(ChronoUnit.MINUTES);
    }

    public Instant containerDateTimeAsInstant(String containerName) throws IOException {

        final var getTimeProcess = new ProcessBuilder()
                .command(execInContainer(containerName, "date"))
                .start();

        final var reader = new BufferedReader(new InputStreamReader(getTimeProcess.getInputStream()));
        final var stringJoiner = new StringJoiner(System.getProperty("line.separator"));
        reader.lines().iterator().forEachRemaining(stringJoiner::add);
        final var retrievedString = stringJoiner.toString();

        final var containerInstant = ZonedDateTime
                .parse(retrievedString, DateTimeFormatter.ofPattern("EEE dd MMM yyyy hh:mm:ss a zzz", Locale.ENGLISH))
                .toInstant()
                .truncatedTo(ChronoUnit.MINUTES);

        if (containerName.equals("ws-rest")) {
            await("Wait for faketime to be set accross java process")
                    .atMost(1, TimeUnit.MINUTES)
                    .pollInterval(fibonacci(SECONDS))
                    .until(this::getWSRestServiceDate, is(containerInstant));
        }

        return containerInstant;
    }

    public Instant getWSRestServiceDate() {

        final var redirectResponse = given()
                .contentType(JSON)
                .baseUri(applicationProperties.getWsRestBaseUrl().toString())
                .expect()
                .statusCode(400)

                .when()
                .post("/api/v6/status");

        final var headerDateValue = redirectResponse.then().extract().header(DATE);
        return OffsetDateTime.parse(headerDateValue, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant()
                .truncatedTo(MINUTES);
    }

}
