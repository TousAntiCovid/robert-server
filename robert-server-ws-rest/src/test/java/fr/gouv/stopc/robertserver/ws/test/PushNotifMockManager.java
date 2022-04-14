package fr.gouv.stopc.robertserver.ws.test;

import fr.gouv.stopc.robertserver.ws.vo.PushInfoVo;
import org.mockserver.client.MockServerClient;
import org.mockserver.model.JsonBody;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;
import org.testcontainers.containers.MockServerContainer;
import org.testcontainers.utility.DockerImageName;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.mockserver.model.ClearType.LOG;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.verify.VerificationTimes.exactly;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;
import static org.testcontainers.shaded.org.awaitility.pollinterval.FibonacciPollInterval.fibonacci;
import static org.testcontainers.utility.MountableFile.forHostPath;

/**
 * Starts a mockserver container with responses looking like the
 * push-notif-server.
 * <p>
 * See /docker-compose/mocks/README.md
 */
public class PushNotifMockManager implements TestExecutionListener {

    private static final MockServerContainer MOCK_SERVER_CONTAINER = new MockServerContainer(
            DockerImageName.parse("mockserver/mockserver:mockserver-5.13.2")
    )
            .withEnv("MOCKSERVER_INITIALIZATION_JSON_PATH", "/expectations.json")
            .withCopyFileToContainer(
                    forHostPath("../docker-compose/mock-server/push-notif-server.json"),
                    "/expectations.json"
            );

    private static final MockServerClient mockServerClient;

    static {
        MOCK_SERVER_CONTAINER.start();
        System.setProperty("push.server.host", MOCK_SERVER_CONTAINER.getHost());
        System.setProperty("push.server.port", MOCK_SERVER_CONTAINER.getServerPort().toString());

        mockServerClient = new MockServerClient(MOCK_SERVER_CONTAINER.getHost(), MOCK_SERVER_CONTAINER.getServerPort());
    }

    public static void verifyNoInteractionsWithPushNotifServer() {
        mockServerClient.verifyZeroInteractions();
    }

    public static void verifyPushNotifServerReceivedRegisterForToken(PushInfoVo pushInfo) {
        mockServerClient.verify(
                request()
                        .withMethod("POST")
                        .withPath("/internal/api/v1/push-token")
                        .withBody(
                                JsonBody.json(
                                        "{" +
                                                "\"token\": \"" + pushInfo.getToken() + "\"," +
                                                "\"locale\": \"" + pushInfo.getLocale() + "\"," +
                                                "\"timezone\": \"" + pushInfo.getTimezone() + "\"" +
                                                "}"
                                )
                        ),
                exactly(1)
        );
    }

    public static void verifyPushNotifServerReceivedUnregisterForToken(String token) {
        await("a DELETE /internal/api/v1/push-token/" + token + " request on push-notif-server-mock")
                .pollInterval(fibonacci())
                .atMost(2, SECONDS)
                .untilAsserted(
                        () -> mockServerClient.verify(
                                request()
                                        .withMethod("DELETE")
                                        .withPath("/internal/api/v1/push-token/" + token),
                                exactly(1)
                        )
                );
    }

    @Override
    public void beforeTestMethod(TestContext testContext) {
        mockServerClient.clear(request(), LOG);
    }
}
