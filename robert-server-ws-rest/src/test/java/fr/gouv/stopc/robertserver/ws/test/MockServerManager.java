package fr.gouv.stopc.robertserver.ws.test;

import fr.gouv.stopc.robertserver.ws.vo.PushInfoVo;
import org.mockserver.client.MockServerClient;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;
import org.testcontainers.containers.MockServerContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.mockserver.model.ClearType.LOG;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.JsonBody.json;
import static org.mockserver.verify.VerificationTimes.exactly;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;
import static org.testcontainers.shaded.org.awaitility.pollinterval.FibonacciPollInterval.fibonacci;
import static org.testcontainers.utility.MountableFile.forHostPath;

/**
 * Starts a mockserver containers with responses looking like orange captcha
 * server, push-notif-server and submission-code-server.
 * <p>
 * See /docker-compose/mocks/README.md
 */
public class MockServerManager implements TestExecutionListener {

    private final static MockServerClient CAPTCHA = startMockServer(
            "captcha.json", container -> Map.of(
                    "robert.captcha.public-base-url", container.getEndpoint() + "/public/api/v1",
                    "robert.captcha.private-base-url", container.getEndpoint() + "/private/api/v1"
            )
    );

    private final static MockServerClient PUSH_NOTIF_SERVER = startMockServer(
            "push-notif-server.json",
            container -> Map.of(
                    "push.server.host", container.getHost(),
                    "push.server.port", container.getServerPort().toString()
            )
    );

    private final static MockServerClient SUBMISSION_CODE_SERVER = startMockServer(
            "submission-code-server.json",
            container -> Map.of(
                    "submission.code.server.url", container.getEndpoint()
            )
    );

    private static MockServerClient startMockServer(String stubsFileName,
            Function<MockServerContainer, Map<String, String>> generateConfigurationToExport) {
        final var container = new MockServerContainer(
                DockerImageName.parse("mockserver/mockserver:mockserver-5.14.0")
        )
                .withEnv("MOCKSERVER_INITIALIZATION_JSON_PATH", "/expectations.json")
                .withCopyFileToContainer(
                        forHostPath("../docker-compose/mock-server/" + stubsFileName),
                        "/expectations.json"
                );
        container.start();

        generateConfigurationToExport.apply(container)
                .forEach(System::setProperty);

        return new MockServerClient(container.getHost(), container.getServerPort());
    }

    @Override
    public void beforeTestMethod(TestContext testContext) {
        List.of(CAPTCHA, PUSH_NOTIF_SERVER, SUBMISSION_CODE_SERVER)
                .forEach(mock -> mock.clear(request(), LOG));
    }

    public static void verifyNoInteractionsWithPushNotifServer() {
        PUSH_NOTIF_SERVER.verifyZeroInteractions();
    }

    public static void verifyPushNotifServerReceivedRegisterForToken(PushInfoVo pushInfo) {
        await(
                "a POST /internal/api/v1/push-token request for token " + pushInfo.getToken()
                        + " on push-notif-server-mock"
        )
                .pollInterval(fibonacci())
                .atMost(2, SECONDS)
                .untilAsserted(
                        () -> PUSH_NOTIF_SERVER.verify(
                                request()
                                        .withMethod("POST")
                                        .withPath("/internal/api/v1/push-token")
                                        .withBody(
                                                json(
                                                        "{" +
                                                                "\"token\": \"" + pushInfo.getToken() + "\"," +
                                                                "\"locale\": \"" + pushInfo.getLocale() + "\"," +
                                                                "\"timezone\": \"" + pushInfo.getTimezone() + "\"" +
                                                                "}"
                                                )
                                        ),
                                exactly(1)
                        )
                );
    }

    public static void verifyPushNotifServerReceivedUnregisterForToken(String token) {
        await("a DELETE /internal/api/v1/push-token/" + token + " request on push-notif-server-mock")
                .pollInterval(fibonacci())
                .atMost(2, SECONDS)
                .untilAsserted(
                        () -> PUSH_NOTIF_SERVER.verify(
                                request()
                                        .withMethod("DELETE")
                                        .withPath("/internal/api/v1/push-token/" + token),
                                exactly(1)
                        )
                );
    }

    public static void verifyNoInteractionsWithSubmissionCodeServer() {
        SUBMISSION_CODE_SERVER.verifyZeroInteractions();
    }

}
