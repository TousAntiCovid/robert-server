package fr.gouv.stopc.robertserver.crypto

import fr.gouv.stopc.robertserver.common.RobertClock
import fr.gouv.stopc.robertserver.crypto.grpc.RobertCryptoGrpcService
import io.grpc.Server
import io.grpc.ServerBuilder
import io.micrometer.core.aop.TimedAspect
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.netty.NettyAutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Profile
import java.security.KeyStore
import java.security.Security
import java.util.concurrent.TimeUnit.SECONDS
import kotlin.io.path.toPath

@SpringBootApplication(exclude = [NettyAutoConfiguration::class])
@EnableConfigurationProperties(RobertCryptoProperties::class)
class RobertCryptoApplication(
    private val config: RobertCryptoProperties
) {

    @Bean
    fun timedAspect(meterRegistry: MeterRegistry) = TimedAspect(meterRegistry)

    @Bean
    fun robertClock() = RobertClock(config.serviceStartDate)

    @Bean(destroyMethod = "shutdown")
    fun grpcServer(robertCryptoGrpcService: RobertCryptoGrpcService): Server =
        ServerBuilder.forPort(config.serverPort)
            .addService(robertCryptoGrpcService)
            .build()
            .start()
            .apply {
                awaitTermination(30, SECONDS)
            }

    @Bean("keystore")
    @Profile("!jks")
    fun softhsmKeystore(): KeyStore {
        val softHsmConfigFilePath = config.keystoreConfigurationUri.toPath().toAbsolutePath().toString()
        val sunProvider = Security.getProvider("SunPKCS11")
            .configure(softHsmConfigFilePath)
        val keystore = KeyStore.getInstance("PKCS11", sunProvider)
        keystore.load(null, config.keystorePassword.toCharArray())
        return keystore
    }

    @Bean("keystore")
    @Profile("jks")
    fun jksKeystore(): KeyStore {
        val jksFilePath = config.keystoreConfigurationUri.toURL()
        val keystore = KeyStore.getInstance("PKCS12")
        keystore.load(jksFilePath.openStream(), config.keystorePassword.toCharArray())
        return keystore
    }
}

fun main(args: Array<String>) {
    runApplication<RobertCryptoApplication>(*args)
}
