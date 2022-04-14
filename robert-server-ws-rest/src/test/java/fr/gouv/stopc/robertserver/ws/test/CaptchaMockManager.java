package fr.gouv.stopc.robertserver.ws.test;

import org.springframework.test.context.TestExecutionListener;
import org.testcontainers.containers.MockServerContainer;
import org.testcontainers.utility.DockerImageName;

import static org.testcontainers.utility.MountableFile.forHostPath;

/**
 * Starts a mockserver container with responses looking like orange captcha
 * server.
 * <p>
 * Any request to verify a captcha challenge having an identifier containing
 * <em>success</em> will result in a successful verification. Others will
 * response in a wrong response.
 */
public class CaptchaMockManager implements TestExecutionListener {

    private static final MockServerContainer MOCK_SERVER_CONTAINER = new MockServerContainer(
            DockerImageName.parse("mockserver/mockserver:mockserver-5.13.2")
    )
            .withEnv("MOCKSERVER_INITIALIZATION_JSON_PATH", "/expectations.json")
            .withCopyFileToContainer(forHostPath("../docker-compose/mock-server/captcha.json"), "/expectations.json");

    static {
        MOCK_SERVER_CONTAINER.start();
        System.setProperty("captcha.internal.hostname", MOCK_SERVER_CONTAINER.getEndpoint());
        System.setProperty(
                "captcha.internal.verify.url",
                MOCK_SERVER_CONTAINER.getEndpoint() + "/private/api/v1/captcha/{captchaId}/checkAnswer"
        );
    }
}
