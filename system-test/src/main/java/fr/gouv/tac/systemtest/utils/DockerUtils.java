package fr.gouv.tac.systemtest.utils;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import fr.gouv.tac.systemtest.config.DockerConfig;
import fr.gouv.tac.systemtest.config.RobertServerBatchConfig;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static fr.gouv.tac.systemtest.config.DockerConfig.*;
import static fr.gouv.tac.systemtest.config.RobertServerBatchConfig.ROBERT_PROTOCOL_SCORING_THRESHOLD_KEY;
import static fr.gouv.tac.systemtest.config.RobertServerBatchConfig.ROBERT_SCORING_BATCH_MODE_KEY;
import static java.util.Optional.ofNullable;

@Slf4j
public class DockerUtils {

    private final DockerClient dockerClient;

    public DockerUtils() {

        DefaultDockerClientConfig dockerClientConfig = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost(DockerConfig.getProperty(DOCKERD_ADDRESS_KEY))
                .build();

        this.dockerClient = DockerClientBuilder.getInstance(dockerClientConfig).build();
    }

    public enum BatchMode {
        NONE,
        FULL_REGISTRATION_SCAN_COMPUTE_RISK,
        SCORE_CONTACTS_AND_COMPUTE_RISK,
        PURGE_OLD_EPOCH_EXPOSITIONS;
    }

    @Test
    public void testWithDebug() {
        launchRobertServerBatchContainer(BatchMode.SCORE_CONTACTS_AND_COMPUTE_RISK, true, false);
    }

    @Test
    public void testWithoutDebug() {
        launchRobertServerBatchContainer(BatchMode.SCORE_CONTACTS_AND_COMPUTE_RISK, false, true);
    }

    public void launchRobertServerBatchContainer(BatchMode batchMode, boolean isDebug, boolean mustBeAtRisk) {

        //TODO METTRE EN PLACE LA CONSTRUCTION + DEPLOIEMENT DE L'IMAGE DOCKER SUR LE REPOSITORY GIT ==> DANS IC

        final String imageNameFilter = String.join(":",
                DockerConfig.getProperty(ROBERT_SERVER_BATCH_IMAGE_NAME_KEY),
                DockerConfig.getProperty(ROBERT_SERVER_BATCH_IMAGE_VERSION_KEY));

        List<Image> imageList = dockerClient.listImagesCmd()
                .withImageNameFilter(imageNameFilter)
                .exec();

        Image batchServerImage = imageList.get(0);

        List<Container> containers = dockerClient.listContainersCmd()
                .withShowAll(true)
                .withNameFilter(Collections.singletonList(DockerConfig.getProperty(ROBERT_SERVER_BATCH_CONTAINER_NAME_KEY)))
                .exec();

        for (Container container : containers) {
            InspectContainerResponse inspectContainerResponse = dockerClient.inspectContainerCmd(container.getId()).exec();
            log.info("stop and remove container {}-{}", container.getId(), container.getNames());

            ofNullable(inspectContainerResponse.getState().getRunning()).ifPresent(containerRunningState -> {
                if (Boolean.TRUE.equals(containerRunningState)) {
                    dockerClient.stopContainerCmd(container.getId()).exec();
                }
            });
            dockerClient.removeContainerCmd(container.getId()).exec();
        }

        final String scoringThreshold;
        if (mustBeAtRisk) {
            scoringThreshold = "0.0001";
        } else {
            scoringThreshold = "0.1";
        }

        List<String> env = RobertServerBatchConfig.getPropertiesAsList();
        env.add(String.join("=", ROBERT_PROTOCOL_SCORING_THRESHOLD_KEY, scoringThreshold));
        env.add(String.join("=", ROBERT_SCORING_BATCH_MODE_KEY, batchMode.name()));

        CreateContainerCmd createContainerCmd = dockerClient.createContainerCmd(batchServerImage.getRepoTags()[0])
                .withName(DockerConfig.getProperty(ROBERT_SERVER_BATCH_CONTAINER_NAME_KEY));

        if (isDebug) {
            env.add("JAVA_OPTS=-Xrunjdwp:transport=dt_socket,address=19091,server=y,suspend=y");
        }

        final ListNetworksCmd listNetworksFilterTacCmd = dockerClient.listNetworksCmd().withNameFilter("tac");
        final List<Network> tacNetworks = listNetworksFilterTacCmd.exec();
        final String tac_network_id = tacNetworks.get(0).getId();

        log.info("Set up container network {} to access mongo container", tac_network_id);
        ExposedPort exposedPort = new ExposedPort(19091);
        Ports portBindings = new Ports();
        portBindings.bind(exposedPort, Ports.Binding.bindPort(19091));
        createContainerCmd.withEnv(env)
                .withHostConfig(HostConfig.newHostConfig().withNetworkMode(tac_network_id).withPortBindings(portBindings))
                .withExposedPorts(exposedPort);

        CreateContainerResponse createContainerResponse = createContainerCmd.exec();

        log.info("start container");
        dockerClient.startContainerCmd(createContainerResponse.getId()).exec();

        log.info("wait end of container");
        WaitContainerResultCallback waitContainerResultCallback = dockerClient.waitContainerCmd(createContainerResponse.getId()).start();

        // TODO VERIFIER COMMENT FAIRE FONCTIONNER CORRECTEMENT LE TIMEOUT
        Integer statusCode = waitContainerResultCallback.awaitStatusCode(20, TimeUnit.MINUTES);

        log.info("statusCode = {}", statusCode);
    }
}
