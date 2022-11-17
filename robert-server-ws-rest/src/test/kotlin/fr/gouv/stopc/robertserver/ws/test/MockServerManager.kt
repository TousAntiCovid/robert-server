package fr.gouv.stopc.robertserver.ws.test

import org.mockserver.client.MockServerClient
import org.mockserver.model.ClearType.LOG
import org.mockserver.model.HttpRequest.request
import org.mockserver.model.JsonBody.json
import org.mockserver.verify.VerificationTimes.exactly
import org.springframework.test.context.TestContext
import org.springframework.test.context.TestExecutionListener
import org.testcontainers.containers.MockServerContainer
import org.testcontainers.images.builder.Transferable
import org.testcontainers.shaded.org.awaitility.Awaitility.await
import org.testcontainers.shaded.org.awaitility.pollinterval.FibonacciPollInterval.fibonacci
import org.testcontainers.utility.DockerImageName
import java.io.File
import java.util.concurrent.TimeUnit.SECONDS

/**
 * Starts a mockserver containers with responses looking like orange captcha
 * server, push-notif-server and submission-code-server.
 *
 * @see /docker-compose/mocks/README.md
 */
class MockServerManager : TestExecutionListener {
    override fun beforeTestMethod(testContext: TestContext) {
        listOf(CAPTCHA, PUSH_NOTIF_SERVER, SUBMISSION_CODE_SERVER)
            .forEach { it.clear(request(), LOG) }
    }

    companion object {
        private val CAPTCHA = startMockServer("captcha.json") { container ->
            mapOf(
                "robert-ws.captcha-server.base-url" to "${container.endpoint}/private/api/v1"
            )
        }
        private val PUSH_NOTIF_SERVER = startMockServer("push-notif-server.json") { container ->
            mapOf(
                "robert-ws.push-server-base-url" to "${container.endpoint}/internal/api/v1"
            )
        }
        private val SUBMISSION_CODE_SERVER = startMockServer("submission-code-server.json") { container ->
            mapOf(
                "robert-ws.submission-code-server-base-url" to "${container.endpoint}/api/v1"
            )
        }

        private fun startMockServer(
            stubsFileName: String,
            generateConfigurationToExport: (MockServerContainer) -> Map<String, String>
        ): MockServerClient {
            val container = MockServerContainer(
                DockerImageName.parse("mockserver/mockserver:mockserver-5.14.0")
            )
                .withReuse(true)
                .withEnv("MOCKSERVER_INITIALIZATION_JSON_PATH", "/expectations.json")
                .withCopyToContainer(
                    Transferable.of(File("../docker-compose/mock-server/$stubsFileName").readText()),
                    "/expectations.json"
                )
            container.start()
            generateConfigurationToExport.invoke(container)
                .forEach { System.setProperty(it.key, it.value) }
            return MockServerClient(container.host, container.serverPort)
        }

        fun verifyNoInteractionsWithPushNotifServer() {
            PUSH_NOTIF_SERVER.verifyZeroInteractions()
        }

        fun verifyPushNotifServerReceivedRegisterForToken(token: String, locale: String, timezone: String) {
            verifyPushNotifServerReceivedRegisterForToken(
                """{"token": "$token", "locale": "$locale", "timezone": "$timezone"}"""
            )
        }

        fun verifyPushNotifServerReceivedRegisterForToken(pushInfo: String) {
            await("a POST /internal/api/v1/push-token request on push-notif-server-mock with body $pushInfo")
                .pollInterval(fibonacci())
                .atMost(2, SECONDS)
                .untilAsserted {
                    PUSH_NOTIF_SERVER.verify(
                        request()
                            .withMethod("POST")
                            .withPath("/internal/api/v1/push-token")
                            .withBody(
                                json(pushInfo)
                            ),
                        exactly(1)
                    )
                }
        }

        fun verifyPushNotifServerReceivedUnregisterForToken(token: String) {
            await("a DELETE /internal/api/v1/push-token/$token request on push-notif-server-mock")
                .pollInterval(fibonacci())
                .atMost(2, SECONDS)
                .untilAsserted {
                    PUSH_NOTIF_SERVER.verify(
                        request()
                            .withMethod("DELETE")
                            .withPath("/internal/api/v1/push-token/$token"),
                        exactly(1)
                    )
                }
        }

        fun verifyNoInteractionsWithSubmissionCodeServer() {
            SUBMISSION_CODE_SERVER.verifyZeroInteractions()
        }
    }
}
