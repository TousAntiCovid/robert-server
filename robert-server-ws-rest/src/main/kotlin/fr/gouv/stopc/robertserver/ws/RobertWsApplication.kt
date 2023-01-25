package fr.gouv.stopc.robertserver.ws

import fr.gouv.stopc.captchaserver.api.CaptchaApi
import fr.gouv.stopc.pushserver.api.PushTokenApi
import fr.gouv.stopc.robert.crypto.grpc.server.messaging.CryptoGrpcServiceImplGrpc
import fr.gouv.stopc.robert.crypto.grpc.server.messaging.CryptoGrpcServiceImplGrpc.CryptoGrpcServiceImplBlockingStub
import fr.gouv.stopc.robertserver.common.RobertClock
import fr.gouv.stopc.robertserver.common.logger
import fr.gouv.stopc.submissioncode.api.SubmissionCodeApi
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.internal.DnsNameResolver
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories
import org.springframework.scheduling.TaskScheduler
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import fr.gouv.stopc.captchaserver.ApiClient as CaptchaApiClient
import fr.gouv.stopc.pushserver.ApiClient as PushApiClient
import fr.gouv.stopc.submissioncode.ApiClient as SubmissionApiClient

@SpringBootApplication
@EnableScheduling
@EnableMongoRepositories
@EnableConfigurationProperties(RobertWsProperties::class)
class RobertWsApplication(
    private val config: RobertWsProperties
) {

    private val log = logger()

    @Bean
    fun robertClock() = RobertClock(config.serviceStartDate)

    @Bean(destroyMethod = "shutdown")
    fun cryptoGrpcChannel(): ManagedChannel = ManagedChannelBuilder.forTarget(config.cryptoServerUri.toString())
        .defaultLoadBalancingPolicy("round_robin")
        .usePlaintext()
        .build()

    @Bean
    fun cryptoGrpcService(): CryptoGrpcServiceImplBlockingStub =
        CryptoGrpcServiceImplGrpc.newBlockingStub(cryptoGrpcChannel())

    /**
     * Expose the [DnsNameResolver] of our GRPC [ManagedChannel] to be able to implement our low-cost kubernetes PODs
     * discovery.
     *
     * @see dnsRefreshPeriodicTask
     */
    @Bean
    fun cryptoGrpcDnsResolver(): DnsNameResolver {
        val channel = cryptoGrpcChannel()
        return channel::class.java.declaredFields
            .find { it.name == "nameResolver" }
            ?.apply { isAccessible = true }
            ?.get(channel)
            as DnsNameResolver
    }

    /**
     * This is a low-cost way to enable Kubernetes PODs scale-up discovery: we schedule a forced refresh of the service
     * DNS record every 30s (30s is the default TTL of kubernetes headless service record).
     *
     * ℹ Service Mesh like Istio of Linkerd would be better production solution.
     */
    @Scheduled(fixedDelayString = "30s")
    fun dnsRefreshPeriodicTask(taskScheduler: TaskScheduler) {
        val dnsNameResolver = cryptoGrpcDnsResolver()
        try {
            dnsNameResolver.refresh()
        } catch (e: Exception) {
            log.info("Unable to refresh $dnsNameResolver: ${e.message}")
        }
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
