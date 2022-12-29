package fr.gouv.stopc.robertserver.ws.service.model

import com.google.protobuf.ByteString
import fr.gouv.stopc.robertserver.common.base64Encode
import java.io.Serializable

/**
 * An application identifier is a 5 byte value.
 */
data class IdA(
    val byteValue: List<Byte>
) : Serializable {
    init {
        if (byteValue.size != 5) {
            throw IllegalArgumentException("idA must be 5 bytes long, was ${byteValue.toByteArray().base64Encode()}")
        }
    }

    constructor(byteStringValue: ByteString) : this(byteStringValue.toList())

    fun toByteArray() = byteValue.toByteArray()

    override fun toString() = byteValue.toByteArray().base64Encode()
}
