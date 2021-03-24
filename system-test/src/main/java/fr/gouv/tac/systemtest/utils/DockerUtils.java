package fr.gouv.tac.systemtest.utils;

import static fr.gouv.tac.systemtest.config.DockerConfig.*;
import static fr.gouv.tac.systemtest.config.RobertServerBatchConfig.ROBERT_PROTOCOL_SCORING_THRESHOLD_KEY;
import static fr.gouv.tac.systemtest.config.RobertServerBatchConfig.ROBERT_SCORING_BATCH_MODE_KEY;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fr.gouv.tac.systemtest.config.DockerConfig;
import fr.gouv.tac.systemtest.config.RobertServerBatchConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Slf4j
public class DockerUtils {

    public enum BatchMode {
        NONE,
        FULL_REGISTRATION_SCAN_COMPUTE_RISK,
        SCORE_CONTACTS_AND_COMPUTE_RISK,
        PURGE_OLD_EPOCH_EXPOSITIONS;
    }

    public void launchRobertServerBatchContainer(BatchMode batchMode, boolean isDebug, boolean mustBeAtRisk) throws TacSystemTestException {

        final String scoringThreshold;
        if (mustBeAtRisk) {
            scoringThreshold = "0.0001";
        } else {
            scoringThreshold = "0.1";
        }

        Map<String, String> envVarMap = new HashMap<>();
        envVarMap.putAll(RobertServerBatchConfig.getPropertiesAsMap());
        envVarMap.put(ROBERT_PROTOCOL_SCORING_THRESHOLD_KEY, scoringThreshold);
        envVarMap.put(ROBERT_SCORING_BATCH_MODE_KEY, batchMode.name());

        if (isDebug) {
            envVarMap.put("JAVA_OPTS","-Xrunjdwp:transport=dt_socket,address=*:19091,server=y,suspend=y");
        }

        Map<String, String> portBindingMap =  new HashMap<>();
        portBindingMap.put("19091", "19091");

        try {

            String imageName = String.join(":", DockerConfig.getProperty(ROBERT_SERVER_BATCH_IMAGE_NAME_KEY), DockerConfig.getProperty(ROBERT_SERVER_BATCH_IMAGE_VERSION_KEY));

            String[] command = prepareDockerRunCommand(envVarMap, portBindingMap, "compose_tac", imageName, DockerConfig.getProperty(ROBERT_SERVER_BATCH_CONTAINER_NAME_KEY) );

            executeCommand(command);

        } catch (Exception e) {
            log.error("Unexpected error occurred", e);
            throw new TacSystemTestException(e);
        }

    }

    private void executeCommand(String[] command) throws IOException, InterruptedException, TacSystemTestException {
        ProcessBuilder pb = new ProcessBuilder(command);
        Process proc = pb.start();

        try (InputStream is = proc.getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            log.info("docker logs : \n ################# \n");

            String line = "";
            while ((line = reader.readLine()) != null) {
                log.info(line + "\n");
            }
        }

        proc.waitFor();

        log.info("exit Status : {} ", proc.exitValue());
        if (proc.exitValue() != 0) {

            try (InputStream errorStream = proc.getErrorStream();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(errorStream))) {
                log.error("docker error logs : \n ?????????????????? \n");

                String line = "";
                while ((line = reader.readLine()) != null) {
                    log.error(line + "\n");
                }
            }

            throw new TacSystemTestException("Failed process from command : "+ String.join(" ", command));
        }

    }


    private String[] prepareDockerRunCommand (Map<String, String> envVarMap, Map<String, String> portBindingMap, String netWork, String imageName, String containerName) {

        if (StringUtils.isEmpty(containerName)) {
            throw new IllegalArgumentException("containerName must not be empty nor null !");
        }

        if (StringUtils.isEmpty(imageName)) {
            throw new IllegalArgumentException("imageName must not be empty nor null !");
        }

        List<String> commandAsList = new ArrayList<>();
        commandAsList.addAll(Arrays.asList("docker", "run", "-i", "--rm", "--name", containerName));
        envVarMap.entrySet().stream().forEach(e -> {
            commandAsList.add("-e");
            commandAsList.add(String.join("=", e.getKey(), e.getValue()));
        });

        if (StringUtils.isNotEmpty(netWork)) {
            commandAsList.add("--net");
            commandAsList.add(netWork);
        }

        portBindingMap.entrySet().stream().forEach(e -> {
            commandAsList.add("-p");
            commandAsList.add(String.join(":", e.getKey(), e.getValue()));
        });

        commandAsList.add(imageName);

        String[] command = new String[commandAsList.size()];
        commandAsList.toArray(command);

        log.info("docker command is : {}", String.join(" ", command));

        return command;
    }
}
