package fr.gouv.stopc.robertserver.crypto.service

import fr.gouv.stopc.robertserver.common.RobertClock
import fr.gouv.stopc.robertserver.common.base64Encode
import fr.gouv.stopc.robertserver.common.logger
import fr.gouv.stopc.robertserver.common.model.randomIdA
import fr.gouv.stopc.robertserver.crypto.cipher.encryptUsingAesGcm
import fr.gouv.stopc.robertserver.crypto.grpc.RobertCryptoException
import fr.gouv.stopc.robertserver.crypto.repository.IdentityRepository
import fr.gouv.stopc.robertserver.crypto.repository.KeyRepository
import fr.gouv.stopc.robertserver.crypto.service.model.Identity
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.springframework.stereotype.Service
import java.security.GeneralSecurityException
import java.security.KeyFactory
import java.security.PublicKey
import java.security.spec.X509EncodedKeySpec
import java.time.Duration
import java.time.temporal.ChronoUnit.DAYS


@Service
class IdentityService(
    private val clock: RobertClock,
    private val keyRepository: KeyRepository,
    private val identityRepository: IdentityRepository
) {
    fun createIdentity(rawClientPublicKey: ByteArray): Identity {
        val clientPublicKey = try {
            KeyFactory.getInstance("EC")
                .generatePublic(X509EncodedKeySpec(rawClientPublicKey))
        } catch (e: GeneralSecurityException) {
            throw RobertCryptoException(400, "Unable to load client public key: ${e.message}")
        }
        return createIdentity(clientPublicKey)
    }

    private fun createIdentity(clientPublicKey: PublicKey): Identity {
        val idA = randomIdA()

        val serverPrivateKey = keyRepository.getServerKeyPair().private
        val keyGenerator = try {
            SecretKeyGenerator(clientPublicKey, serverPrivateKey)
        } catch (e: Exception) {
            throw RobertCryptoException(400, "Unable to derive keys from client public key for client registration: ${e.message}")
        }

        val kek = keyRepository.getKeyEncryptionKey()
        identityRepository.save(
            idA.toBase64String(),
            keyGenerator.keyForMac.encoded
                .encryptUsingAesGcm(kek)
                .base64Encode(),
            keyGenerator.keyForTuples.encoded
                .encryptUsingAesGcm(kek)
                .base64Encode()
        )

        return Identity(idA, keyGenerator.keyForMac, keyGenerator.keyForTuples)
    }

    fun generateEncryptedTuplesBundle(
        identity: Identity,
        countryCode: Int,
        startEpochId: Int,
        bundleDurationInDays: Long
    ): ByteArray {
        val begin = clock.atEpoch(startEpochId)
        val end = begin.truncatedTo(DAYS) + Duration.ofDays(bundleDurationInDays)
        val tuples = RobertTuplesGenerator(countryCode, identity.idA, keyRepository)
            .generate(begin.epochsUntil(end).toList())
        if (tuples.isEmpty()) {
            throw RobertCryptoException(500, "0 ephemeral tuples were generated")
        }
        return Json.encodeToString(tuples)
            .encryptUsingAesGcm(identity.keyForTuples)
    }

}
