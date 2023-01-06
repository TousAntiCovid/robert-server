package fr.gouv.stopc.robertserver.crypto.service.model

import fr.gouv.stopc.robertserver.common.model.IdA
import fr.gouv.stopc.robertserver.crypto.cipher.encryptUsingSkinny64
import java.nio.ByteBuffer
import java.security.Key

/**
 * An unencrypted _Bluetooth IDentifier_.
 *
 * The bluetooth identifier structure is made of 8 bytes:
 *
 *     +---------------------------------+
 *     | Unencrypted EBID                |
 *     +------------+--------------------+
 *     | epochId    | idA                |
 *     |  (24 bits) |          (40 bits) |
 *     |  (3 bytes) |          (5 bytes) |
 *     +------------+--------------------+
 */
data class BluetoothIdentifier(val epochId: Int, val idA: IdA) {

    /**
     * Encrypts this _Bluetooth IDentifier_ making it an EBID.
     */
    fun encrypt(serverKey: Key): Ebid {
        val bid = ByteBuffer.allocate(8)
            // 01234567
            // ........
            // ⬆
            .putInt(epochId)
            // iiii....
            // ____⬆
            .position(1)
            // iiii....
            // _⬆
            .compact()
            // iii.....
            // ⬆
            .position(3)
            // iii.....
            // ___⬆
            .put(idA.toByteArray())
            // iiiaaaaa
            // ________⬆
            .array()
        val ebid = bid.encryptUsingSkinny64(serverKey)
        return Ebid(ebid.toList())
    }
}
