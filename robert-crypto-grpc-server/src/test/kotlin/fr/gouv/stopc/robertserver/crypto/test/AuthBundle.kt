package fr.gouv.stopc.robertserver.crypto.test

import com.google.protobuf.ByteString
import fr.gouv.stopc.robert.server.common.DigestSaltEnum
import fr.gouv.stopc.robert.server.common.utils.ByteUtils
import fr.gouv.stopc.robertserver.common.RobertClock.RobertInstant
import fr.gouv.stopc.robertserver.common.base64Encode
import java.time.temporal.ChronoUnit.DAYS
import java.time.temporal.ChronoUnit.MINUTES
import java.util.Base64
import kotlin.random.Random.Default

fun valid_auth_bundle(): List<AuthBundle> = DigestSaltEnum.values()
    .flatMap(::valid_auth_bundle)

fun valid_auth_bundle(requestType: DigestSaltEnum): List<AuthBundle> {
    val now = clock.now()
    return listOf(
        AuthBundle("regular auth attributes", requestType, time = now),
        AuthBundle("use current time but epoch-1", requestType, time = now, epochId = now.asEpochId() - 1),
        AuthBundle("use current time but epoch-10", requestType, time = now, epochId = now.asEpochId() - 10),
        AuthBundle(
            "use current time but epoch-2days",
            requestType,
            time = now,
            epochId = now.minus(2, DAYS).asEpochId()
        ),
        AuthBundle("use current time but epoch+1", requestType, time = now, epochId = now.asEpochId() + 1),
        AuthBundle("use current time but epoch+10", requestType, time = now, epochId = now.asEpochId() + 10),
        AuthBundle(
            "use current time but epoch+2days",
            requestType,
            time = now,
            epochId = now.plus(2, DAYS).asEpochId()
        ),
        AuthBundle(
            "use current epoch but time-2days",
            requestType,
            time = now.minus(2, DAYS),
            epochId = now.asEpochId()
        ),
        AuthBundle(
            "use current epoch but time-10min",
            requestType,
            time = now.minus(10, MINUTES),
            epochId = now.asEpochId()
        ),
        AuthBundle(
            "use current epoch but time+10min",
            requestType,
            time = now.plus(10, MINUTES),
            epochId = now.asEpochId()
        ),
        AuthBundle(
            "use current epoch but time+2days",
            requestType,
            time = now.plus(2, DAYS),
            epochId = now.asEpochId()
        ),
        AuthBundle(
            "use current epoch but time at NTP timestamp=0",
            requestType,
            time = clock.atNtpTimestamp(0),
            epochId = now.asEpochId()
        )
    )
}

data class AuthBundle(
    val title: String,
    val requestType: DigestSaltEnum,
    val idA: String = Default.nextBytes(5).base64Encode(),
    val time: RobertInstant,
    val epochId: Int = time.asEpochId()
) {

    /**
     * Assemble and encrypt epochId and idA to return an EBID.
     *
     *
     * The unencrypted EBID is made of 8 bytes.
     *
     *     +---------------------------------+
     *     | Unencrypted EBID                |
     *     +------------+--------------------+
     *     | epochId    | idA                |
     *     |  (24 bits) |          (40 bits) |
     *     +------------+--------------------+
     *
     * @see [Robert Protocol 1.1](https://github.com/ROBERT-proximity-tracing/documents/blob/master/ROBERT-specification-EN-v1_1.pdf) §4
     */
    val ebid: ByteString
        get() {
            val idABytes = Base64.getDecoder().decode(idA)
            val ebid = ByteArray(8)
            System.arraycopy(ByteUtils.intToBytes(epochId), 1, ebid, 0, 3)
            System.arraycopy(idABytes, 0, ebid, 3, 5)
            val encryptedEbid = cipherForEbidAtEpoch(epochId).encrypt(ebid)
            return ByteString.copyFrom(encryptedEbid)
        }

    /**
     * Computes the MAC for this EBID, epochId and time.
     *
     *     +----------------------------------------------------------------+
     *     |                    MAC structure (128 bits)                    |
     *     +----------------------------------------------------------------+
     *     | Req type     | EBID           | Epoch          | Time          |
     *     |     (8 bits) |      (64 bits) |      (32 bits) |     (32 bits) |
     *     +----------------------------------------------------------------+
     *
     *
     * @see [Robert Protocol 1.1](https://github.com/ROBERT-proximity-tracing/documents/blob/master/ROBERT-specification-EN-v1_1.pdf) §7 and §C
     */
    val mac: ByteString
        get() {
            val ebid = ebid.toByteArray()
            val data = ByteArray(1 + 8 + Integer.BYTES + Integer.BYTES)
            data[0] = requestType.value
            System.arraycopy(ebid, 0, data, 1, 8)
            System.arraycopy(ByteUtils.intToBytes(epochId), 0, data, 1 + ebid.size, Integer.BYTES)
            System.arraycopy(time.asTime32(), 0, data, 1 + ebid.size + Integer.BYTES, Integer.BYTES)
            val mac: ByteArray = getCipherForMac(idA).encrypt(data)
            return ByteString.copyFrom(mac)
        }

    override fun toString() = "$title (idA=$idA, requestType=$requestType, time=$time, epochId=$epochId)"
}
