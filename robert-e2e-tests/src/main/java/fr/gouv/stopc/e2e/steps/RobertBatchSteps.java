package fr.gouv.stopc.e2e.steps;

import fr.gouv.stopc.e2e.config.ApplicationProperties;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Executors;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Slf4j
@AllArgsConstructor
public class RobertBatchSteps {

    private final ApplicationProperties applicationProperties;

    private final List<String> processErrorLogs;

    private final List<String> processLogs;

    private void getLogsListFromProcessInputStream(final InputStream processStdOutput, List<String> lineList) {
        lineList.clear();
        new BufferedReader(new InputStreamReader(processStdOutput))
                .lines()
                .forEach(line -> {
                    lineList.add(line);
                    log.debug(line);
                });
    }

    @When("robert batch as not been executed yet")
    public void batchDoesNotExecute() {
        // Nothing to do
    }

    @When("robert batch has been triggered")
    @SneakyThrows
    public void launchBatch() {
        final var builder = new ProcessBuilder(applicationProperties.getBatchCommand().split(" "));
        builder.directory(Path.of(".").toFile());
        final var process = builder.start();
        final var background = Executors.newFixedThreadPool(2);

        getLogsListFromProcessInputStream(process.getInputStream(), processLogs);
        getLogsListFromProcessInputStream(process.getErrorStream(), processErrorLogs);

        boolean hasExited = process.waitFor(60, SECONDS);
        background.shutdownNow();
        assertThat(hasExited)
                .as("Robert batch execution timed out after 60 seconds")
                .isTrue();
        assertThat(process.exitValue())
                .as("Robert batch process exit code")
                .isEqualTo(0);
    }

    @Then("robert batch has discarded the hello messages")
    public void checkDiscardedErrorInBatch() {
        var nbDiscardedHello = processLogs
                .stream()
                .filter(line -> line.contains("Could not find keys for id, discarding the hello message"))
                .count();
        assertThat(nbDiscardedHello)
                .as("Robert batch process number of discarded hello message")
                .isGreaterThan(0);
    }
}
