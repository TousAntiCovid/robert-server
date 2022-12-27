package fr.gouv.stopc.robertserver.common

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
