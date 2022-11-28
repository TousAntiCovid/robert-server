package fr.gouv.stopc.robertserver.ws

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import java.net.URI
import java.net.URL
import java.time.Duration

@ConstructorBinding
@ConfigurationProperties(prefix = "robert-ws")
data class RobertWsProperties(

    /**
     * This server country code, ex: `33` for France.
     */
    val countryCode: Short,

    /**
     * The service start date, ex: `2020-06-01`.
     */
    val serviceStartDate: String,

    /**
     * The crypto server URI, ex: `dns:///crypto-server-host.private-network.tld:8080`.
     * @see [io.grpc.ManagedChannelBuilder.forTarget]
     */
    val cryptoServerUri: URI,

    /**
     * The push server base URL, ex: `https://push-server-host.private-network.tdl:8080/internal/api/v1`.
     */
    val pushServerBaseUrl: URL,

    /**
     * Captcha configuration.
     */
    val captchaServer: Captcha,

    /**
     * The submission code server base URL, ex: `https://submission-code-server-host.private-network.tdl:8080/internal/api/v1`.
     */
    val submissionCodeServerBaseUrl: URL,

    /**
     * Maximum time drift between server clock and client request time, ex: simplified `2m` or advanced ISO-8601 `PT2M30S`
     */
    val maxAuthRequestClockSkew: Duration,

    /**
     * Minimum epochs between two `/status` request.
     */
    val minEpochsBetweenStatusRequests: Int,

    /**
     * Ephemeral tuples bundle duration in days (including current day), ex: `5` means _the bundle will contain tuples until J+4 at the end of the day_.
     */
    val ephemeralTuplesBundleDays: Int,

    /**
     * Configuration for the _report validation_ JWT token generation.
     */
    val reportValidationJwt: ReportValidationJwt,

    /**
     * Configuration for the _declaration_ JWT token generation.
     */
    val declarationJwt: DeclarationJwt,

    /**
     * Configuration for the _analytics_ JWT token generation.
     */
    val analyticsJwt: AnalyticsJwt
) {

    data class Captcha(
        /**
         * Enable the captcha protection on /register requests, ex: `yes`
         */
        val enabled: Boolean,

        /**
         * The captcha server base URL, ex: `https://captcha-server-host.private-network.tdl:8080/api/v1`.
         */
        val baseUrl: URL
    )

    data class ReportValidationJwt(

        /**
         * RSA private key to sign _Report Validation_ JWT tokens returned by the `/report` API endpoint, ex: `MIIEv...`.
         *
         * Expecting a single line base64 string of the private key binary representation.
         */
        val signingKey: String,

        /**
         * The validity duration period of the generated _Report Validation_ tokens, ex: simplified `2m` or advanced ISO-8601 `PT2M30S`.
         */
        val validityDuration: Duration
    )

    data class DeclarationJwt(

        /**
         * The kid to use in JWT headers.
         */
        val kid: String,

        /**
         * RSA private key to sign _Declaration_ JWT tokens returned by the `/status` API endpoint, ex: `MIIEv...`.
         *
         * Expecting a single line base64 string of the private key binary representation.
         */
        val signingKey: String
    )
    data class AnalyticsJwt(

        /**
         * RSA private key to sign _Analytics_ JWT tokens returned by the `/status` API endpoint, ex: `MIIEv...`.
         *
         * Expecting a single line base64 string of the private key binary representation.
         */
        val signingKey: String,

        /**
         * The validity duration period of the generated _Analytics_ tokens, ex: simplified `2m` or advanced ISO-8601 `PT2M30S`.
         */
        val validityDuration: Duration
    )
}
