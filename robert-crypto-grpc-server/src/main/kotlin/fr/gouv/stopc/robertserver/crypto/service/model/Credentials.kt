package fr.gouv.stopc.robertserver.crypto.service.model

import fr.gouv.stopc.robertserver.common.RobertClock.RobertInstant
import fr.gouv.stopc.robertserver.common.RobertRequestType
import fr.gouv.stopc.robertserver.crypto.cipher.hmacSha256
import java.nio.ByteBuffer
import java.security.Key

data class Credentials(
    val type: RobertRequestType,
    val epochId: Int,
    val time: RobertInstant,
    val ebid: Ebid,
    val mac: AuthMac
) {
    /**
     * Computes the MAC for this EBID, epochId and time.
     *
     *     +----------------------------------------------------------------+
     *     |            Input data structure for MAC (128 bits)             |
     *     +----------------------------------------------------------------+
     *     | Req type     | EBID           | Epoch          | Time          |
     *     |     (8 bits) |      (64 bits) |      (32 bits) |     (32 bits) |
     *     |     (1 byte) |      (8 bytes) |      (4 bytes) |     (4 bytes) |
     *     +----------------------------------------------------------------+
     *
     * @see [Robert Protocol 1.1](https://github.com/ROBERT-proximity-tracing/documents/blob/master/ROBERT-specification-EN-v1_1.pdf) §7 and §C
     */
    fun hasValidChecksum(keyForMac: Key): Boolean {
        val input = ByteBuffer.allocate(17)
            // 0 12345678 9012 3456
            // .|........|....|....
            // ⬆
            .put(type.salt)
            // 0 12345678 9012 3456
            // s|........|....|....
            // __⬆
            .put(ebid.value.toByteArray())
            // 0 12345678 9012 3456
            // s|ebidebid|....|....
            // ___________⬆
            .putInt(epochId)
            // 0 12345678 9012 3456
            // s|ebidebid|iiii|....
            // ________________⬆
            .put(time.asTime32())
            // 0 12345678 9012 3456
            // s|ebidebid|iiii|tttt
            // ____________________⬆
            .array()

        val actualMac = input.hmacSha256(keyForMac)
        return mac.equals(actualMac)
    }
}
