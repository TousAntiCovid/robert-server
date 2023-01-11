package fr.gouv.stopc.robertserver.crypto.service.model

import fr.gouv.stopc.robertserver.common.base64Encode

const val AUTH_MAC_SIZE = 256 / 8

data class AuthMac(val value: List<Byte>) {

    init {
        if (value.size != AUTH_MAC_SIZE) {
            throw IllegalArgumentException("Authentication MAC should be 256 bits/32 bytes long but is has ${value.size} bytes: $this")
        }
    }

    fun equals(otherMac: ByteArray) = value.toByteArray() contentEquals otherMac

    override fun toString() = value.toByteArray().base64Encode()
}
