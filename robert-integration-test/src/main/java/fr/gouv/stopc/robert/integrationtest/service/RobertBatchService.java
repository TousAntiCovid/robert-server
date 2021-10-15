package fr.gouv.stopc.robert.integrationtest.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;

import fr.gouv.stopc.robert.integrationtest.config.ApplicationProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class RobertBatchService {

    private static final long BATCH_EXECUTION_TIMEOUT_IN_SECONDS = 30;

    private final ApplicationProperties applicationProperties;

    public void triggerBatch(boolean mustBeAtRisk) throws IOException, InterruptedException {

        final var batchTriggerCommand = applicationProperties.getBatch().getCommand().split(" ");
        final var builder = new ProcessBuilder(batchTriggerCommand);
        if (mustBeAtRisk) {
            builder.environment().put("ROBERT_PROTOCOL_SCORING_THRESHOLD", "0.0001");
        }
        builder.directory(Path.of(".").toFile());
        final var process = builder.start();
        final var background = Executors.newFixedThreadPool(2);
        background.submit(new StreamGobbler(process.getInputStream(), log::debug));
        background.submit(new StreamGobbler(process.getErrorStream(), log::error));
        boolean hasExited = process.waitFor(BATCH_EXECUTION_TIMEOUT_IN_SECONDS, TimeUnit.SECONDS);
        background.shutdownNow();
        if (!hasExited) {
            throw new RuntimeException("Robert batch timeout");
        }
        if (process.exitValue() != 0) {
            throw new RuntimeException("Robert batch  failed");
        }
    }

    private static class StreamGobbler implements Runnable {

        private final InputStream inputStream;

        private final Consumer<String> consumer;

        public StreamGobbler(InputStream inputStream, Consumer<String> consumer) {
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
