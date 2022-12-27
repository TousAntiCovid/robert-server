package fr.gouv.stopc.robertserver.crypto.cipher

interface RobertCipher {

    fun encrypt(clearData: ByteArray) :ByteArray

    fun decrypt(encryptedData: ByteArray) :ByteArray
}
