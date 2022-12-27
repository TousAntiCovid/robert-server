package fr.gouv.stopc.robertserver.crypto

import fr.gouv.stopc.robertserver.common.RobertClock
import fr.gouv.stopc.robertserver.crypto.grpc.RobertCryptoGrpcService
import io.grpc.Server
import io.grpc.ServerBuilder
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.web.ServerProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Profile
import java.security.KeyStore
import java.security.Security
import kotlin.io.path.toPath

@SpringBootApplication
@EnableConfigurationProperties(RobertCryptoProperties::class)
class RobertCryptoApplication(
    private val config: RobertCryptoProperties
) {
    @Bean
    fun robertClock() = RobertClock(config.serviceStartDate)

    @Bean
    fun grpcServer(serverProperties: ServerProperties, robertCryptoGrpcService: RobertCryptoGrpcService): Server =
        ServerBuilder.forPort(serverProperties.port)
            .addService(robertCryptoGrpcService)
            .build()
            .start()

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
