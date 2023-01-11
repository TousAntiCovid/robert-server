package fr.gouv.stopc.robertserver.crypto

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import java.net.URI
import java.time.Duration

@ConstructorBinding
@ConfigurationProperties(prefix = "robert-crypto")
data class RobertCryptoProperties(

    /**
     * The port the GRPC service should listen to.
     */
    val serverPort: Int,

    /**
     * The service start date, ex: `2020-06-01`.
     */
    val serviceStartDate: String,

    /**
     * The keystore password.
     */
    val keystorePassword: String,

    /**
     * The path to the keystore configuration file, ex: `/config/SoftHSMv2/softhsm2.cfg`.
     */
    val keystoreConfigurationUri: URI,

    /**
     * Maximum time drift error hypothesis the application can make to attempt to find the _server-key_ to use to decrypt ebids, ex: `3m`.
     *
     * Two devices have 9s clock drift. A hello message emitted today at 00:00:03 is received yesterday at 23:59:54.
     * The EBID of the hello message is encrypted with today's server-key. But the server will see the message has been
     * received yesterday and try to decrypt it with yesterday's server-key, which will result in invalid data.
     *
     * Setting this parameter to `3m` will let the server attempt to decrypt the EBID with a key from an adjacent day < 3m.
     */
    val helloMessageTimestampTolerance: Duration
)
