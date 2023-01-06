package fr.gouv.stopc.robertserver.crypto.service

import fr.gouv.stopc.robertserver.crypto.cipher.HMAC_ALGORITHM_SHA_256
import fr.gouv.stopc.robertserver.crypto.cipher.hmacSha256
import java.security.PrivateKey
import java.security.PublicKey
import javax.crypto.KeyAgreement
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

private const val KEY_AGREEMENT_ALGORITHM_ECDH = "ECDH"
const val GENERATED_KEY_ALGORITHM_AES = "AES"

private const val HASH_MAC = "mac"
private const val HASH_TUPLES = "tuples"

class SecretKeyGenerator(clientPublicKey: PublicKey, serverPrivateKey: PrivateKey) {

    val keyForMac: SecretKey

    val keyForTuples: SecretKey

    init {
        val keyAgreement = KeyAgreement.getInstance(KEY_AGREEMENT_ALGORITHM_ECDH).apply {
            init(serverPrivateKey)
            doPhase(clientPublicKey, true)
        }
        val sharedKey = SecretKeySpec(keyAgreement.generateSecret(), HMAC_ALGORITHM_SHA_256)
        keyForMac = SecretKeySpec(HASH_MAC.hmacSha256(sharedKey), GENERATED_KEY_ALGORITHM_AES)
        keyForTuples = SecretKeySpec(HASH_TUPLES.hmacSha256(sharedKey), GENERATED_KEY_ALGORITHM_AES)
    }
}
