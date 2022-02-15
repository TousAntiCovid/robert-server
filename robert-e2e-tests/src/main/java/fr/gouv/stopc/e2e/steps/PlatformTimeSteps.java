package fr.gouv.stopc.e2e.steps;

import io.cucumber.java.fr.Alors;
import io.cucumber.java.fr.Etantdonnéque;
import lombok.AllArgsConstructor;

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

import static java.time.temporal.ChronoUnit.MINUTES;
import static org.assertj.core.api.Assertions.within;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@AllArgsConstructor
public class PlatformTimeSteps {

    @Etantdonnéque("l'on est aujourd'hui")
    public void resetFakeTimeToNow() throws IOException, InterruptedException {
        new ProcessBuilder()
                .command(execInContainer("ws-rest", "rm -f /etc/faketime.d/faketime"))
                .start()
                .waitFor(2, TimeUnit.SECONDS);
    }

    @Etantdonnéque("l'on est il y a {duration}")
    public void changeSystemDateTo(final Duration durationAgo) throws IOException, InterruptedException {

        new ProcessBuilder()
                .command(execInContainer("ws-rest", "echo -" + durationAgo.toSeconds() + " > /etc/faketime.d/faketime"))
                .start()
                .waitFor(3, TimeUnit.SECONDS);

    }

    @Alors("l'horloge de {word} est à il y a {naturalTime}")
    public void verifyContainerClock(final String containerName,
            final Instant dateInPast) throws IOException {
        assertThat(getContainerTime(containerName)).isCloseTo(dateInPast, within(1, MINUTES));
    }

    private List<String> execInContainer(final String containerName,
            final String command) {
        return List.of("docker-compose", "exec", "-T", containerName, "bash", "-c", command);
    }

    public Instant getContainerTime(String containerName) throws IOException {

        final var getTimeProcess = new ProcessBuilder()
                .command(execInContainer(containerName, "date"))
                .start();

        final var reader = new BufferedReader(new InputStreamReader(getTimeProcess.getInputStream()));
        final var stringJoiner = new StringJoiner(System.getProperty("line.separator"));
        reader.lines().iterator().forEachRemaining(stringJoiner::add);
        final var retrievedString = stringJoiner.toString();

        return ZonedDateTime
                .parse(retrievedString, DateTimeFormatter.ofPattern("EEE dd MMM yyyy hh:mm:ss a zzz", Locale.ENGLISH))
                .toInstant()
                .truncatedTo(ChronoUnit.MINUTES);
    }

}
