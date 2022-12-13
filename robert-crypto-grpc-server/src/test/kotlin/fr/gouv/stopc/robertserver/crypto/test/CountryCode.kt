package fr.gouv.stopc.robertserver.crypto.test

import com.google.protobuf.ByteString

enum class CountryCode(val numericCode: Int) {

    GERMANY(49),
    FRANCE(33);

    fun asByteArray() = arrayOf(numericCode.toByte()).toByteArray()

    fun asByteString(): ByteString = ByteString.copyFrom(asByteArray())

    override fun toString() = "$name($numericCode)"
}
