package fr.gouv.stopc.robertserver.crypto.cipher

import java.security.Key
import javax.crypto.Cipher

private const val AES_ECB_CIPHER_SCHEME = "AES/ECB/NoPadding"
private const val AES_ECB_IV_LENGTH = 96 / 8
private const val AES_ECB_AUTH_TAG_LENGTH = 128 / 8

fun ByteArray.encryptUsingAesEcb(key: Key) = AesEcbCipher(key).encrypt(this)

fun ByteArray.decryptUsingAesEcb(key: Key) = AesEcbCipher(key).decrypt(this)

class AesEcbCipher(private val key: Key) : RobertCipher {

    override fun encrypt(clearData: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(AES_ECB_CIPHER_SCHEME).apply {
            init(Cipher.ENCRYPT_MODE, key)
        }
        return cipher.doFinal(clearData)
    }

    override fun decrypt(encryptedData: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(AES_ECB_CIPHER_SCHEME).apply {
            init(Cipher.DECRYPT_MODE, key)
        }
        return cipher.doFinal(encryptedData)
    }
}
