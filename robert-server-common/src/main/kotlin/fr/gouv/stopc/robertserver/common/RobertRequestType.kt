package fr.gouv.stopc.robertserver.common

/**
 * Robert protocol request type value.
 *
 * @see <a href="https://github.com/ROBERT-proximity-tracing/documents/blob/master/ROBERT-specification-EN-v1_1.pdf">Robert protocol</a> C. Authenticated Requests
 */
enum class RobertRequestType(val salt: Byte) {

    HELLO(1),
    STATUS(2),
    UNREGISTER(3),
    DELETE_HISTORY(4);

    companion object {

        @JvmStatic
        fun fromValue(salt: Int) = values().find { it.salt.toInt() == salt }

        @JvmStatic
        fun fromValue(salt: Byte) = values().find { it.salt == salt }
    }
}
