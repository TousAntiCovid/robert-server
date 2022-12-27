package fr.gouv.stopc.robertserver.crypto.service.model

import fr.gouv.stopc.robertserver.common.base64Decode
import fr.gouv.stopc.robertserver.common.base64Encode
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Encrypted Country Code.
 *
 * It's an _encrypted_ [CountryCode] and must be 8 bits long.
 */
@Serializable(with = Ecc.Base64Serializer::class)
data class Ecc(val value: Byte) {

    constructor(base64Value: String) : this(
        base64Value.base64Decode()
            .also { it.size == 1 || throw IllegalArgumentException("ECC should be 8 bits/1 byte long but is has ${it.size} bytes: $base64Value") }
            .first()
    )

    private val base64Value: String
        get() = byteArrayOf(value).base64Encode()

    override fun toString() = base64Value

    object Base64Serializer : KSerializer<Ecc> {
        override val descriptor = PrimitiveSerialDescriptor("ECC", PrimitiveKind.STRING)

        override fun deserialize(decoder: Decoder) = Ecc(decoder.decodeString().base64Decode().first())

        override fun serialize(encoder: Encoder, value: Ecc) = encoder.encodeString(value.base64Value)
    }
}
