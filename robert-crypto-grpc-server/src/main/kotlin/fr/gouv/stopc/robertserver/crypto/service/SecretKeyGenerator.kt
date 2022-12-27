package fr.gouv.stopc.robertserver.crypto.service

import java.security.PrivateKey
import java.security.PublicKey
import javax.crypto.KeyAgreement
import javax.crypto.Mac
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

private const val KEY_AGREEMENT_ALGORITHM_ECDH = "ECDH"
private const val GENERATED_KEY_ALGORITHM_AES = "AES"

private const val HMAC_ALGORITHM_SHA_256 = "HmacSHA256"
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
        val mac = Mac.getInstance(HMAC_ALGORITHM_SHA_256).apply {
            init(sharedKey)
        }
        keyForMac = SecretKeySpec(mac.doFinal(HASH_MAC.toByteArray()), GENERATED_KEY_ALGORITHM_AES)
        keyForTuples = SecretKeySpec(mac.doFinal(HASH_TUPLES.toByteArray()), GENERATED_KEY_ALGORITHM_AES)
    }
}
