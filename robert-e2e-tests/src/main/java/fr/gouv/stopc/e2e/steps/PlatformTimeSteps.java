package fr.gouv.stopc.e2e.steps;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.fr.Alors;
import io.cucumber.java.fr.Etantdonnéque;
import lombok.AllArgsConstructor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.*;
import java.util.List;
import java.util.StringJoiner;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
import static java.lang.String.join;
import static java.lang.System.getProperty;
import static java.time.Duration.ZERO;
import static java.time.Instant.now;
import static java.time.temporal.ChronoUnit.MINUTES;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.assertj.core.api.Assertions.within;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.awaitility.Awaitility.given;
import static org.awaitility.pollinterval.FibonacciPollInterval.fibonacci;

@AllArgsConstructor
public class PlatformTimeSteps {

    @Etantdonnéque("l'on est aujourd'hui")
    public void resetFakeTimeToNow() throws IOException, InterruptedException {

        execInContainer("ws-rest", "rm -f /etc/faketime.d/faketime");
        verifyServiceClock("ws-rest", ZERO);
        verifyServiceClock("crypto-server", ZERO);
    }

    @Etantdonnéque("l'on est il y a {duration}")
    public void changeSystemDateTo(final Duration durationAgo) throws IOException, InterruptedException {

        execInContainer("ws-rest", format("echo -%d > /etc/faketime.d/faketime", durationAgo.toSeconds()));
        verifyServiceClock("ws-rest", durationAgo);
        verifyServiceClock("crypto-server", durationAgo);
    }

    @Alors("l'horloge de {word} est à il y a {duration}")
    public void verifyServiceClock(final String containerName,
            final Duration duration) {

        final var expectedFakedInstant = now().minus(duration);
        given()
                .await("Wait for faked time to be set in service " + containerName)
                .atMost(1, TimeUnit.MINUTES)
                .with()
                .pollInterval(fibonacci(MILLISECONDS))
                .and()
                .untilAsserted(
                        () -> assertThat(getServiceDateFromContainer(containerName))
                                .isCloseTo(expectedFakedInstant, within(1, MINUTES))
                );

    }

    private String execInContainer(
            final String containerName,
            final String command) throws IOException, InterruptedException {

        final var dockerExecCommand = List
                .of("docker-compose", "exec", "-T", containerName, "bash", "-c", command);
        final var process = new ProcessBuilder()
                .command(dockerExecCommand)
                .start();
        assertThat(process.waitFor())
                .as("docker exec exit code : %s", join(" ", dockerExecCommand))
                .isEqualTo(0);

        final var reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        final var stringJoiner = new StringJoiner(getProperty("line.separator"));
        reader.lines().iterator().forEachRemaining(stringJoiner::add);
        return stringJoiner.toString();
    }

    private Instant getServiceDateFromContainer(final String containerName) throws IOException, InterruptedException {

        final var actuatorInfoResult = execInContainer(
                containerName,
                "curl -X GET -H 'Content-Type: application/json' localhost:8081/actuator/info"
        );
        return Instant.parse(
                new ObjectMapper().reader().readTree(actuatorInfoResult)
                        .get("robertClock")
                        .get("currentTime")
                        .asText()
        );
    }

}
