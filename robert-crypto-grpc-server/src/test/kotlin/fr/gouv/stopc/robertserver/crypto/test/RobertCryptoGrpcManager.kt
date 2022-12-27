package fr.gouv.stopc.robertserver.crypto.test

import fr.gouv.stopc.robert.crypto.grpc.server.messaging.CryptoGrpcServiceImplGrpc
import fr.gouv.stopc.robert.crypto.grpc.server.messaging.CryptoGrpcServiceImplGrpc.CryptoGrpcServiceImplBlockingStub
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.Server
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.test.context.TestContext
import org.springframework.test.context.TestExecutionListener
import org.springframework.test.context.support.TestPropertySourceUtils

private lateinit var client: CryptoGrpcServiceImplBlockingStub

/**
 * Returns a GRPC client to call operations on running CryptoServer.
 */
fun whenRobertCryptoClient(): CryptoGrpcServiceImplBlockingStub = client

/**
 * Make sure to start the application on a random port and opens/closes a client to send requests to the test server.
 */
class RobertCryptoGrpcManager : TestExecutionListener, ApplicationContextInitializer<ConfigurableApplicationContext> {

    override fun initialize(applicationContext: ConfigurableApplicationContext) {
        TestPropertySourceUtils.addInlinedPropertiesToEnvironment(applicationContext, "robert.crypto.server.port=0")
    }

    override fun beforeTestClass(testContext: TestContext) {
        val serverPort = testContext.applicationContext
            .getBean(Server::class.java)
            .port

        val channel = ManagedChannelBuilder.forTarget("dns:///localhost:$serverPort")
            .usePlaintext()
            .build()
        client = CryptoGrpcServiceImplGrpc.newBlockingStub(channel)
    }

    override fun afterTestClass(testContext: TestContext) {
        (client.channel as ManagedChannel).shutdownNow()
    }
}
