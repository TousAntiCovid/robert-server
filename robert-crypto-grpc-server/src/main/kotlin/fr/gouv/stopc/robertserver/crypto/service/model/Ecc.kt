package fr.gouv.stopc.robertserver.crypto.service.model

import fr.gouv.stopc.robertserver.common.base64Decode
import fr.gouv.stopc.robertserver.common.base64Encode
import fr.gouv.stopc.robertserver.crypto.cipher.encryptUsingAesEcb
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.nio.ByteBuffer
import java.security.Key
import kotlin.experimental.xor

/**
 * Encrypted Country Code.
 *
 * It's an _encrypted_ [CountryCode] and must be 8 bits long.
 */
@Serializable(with = Ecc.Base64Serializer::class)
data class Ecc(val value: Byte) {

    constructor(base64Value: String) : this(base64Value.base64Decode())

    constructor(arrayValue: ByteArray) : this(
        arrayValue
            .also { it.size == 1 || throw IllegalArgumentException("ECC should be 8 bits/1 byte long but it has ${it.size} bytes: ${it.base64Encode()}") }
            .first()
    )

    private val base64Value: String
        get() = byteArrayOf(value).base64Encode()

    /**
     * Country code is encrypted using the federation key _KG_ and the EBID of the same epoch.
     *
     *     ECCA,i = MSB(AES(KG, EBIDA,i | 0^64)) ⊕ CCA
     *
     * @see [Robert Protocol 1.1](https://github.com/ROBERT-proximity-tracing/documents/blob/master/ROBERT-specification-EN-v1_1.pdf) §4. Generation of the Ephemeral Bluetooth Identifiers
     */
    fun decrypt(federationKey: Key, ebid: Ebid): CountryCode {
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

        val decryptedCountryCode = firstByteOfEncryptedEbid xor value
        return CountryCode(decryptedCountryCode.toInt())
    }

    override fun toString() = base64Value

    object Base64Serializer : KSerializer<Ecc> {
        override val descriptor = PrimitiveSerialDescriptor("ECC", PrimitiveKind.STRING)

        override fun deserialize(decoder: Decoder) = Ecc(decoder.decodeString().base64Decode().first())

        override fun serialize(encoder: Encoder, value: Ecc) = encoder.encodeString(value.base64Value)
    }
}
