package fr.gouv.stopc.robertserver.crypto.service.model

import fr.gouv.stopc.robertserver.crypto.cipher.encryptUsingAesEcb
import java.nio.ByteBuffer
import java.security.Key
import kotlin.experimental.xor


data class CountryCode(val countryCode: Int) {

    /**
     * Country code is encrypted using the federation key _KG_ and the EBID of the same epoch.
     *
     *     ECCA,i = MSB(AES(KG, EBIDA,i | 0^64)) ⊕ CCA
     *
     * @see [Robert Protocol 1.1](https://github.com/ROBERT-proximity-tracing/documents/blob/master/ROBERT-specification-EN-v1_1.pdf) §4. Generation of the Ephemeral Bluetooth Identifiers
     */
    fun encrypt(federationKey: Key, ebid: Ebid): Ecc {
        val zeroPaddedEbid = ByteBuffer.allocate(128 / 8)
            // 0123456789012345
            // ................
            // ⬆
            .put(ebid.value.toByteArray())
            // ebidebid........
            // ________⬆
            .array()
            // ebidebid00000000

        val firstByteOfEncryptedEbid = zeroPaddedEbid.encryptUsingAesEcb(federationKey)
            .first()

        return Ecc(firstByteOfEncryptedEbid xor countryCode.toByte())
    }
}
