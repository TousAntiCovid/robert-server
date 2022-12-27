package fr.gouv.stopc.robertserver.crypto.cipher

import fr.gouv.stopc.robert.server.crypto.structure.impl.CryptoSkinny64
import java.security.Key

fun ByteArray.encryptUsingSkinny64(key: Key): ByteArray = CryptoSkinny64(key.encoded).encrypt(this)

class Skinny64Cipher(key: Key) : RobertCipher {

    private val delegate = CryptoSkinny64(key.encoded)

    override fun encrypt(clearData: ByteArray): ByteArray = delegate.encrypt(clearData)

    override fun decrypt(encryptedData: ByteArray): ByteArray = delegate.decrypt(encryptedData)

}
