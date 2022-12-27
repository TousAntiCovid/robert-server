package fr.gouv.stopc.robertserver.common.model

import fr.gouv.stopc.robertserver.common.base64Decode
import fr.gouv.stopc.robertserver.common.base64Encode
import java.io.Serializable
import java.util.concurrent.ThreadLocalRandom

/**
 * Generates a random application identifier.
 */
fun randomIdA(): IdA {
    val randomValue = ByteArray(5)
    ThreadLocalRandom.current().nextBytes(randomValue)
    return IdA(randomValue.asList())
}

/**
 * An application identifier is a 5 byte value.
 *
 * This an immutable wrapper to avoid using [ByteArray]s which are mutable and may have arbitrary length.
 */
data class IdA(
    val byteValue: List<Byte>
) : Serializable {
    init {
        if (byteValue.size != 5) {
            throw IllegalArgumentException("idA must be 5 bytes long, was ${byteValue.toByteArray().base64Encode()}")
        }
    }

    constructor(base64IdA: String) : this(base64IdA.base64Decode().toList())

    fun toByteArray() = byteValue.toByteArray()

    fun toBase64String() = byteValue.toByteArray().base64Encode()

    override fun toString() = toBase64String()
}
