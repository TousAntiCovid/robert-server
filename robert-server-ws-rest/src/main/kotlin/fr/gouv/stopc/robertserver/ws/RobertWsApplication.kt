package fr.gouv.stopc.robertserver.ws

import fr.gouv.stopc.captchaserver.api.CaptchaApi
import fr.gouv.stopc.pushserver.api.PushTokenApi
import fr.gouv.stopc.robert.crypto.grpc.server.messaging.CryptoGrpcServiceImplGrpc
import fr.gouv.stopc.robert.crypto.grpc.server.messaging.CryptoGrpcServiceImplGrpc.CryptoGrpcServiceImplBlockingStub
import fr.gouv.stopc.robertserver.common.RobertClock
import fr.gouv.stopc.submissioncode.api.SubmissionCodeApi
import io.grpc.ManagedChannelBuilder
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories
import fr.gouv.stopc.captchaserver.ApiClient as CaptchaApiClient
import fr.gouv.stopc.pushserver.ApiClient as PushApiClient
import fr.gouv.stopc.submissioncode.ApiClient as SubmissionApiClient

@SpringBootApplication
@EnableMongoRepositories
@EnableConfigurationProperties(RobertWsProperties::class)
class RobertWsApplication(
    private val config: RobertWsProperties
) {
    @Bean
    fun robertClock() = RobertClock(config.serviceStartDate)

    @Bean
    fun cryptoGrpcService(): CryptoGrpcServiceImplBlockingStub {
        val channel = ManagedChannelBuilder.forTarget(config.cryptoServerUri.toString())
            .defaultServiceConfig(
                mapOf(
                    "loadBalancingConfig" to listOf(
                        mapOf(
                            "round_robin" to emptyMap<Any, Any>()
                        )
                    )
                )
            )
            .usePlaintext()
            .build()
        return CryptoGrpcServiceImplGrpc.newBlockingStub(channel)
    }

    @Bean
    fun pushTokenApi(): PushTokenApi {
        val apiClient = PushApiClient()
            .apply { basePath = config.pushServerBaseUrl.toString() }
        return PushTokenApi(apiClient)
    }

    @Bean
    fun captchaApi(): CaptchaApi {
        val apiClient = CaptchaApiClient()
            .apply { basePath = config.captchaServer.baseUrl.toString() }
        return CaptchaApi(apiClient)
    }

    @Bean
    fun submissionCodeApi(): SubmissionCodeApi {
        val apiClient = SubmissionApiClient()
            .apply { basePath = config.submissionCodeServerBaseUrl.toString() }
        return SubmissionCodeApi(apiClient)
    }
}

fun main(args: Array<String>) {
    runApplication<RobertWsApplication>(*args)
}
