package fr.gouv.stopc.e2e.steps;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.gouv.stopc.e2e.config.ApplicationProperties;
import io.cucumber.java.fr.Alors;
import io.cucumber.java.fr.Etantdonnéque;
import lombok.AllArgsConstructor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.StringJoiner;
import java.util.concurrent.TimeUnit;

import static java.lang.System.getProperty;
import static java.time.Duration.ZERO;
import static java.time.Instant.now;
import static java.time.format.DateTimeFormatter.*;
import static java.time.temporal.ChronoUnit.MINUTES;
import static java.util.Locale.*;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.assertj.core.api.Assertions.within;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.awaitility.Awaitility.await;
import static org.awaitility.pollinterval.FibonacciPollInterval.fibonacci;

@AllArgsConstructor
public class PlatformTimeSteps {

    private final ApplicationProperties applicationProperties;

    @Etantdonnéque("l'on est aujourd'hui")
    public void resetFakeTimeToNow() throws IOException {
        new ProcessBuilder()
                .command(execInContainer("ws-rest", "rm -f /etc/faketime.d/faketime"))
                .start();
        verifyServiceClock("ws-rest", ZERO);
        verifyServiceClock("crypto-server", ZERO);
    }

    @Etantdonnéque("l'on est il y a {duration}")
    public void changeSystemDateTo(final Duration durationAgo) throws IOException {

        new ProcessBuilder()
                .command(execInContainer("ws-rest", "echo -" + durationAgo.toSeconds() + " > /etc/faketime.d/faketime"))
                .start();

        verifyServiceClock("ws-rest", durationAgo);
        verifyServiceClock("crypto-server", durationAgo);
    }

    @Alors("l'horloge de {word} est à il y a {duration}")
    public void verifyServiceClock(final String containerName,
            final Duration duration) throws IOException {

        final var expectedFakedInstant = now().minus(duration);
        final var containerInstant = getContainerTime(containerName);

        assertThat(containerInstant).isCloseTo(expectedFakedInstant, within(1, MINUTES));

        await("Wait for faked time to be set accross service process")
                .atMost(1, TimeUnit.MINUTES)
                .pollInterval(fibonacci(MILLISECONDS))
                .untilAsserted(
                        () -> assertThat(getServiceDateFromContainer(containerName))
                                .isCloseTo(containerInstant, within(1, MINUTES))
                );

    }

    private List<String> execInContainer(final String containerName,
            final String command) {
        return List.of("docker-compose", "exec", "-T", containerName, "bash", "-c", command);
    }

    public Instant getContainerTime(final String containerName) throws IOException {

        final var getTimeProcess = new ProcessBuilder()
                .command(execInContainer(containerName, "date"))
                .start();

        final var reader = new BufferedReader(new InputStreamReader(getTimeProcess.getInputStream()));
        final var stringJoiner = new StringJoiner(getProperty("line.separator"));
        reader.lines().iterator().forEachRemaining(stringJoiner::add);
        final var retrievedString = stringJoiner.toString();

        return ZonedDateTime
                .parse(retrievedString, ofPattern("EEE dd MMM yyyy hh:mm:ss a zzz", ENGLISH))
                .toInstant()
                .truncatedTo(ChronoUnit.MINUTES);
    }

    private Instant getServiceDateFromContainer(final String containerName) throws IOException {

        final var command = "curl -X GET -H 'Content-Type: application/json' localhost:8081/actuator/info";
        final var getTimeProcess = new ProcessBuilder()
                .command(execInContainer(containerName, command))
                .start();

        final var reader = new BufferedReader(new InputStreamReader(getTimeProcess.getInputStream()));
        final var stringJoiner = new StringJoiner(getProperty("line.separator"));
        reader.lines().iterator().forEachRemaining(stringJoiner::add);
        final var retrievedString = stringJoiner.toString();

        new ObjectMapper().reader().readTree(retrievedString)
                .get("robertClock")
                .get("currentTime")
                .asText();

        return Instant.parse(
                new ObjectMapper().reader().readTree(retrievedString)
                        .get("robertClock")
                        .get("currentTime")
                        .asText()
        );
    }

}
