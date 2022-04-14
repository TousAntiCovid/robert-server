package fr.gouv.stopc.robertserver.ws.test;

import org.mockserver.client.MockServerClient;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;
import org.testcontainers.containers.MockServerContainer;
import org.testcontainers.utility.DockerImageName;

import static org.mockserver.model.ClearType.LOG;
import static org.mockserver.model.HttpRequest.request;
import static org.testcontainers.utility.MountableFile.forHostPath;

/**
 * Starts a mockserver container with responses looking like the
 * submission-code-server.
 * <p>
 * See /docker-compose/mocks/README.md
 */
public class SubmissionMockManager implements TestExecutionListener {

    private static final MockServerContainer MOCK_SERVER_CONTAINER = new MockServerContainer(
            DockerImageName.parse("mockserver/mockserver:mockserver-5.13.2")
    )
            .withEnv("MOCKSERVER_INITIALIZATION_JSON_PATH", "/expectations.json")
            .withCopyFileToContainer(
                    forHostPath("../docker-compose/mock-server/submission-code-server.json"),
                    "/expectations.json"
            );

    private static final MockServerClient mockServerClient;

    static {
        MOCK_SERVER_CONTAINER.start();
        System.setProperty("submission.code.server.url", MOCK_SERVER_CONTAINER.getEndpoint());

        mockServerClient = new MockServerClient(MOCK_SERVER_CONTAINER.getHost(), MOCK_SERVER_CONTAINER.getServerPort());
    }

    public static void verifyNoInteractionsWithSubmissionCodeServer() {
        mockServerClient.verifyZeroInteractions();
    }

    @Override
    public void beforeTestMethod(TestContext testContext) throws Exception {
        mockServerClient.clear(request(), LOG);
    }
}
