package fr.gouv.stopc.robertserver.ws.service

import com.nimbusds.jose.JOSEObjectType.JWT
import com.nimbusds.jose.JWSAlgorithm.RS256
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import fr.gouv.stopc.robertserver.common.RobertClock
import fr.gouv.stopc.robertserver.common.base64Decode
import fr.gouv.stopc.robertserver.common.base64Encode
import fr.gouv.stopc.robertserver.common.sha256
import fr.gouv.stopc.robertserver.ws.RobertWsProperties
import fr.gouv.stopc.robertserver.ws.service.model.IdA
import org.springframework.stereotype.Service
import java.security.KeyFactory
import java.security.spec.PKCS8EncodedKeySpec
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit.MINUTES
import java.util.Date

/**
 * Builds JWT returned by the Robert API.
 */
@Service
class JwtService(private val config: RobertWsProperties) {

    private val reportValidationJwtSigner = rsaSigner(config.reportValidationJwt.signingKey)

    private val declarationJwtSigner = rsaSigner(config.declarationJwt.signingKey)

    private val analyticsJwtSigner = rsaSigner(config.analyticsJwt.signingKey)

    init {
        // Best effort configuration validation: generate JWTs to attempt to detect private key misconfiguration
        generateReportValidationToken()
        generateAnalyticsToken()
        val clock = RobertClock(LocalDate.now())
        generateDeclarationToken(IdA(ByteArray(5).asList()), RiskStatus.High(clock.now(), clock.now(), clock.now()))
    }

    private fun rsaSigner(signingKey: String): RSASSASigner {
        val keySpec = PKCS8EncodedKeySpec(signingKey.base64Decode())
        val privateKey = KeyFactory.getInstance("RSA")
            .generatePrivate(keySpec)
        return RSASSASigner(privateKey)
    }

    final fun generateReportValidationToken(): String {
        val now = Instant.now()
        val header = JWSHeader.Builder(RS256)
            .type(JWT)
            .build()
        val payload = JWTClaimsSet.Builder()
            .issueTime(Date.from(now))
            .expirationTime(Date.from(now.plus(config.reportValidationJwt.validityDuration)))
            .build()
        val jwt = SignedJWT(header, payload)
        jwt.sign(reportValidationJwtSigner)
        return jwt.serialize()
    }

    final fun generateAnalyticsToken(): String {
        // Truncate IAT and EXP timestamps so all clients making a request within the same minute will get the same token.
        // See TAC-1139
        val now = Instant.now().truncatedTo(MINUTES)
        val header = JWSHeader.Builder(RS256)
            .type(JWT)
            .build()
        val payload = JWTClaimsSet.Builder()
            .issuer("robert-server")
            .issueTime(Date.from(now))
            .expirationTime(Date.from(now.plus(config.analyticsJwt.validityDuration)))
            .build()
        val jwt = SignedJWT(header, payload)
        jwt.sign(analyticsJwtSigner)
        return jwt.serialize()
    }

    final fun generateDeclarationToken(idA: IdA, riskStatus: RiskStatus.High): String {
        val idABase64 = idA.byteValue.toByteArray().base64Encode()
        val riskLevel = 4
        val riskEpochId = riskStatus.lastRiskScoringDate.asEpochId()
        val jti = "$idABase64$riskLevel$riskEpochId"
            .toByteArray()
            .sha256()

        val now = Instant.now()
        val header = JWSHeader.Builder(RS256)
            .type(JWT)
            .keyID(config.declarationJwt.kid)
            .build()
        val payload = JWTClaimsSet.Builder()
            .jwtID(jti)
            .issuer("tac")
            .issueTime(Date.from(now))
            .claim("notificationDateTimestamp", riskStatus.lastStatusRequest.asNtpTimestamp())
            .claim("lastContactDateTimestamp", riskStatus.lastContactDate.asNtpTimestamp())
            .build()
        val jwt = SignedJWT(header, payload)
        jwt.sign(declarationJwtSigner)
        return jwt.serialize()
    }
}
