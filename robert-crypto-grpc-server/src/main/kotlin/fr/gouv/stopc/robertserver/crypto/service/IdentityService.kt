package fr.gouv.stopc.robertserver.crypto.service

import fr.gouv.stopc.robertserver.common.RobertClock
import fr.gouv.stopc.robertserver.common.model.IdA
import fr.gouv.stopc.robertserver.common.model.randomIdA
import fr.gouv.stopc.robertserver.crypto.cipher.encryptUsingAesGcm
import fr.gouv.stopc.robertserver.crypto.grpc.RobertGrpcException
import fr.gouv.stopc.robertserver.crypto.repository.IdentityRepository
import fr.gouv.stopc.robertserver.crypto.repository.KeyRepository
import fr.gouv.stopc.robertserver.crypto.service.model.Credentials
import fr.gouv.stopc.robertserver.crypto.service.model.Identity
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.springframework.stereotype.Service
import java.security.GeneralSecurityException
import java.security.KeyFactory
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
            throw RobertGrpcException(400, "Unable to load client public key", "${e.message}")
        }

        val idA = randomIdA()

        val serverPrivateKey = keyRepository.getServerKeyPair().private
        val keyGenerator = try {
            SecretKeyGenerator(clientPublicKey, serverPrivateKey)
        } catch (e: Exception) {
            throw RobertGrpcException(400, "Unable to derive keys from client public key", "${e.message}")
        }

        val identity = Identity(idA, keyGenerator.keyForMac, keyGenerator.keyForTuples)

        val kek = keyRepository.getKeyEncryptionKey()
        val encryptedIdentity = identity.encrypt(kek)
        identityRepository.save(encryptedIdentity)

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
            throw RobertGrpcException(500, "Internal error", "0 ephemeral tuples were generated")
        }
        return Json.encodeToString(tuples)
            .encryptUsingAesGcm(identity.keyForTuples)
    }

    fun authenticate(credentials: Credentials): IdA {
        val date = clock.atEpoch(credentials.epochId).toUtcLocalDate()
        val serverKey = keyRepository.getServerKey(date)
            ?: throw RobertGrpcException(430, "Missing server key", "No server key for $date")
        // 3. retrieves the permanent identifier idA and epoch i'A by decrypting ebidA
        val bid = credentials.ebid.decrypt(serverKey)
        // 4. verifies that iA == i'A
        if (bid.epochId != credentials.epochId) {
            throw RobertGrpcException(
                400,
                "Could not decrypt EBID content",
                "Request epoch ${credentials.epochId} and EBID epoch ${bid.epochId} don't match"
            )
        }
        // 5. uses idA to retrieve from IDTable the associated entries: KauthA , UNA, SREA, LEEA
        val kek = keyRepository.getKeyEncryptionKey()
        val identity = identityRepository.findByIdA(bid.idA.toBase64String())
            ?.decrypt(kek)
            ?: throw RobertGrpcException(404, "Could not find idA", "IdA contained in EBID(${credentials.ebid}) was not found in database")
        // 6. verifies that macA,i == HMAC − SHA256(KauthA , c2 | ebidA | iA | tA)
        if (!credentials.hasValidChecksum(identity.keyForMac)) {
            throw RobertGrpcException(400, "Invalid MAC", "${credentials.mac} don't match expected checksum")
        }
        return identity.idA
    }

    fun delete(idA: IdA) = identityRepository.deleteByIdA(idA.toBase64String())
}
