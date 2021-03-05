package fr.gouv.stopc.robertserver.utils;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.WaitContainerResultCallback;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
public class DockerUtils {

    private final DockerClient dockerClient;

    public DockerUtils() {

        //TODO METTRE EN CONFIGURATION CETTE CONFIGURATION
        DefaultDockerClientConfig dockerClientConfig = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost("tcp://localhost:2375")
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
    public void testWithDebug(){
        launchRobertServerBatchContainer(BatchMode.SCORE_CONTACTS_AND_COMPUTE_RISK, true, false);
    }

    @Test
    public void testWithoutDebug(){
        launchRobertServerBatchContainer(BatchMode.SCORE_CONTACTS_AND_COMPUTE_RISK, false, true);
    }

    public void launchRobertServerBatchContainer(BatchMode batchMode, boolean isDebug, boolean mustBeAtRisk) {

        //TODO METTRE EN CONFIGURATION LE PASSAGE DE LA VERSION DE L'IMAGE UTILISEE PAR LES TESTS
        //TODO METTRE EN PLACE LA CONSTRUCTION + DEPLOIEMENT DE L'IMAGE DOCKER SUR LE REPOSITORY GIT ==> DANS IC
        List<Image> imageList = dockerClient.listImagesCmd().withImageNameFilter("*robert-server-batch*").exec();

        Image batchServerImage = imageList.get(0);

        List<Container> containers = dockerClient.listContainersCmd()
                .withShowAll(true)
                .withNameFilter(Collections.singletonList("robert-server-batch-functional-test"))
                .exec();
        for (Container container : containers) {

            InspectContainerResponse inspectContainerResponse = dockerClient.inspectContainerCmd(container.getId()).exec();

            log.info("stop and remove container {}-{}", container.getId(), container.getNames());
            if (inspectContainerResponse.getState().getRunning()) {
                dockerClient.stopContainerCmd(container.getId()).exec();
            }
            dockerClient.removeContainerCmd(container.getId()).exec();
        }

        final String scoringThreshold;
        if (mustBeAtRisk) {
            scoringThreshold = "0.0001";
        } else {
            scoringThreshold = "0.1";
        }

        //TODO VOIR COMMENT INJECTER CES VALEURS DE FACON PLUS PROPRE
        List<String> env = new ArrayList<>();
        env.addAll(Arrays.asList("ROBERT_SERVER_DB_HOST=10.0.75.1",
                "ROBERT_SERVER_DB_PORT=27017",
                "ROBERT_CRYPTO_SERVER_HOST=10.0.75.1",
                "ROBERT_CRYPTO_SERVER_PORT=9090",
                "ROBERT_SERVER_COUNTRY_CODE=0x33",
                "ROBERT_PROTOCOL_SCORING_THRESHOLD"+scoringThreshold,
                "ROBERT_SCORING_BATCH_MODE="+batchMode.name()));


        CreateContainerCmd createContainerCmd = dockerClient.createContainerCmd(batchServerImage.getRepoTags()[0])
                .withName("robert-server-batch-functional-test");

        if (isDebug) {
            ExposedPort exposedPort = new ExposedPort(19091);
            Ports portBindings = new Ports();
            portBindings.bind(exposedPort, Ports.Binding.bindPort(19091));
            env.add("JAVA_OPTS=-Xrunjdwp:transport=dt_socket,address=19091,server=y,suspend=y");
            createContainerCmd.withExposedPorts(exposedPort).withHostConfig(HostConfig.newHostConfig().withPortBindings(portBindings));
        }

        createContainerCmd.withEnv(env);


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
