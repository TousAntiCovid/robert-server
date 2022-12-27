package fr.gouv.stopc.robertserver.crypto.cipher

import fr.gouv.stopc.robertserver.common.base64Decode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import javax.crypto.spec.SecretKeySpec
import kotlin.random.Random

class Skinny64CipherTest {

    private val key1 = "7QDIWxINaGGHU+JL/ZCPYLLbtBtCLfzQ"
        .base64Decode()
        .let { SecretKeySpec(it, "Skinny64") }

    private val key2 = "yFsSDWjiS/2QYYdTj2Cy27QbQi380O0A"
        .base64Decode()
        .let { SecretKeySpec(it, "Skinny64") }

    private val clearText = "Uwxh016GY8M=".base64Decode()
    private val encryptedText1 = "3SzxqPMwMDw=".base64Decode()
    private val encryptedText2 = "S9yv/0Z6gCk=".base64Decode()

    @Test
    fun can_decrypt_using_key1() {
        val cipher = Skinny64Cipher(key1)
        assertThat(cipher.decrypt(encryptedText1))
            .isEqualTo(clearText)
    }

    @Test
    fun can_decrypt_using_key2() {
        val cipher = Skinny64Cipher(key2)
        assertThat(cipher.decrypt(encryptedText2))
            .isEqualTo(clearText)
    }

    @Test
    fun can_encrypt_using_key1() {
        val cipher = Skinny64Cipher(key1)
        assertThat(cipher.encrypt(clearText))
            .isEqualTo(encryptedText1)
    }

    @Test
    fun can_encrypt_using_key2() {
        val cipher = Skinny64Cipher(key2)
        assertThat(cipher.encrypt(clearText))
            .isEqualTo(encryptedText2)
    }

    @RepeatedTest(20)
    fun can_reverse_encryption() {
        val key = SecretKeySpec(Random.nextBytes(24), "Skinny64")
        val cipher = Skinny64Cipher(key)

        val data = Random.nextBytes(8)
        val encrypted = cipher.encrypt(data)
        val decrypted = cipher.decrypt(encrypted)

        assertThat(data)
            .isEqualTo(decrypted)
    }
}
