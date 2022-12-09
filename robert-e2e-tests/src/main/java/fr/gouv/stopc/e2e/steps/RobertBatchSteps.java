package fr.gouv.stopc.e2e.steps;

import ch.qos.logback.classic.Level;
import fr.gouv.stopc.e2e.config.ApplicationProperties;
import io.cucumber.java.en.Then;
import io.cucumber.java.fr.Lorsque;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@AllArgsConstructor
public class RobertBatchSteps {

    private final ApplicationProperties applicationProperties;

    private final List<String> processLogs = new ArrayList<>();

    @Lorsque("le batch robert est exécuté")
    @SneakyThrows
    public void launchBatch() {
        processLogs.clear();

        final var builder = new ProcessBuilder(applicationProperties.getBatchCommand().split(" "));
        builder.directory(Path.of(".").toFile());
        final var process = builder.start();

        final var background = Executors.newFixedThreadPool(2);
        background.submit(new StreamGobbler(process.getInputStream(), processLogs));
        background.submit(new StreamGobbler(process.getErrorStream(), processLogs));

        final var hasExited = process.waitFor(180, SECONDS);
        background.shutdownNow();
        assertThat(hasExited)
                .as(
                        "Robert batch execution took more than 60 seconds. Container log output:\n    %s",
                        String.join("\n    ", processLogs)
                )
                .isTrue();
        assertThat(process.exitValue())
                .as("Robert batch process exit code. Container log output:\n    %s", String.join("\n    ", processLogs))
                .isEqualTo(0);
    }

    @Then("les logs du batch robert contiennent {logLevel} {string}")
    public void checkDiscardedErrorInBatch(Level logLevel, String message) {
        assertThat(processLogs)
                .filteredOn(event -> event.contains(" " + logLevel.toString() + " "))
                .extracting(event -> event.replaceFirst(".* : ", ""))
                .contains(message);
    }

    private static class StreamGobbler implements Runnable {

        private final InputStream inputStream;

        private final List<String> processLogs;

        public StreamGobbler(final InputStream inputStream, List<String> processLogs) {
            this.inputStream = inputStream;
            this.processLogs = processLogs;
        }

        @Override
        public void run() {
            new BufferedReader(new InputStreamReader(inputStream))
                    .lines()
                    .forEach(line -> {
                        processLogs.add(line);
                        log.debug(line);
                    });
        }
    }
}
