package fr.gouv.stopc.robertserver.common

import org.apache.commons.codec.digest.DigestUtils
import java.util.Base64

/**
 * Computes SHA-256 for this [ByteArray] and returns a [String] HEX representation.
 */
fun ByteArray.sha256(): String = DigestUtils.sha256Hex(this)

/**
 * Encode this [ByteArray] to a Base64 [String].
 */
fun ByteArray.base64Encode(): String = Base64.getEncoder().encodeToString(this)
