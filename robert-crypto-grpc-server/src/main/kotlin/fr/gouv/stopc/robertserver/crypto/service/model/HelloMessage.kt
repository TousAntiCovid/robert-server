package fr.gouv.stopc.robertserver.crypto.service.model

import fr.gouv.stopc.robertserver.common.RobertRequestType.HELLO
import fr.gouv.stopc.robertserver.crypto.cipher.hmacSha256
import java.nio.ByteBuffer
import java.security.Key

data class HelloMessage(
    val ecc: Ecc,
    val ebid: Ebid,
    val time: Int
) {

    /**
     * Here is the structure of a HelloMessage:
     *
     *     +----------------------------------------------------------------+
     *     |                    HelloMessage (128 bits)                     |
     *     +----------------------------------------------------------------+
     *     | ECC          | EBID           | Time           | MAC           |
     *     |     (8 bits) |      (64 bits) |      (16 bits) |     (40 bits) |
     *     |     (1 byte) |      (8 bytes) |      (2 bytes) |     (5 bytes) |
     *     +----------------------------------------------------------------+
     *
     * Here is the data structure used to compute the MAC:
     *
     *     +--------------------------------------------------------+
     *     |           HelloMessage MAC (40 bits/12 bytes)          |
     *     +--------------------------------------------------------+
     *     | Salt     | ECC       | EBID           | Time LSB       |
     *     |  (8 bits)|  (8 bits) |      (64 bits) |      (16 bits) |
     *     |  (1 byte)|  (1 byte) |      (8 bytes) |      (2 bytes) |
     *     +--------------------------------------------------------+
     *
     * @see [Robert Protocol 1.1](https://github.com/ROBERT-proximity-tracing/documents/blob/master/ROBERT-specification-EN-v1_1.pdf) §5.1. HELLO Message Broadcasting
     */
    fun computeMac(keyForMac: Key): ByteArray {
        val macInput = ByteBuffer.allocate(12)
            // 012345678901
            // ____________
            // ⬆
            .putInt(8, time)
            // ________tttt
            // ⬆
            .put(HELLO.salt)
            // s_______tttt
            // _⬆
            .put(ecc.value)
            // sc______tttt
            // __⬆
            .put(ebid.value.toByteArray())
            // scebidebidtt
            // __________⬆
            .array()
        val mac = macInput.hmacSha256(keyForMac)
        // truncate to 40 bits / 8 bytes
        return mac.take(5).toByteArray()
    }

    fun verifyMac(keyForMac: Key, actualMac: ByteArray): Boolean {
        val expectedMac = computeMac(keyForMac)
        return actualMac contentEquals expectedMac
    }
}
