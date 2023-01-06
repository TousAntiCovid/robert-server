package fr.gouv.stopc.robertserver.crypto.cipher

import java.security.Key
import javax.crypto.Mac

const val HMAC_ALGORITHM_SHA_256 = "HmacSHA256"

fun ByteArray.hmacSha256(key: Key): ByteArray {
    val mac = Mac.getInstance(HMAC_ALGORITHM_SHA_256).apply {
        init(key)
    }
    return mac.doFinal(this)
}

fun String.hmacSha256(key: Key) = this.toByteArray().hmacSha256(key)
