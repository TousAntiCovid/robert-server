package fr.gouv.stopc.e2e.steps;

import fr.gouv.stopc.robert.client.api.RobertApi;
import io.cucumber.java.fr.Alors;
import io.cucumber.java.fr.Etantdonnéqu;
import io.cucumber.java.fr.Etantdonnéque;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZonedDateTime;
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

@RequiredArgsConstructor
public class PlatformTimeSteps {

    private final RobertApi robertApi;

    public ZonedDateTime getPlatformTime() {
        return ZonedDateTime.parse(execInContainer("ws-rest", "date --iso-8601=seconds -u"));
    }

    public LocalDate getPlatformDate() {
        return getPlatformTime().toLocalDate();
    }

    @Etantdonnéqu("l'on est aujourd'hui")
    @Etantdonnéqu("on est aujourd'hui")
    public void resetFakeTimeToNow() {
        changeSystemTimeTo(ZERO);
    }

    @Etantdonnéque("l'on est il y a {duration}")
    public void changeSystemTimeTo(final Duration durationAgo) {
        execInContainer("ws-rest", format("echo -%d > /etc/faketime.d/faketime", durationAgo.toSeconds()));
        verifyServiceClock("ws-rest", durationAgo);
        verifyServiceClock("crypto-server", durationAgo);
    }

    @Alors("l'horloge de {word} est à il y a {duration}")
    public void verifyServiceClock(final String containerName, final Duration duration) {

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

    @SneakyThrows
    private String execInContainer(final String containerName, final String command) {

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
