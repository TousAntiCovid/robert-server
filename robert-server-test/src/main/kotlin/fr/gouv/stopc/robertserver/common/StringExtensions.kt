package fr.gouv.stopc.robertserver.common

import java.util.Base64

/**
 * Decode this base64 [String] to [ByteArray].
 */
fun String.base64Decode(): ByteArray = Base64.getDecoder().decode(this)

/**
 * Encode this [String] to a Base64 [String].
 */
fun String.base64Encode(): String = Base64.getEncoder().encodeToString(this.toByteArray())

/**
 * Encode this [ByteArray] to a Base64 [String].
 */
fun ByteArray.base64Encode(): String = Base64.getEncoder().encodeToString(this)
