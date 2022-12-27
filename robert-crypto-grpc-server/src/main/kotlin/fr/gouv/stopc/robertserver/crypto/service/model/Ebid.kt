package fr.gouv.stopc.robertserver.crypto.service.model

import fr.gouv.stopc.robertserver.common.base64Decode
import fr.gouv.stopc.robertserver.common.base64Encode
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind.STRING
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

// An EBID is 64 bits long
private const val EBID_SIZE = 64 / 8

/**
 * EBID stands for _Encrypted Bluetooth IDentifier_.
 *
 * It's an _encrypted_ [BluetoothIdentifier] and must be 64 bits long.
 */
@Serializable(with = Ebid.Base64Serializer::class)
data class Ebid(val value: List<Byte>) {

    init {
        if (value.size != EBID_SIZE) {
            throw IllegalArgumentException("EBID should be 64 bits/8 bytes long but is has ${value.size} bytes: $this")
        }
    }

    constructor(base64Ebid: String) : this(base64Ebid.base64Decode().toList())

    private val base64Value
        get() = value.toByteArray().base64Encode()

    override fun toString() = base64Value

    object Base64Serializer : KSerializer<Ebid> {
        override val descriptor = PrimitiveSerialDescriptor("EBID", STRING)

        override fun deserialize(decoder: Decoder) = Ebid(decoder.decodeString().base64Decode().toList())

        override fun serialize(encoder: Encoder, value: Ebid) = encoder.encodeString(value.base64Value)
    }
}
