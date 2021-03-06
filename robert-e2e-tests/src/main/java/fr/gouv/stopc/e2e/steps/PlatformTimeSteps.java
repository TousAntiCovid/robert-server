package fr.gouv.stopc.e2e.steps;

import fr.gouv.stopc.robert.client.api.RobertApi;
import io.cucumber.java.fr.Alors;
import io.cucumber.java.fr.Etantdonnéque;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static java.lang.String.format;
import static java.time.Duration.ZERO;
import static java.time.Instant.now;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.stream.Collectors.joining;
import static org.assertj.core.api.Assertions.within;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.awaitility.Awaitility.given;
import static org.awaitility.pollinterval.FibonacciPollInterval.fibonacci;

@AllArgsConstructor
public class PlatformTimeSteps {

    @Autowired
    private final RobertApi robertApi;

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
                .atMost(1, MINUTES)
                .with()
                .pollInterval(fibonacci(MILLISECONDS))
                .and()
                .untilAsserted(
                        () -> assertThat(robertApi.clock().getTime().toInstant())
                                .isCloseTo(expectedFakedInstant, within(1, ChronoUnit.MINUTES))
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
                .as("exit code for command: %s", String.join(" ", dockerExecCommand))
                .isEqualTo(0);

        try (final var reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            return reader.lines()
                    .collect(joining());
        }
    }
}
