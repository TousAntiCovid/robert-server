package fr.gouv.stopc.e2e.steps;

import fr.gouv.stopc.e2e.config.ApplicationProperties;
import io.cucumber.java.en.And;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Slf4j
@AllArgsConstructor
public class RobertBatchSteps {

    private final ApplicationProperties applicationProperties;

    @And("robert batch as not been executed yet")
    public void batchDoesNotExecute() {

    }

    @And("robert batch has been triggered")
    @SneakyThrows
    public void launchBatch() {
        final var builder = new ProcessBuilder(applicationProperties.getBatchCommand().split(" "));
        builder.directory(Path.of(".").toFile());
        final var process = builder.start();
        final var background = Executors.newFixedThreadPool(2);
        background.submit(new StreamGobbler(process.getInputStream(), log::debug));
        background.submit(new StreamGobbler(process.getErrorStream(), log::error));
        boolean hasExited = process.waitFor(60, SECONDS);
        background.shutdownNow();
        assertThat(hasExited)
                .as("Robert batch execution timed out after 60 seconds")
                .isTrue();
        assertThat(process.exitValue())
                .as("Robert batch process exit code")
                .isEqualTo(0);
    }

    private static class StreamGobbler implements Runnable {

        private final InputStream inputStream;

        private final Consumer<String> consumer;

        public StreamGobbler(final InputStream inputStream, final Consumer<String> consumer) {
            this.inputStream = inputStream;
            this.consumer = consumer;
        }

        @Override
        public void run() {
            new BufferedReader(new InputStreamReader(inputStream))
                    .lines()
                    .forEach(consumer);
        }
    }
}
