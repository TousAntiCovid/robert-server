package fr.gouv.stopc.robertserver.crypto.cipher

import fr.gouv.stopc.robert.server.crypto.structure.impl.CryptoAESGCM
import fr.gouv.stopc.robertserver.common.base64Decode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import javax.crypto.AEADBadTagException
import javax.crypto.spec.SecretKeySpec
import kotlin.experimental.xor
import kotlin.random.Random
import kotlin.random.nextInt

class AesGcmCipherTest {

    private val secretKey = "256 bits AES key for java unit tests"
        .toByteArray()
        .take(256 / 8)
        .let { SecretKeySpec(it.toByteArray(), "AES") }

    private val cipher = AesGcmCipher(secretKey)

    @Test
    fun can_encrypt_data() {
        // can't use a fixed value due to random IV, then we ensure the operation is reversible
        val clearData = "clear text message".toByteArray()
        val encryptedData = cipher.encrypt(clearData)

        assertThat(encryptedData)
            .hasSize(clearData.size + 12 + 16)

        val decryptedData = cipher.decrypt(encryptedData)
        assertThat(decryptedData.decodeToString())
            .isEqualTo("clear text message")
    }

    @Test
    fun can_decrypt_data() {
        val encryptedData = "zF95PZaO7BKhWMzcX7jFvppalbEvBLbuynL0gDMUcstviUdyPFLEgDDCy0qb2A==".base64Decode()
        assertThat(
            cipher.decrypt(encryptedData)
                .decodeToString()
        )
            .isEqualTo("clear text message")
    }

    @Test
    fun decrypt_should_fail_when_iv_is_corrupt() {
        val encryptedData = cipher.encrypt("clear text message".toByteArray())

        // Change a bit from the IV
        encryptedData[3] = encryptedData[3] xor 0x8

        assertThrows<AEADBadTagException> { cipher.decrypt(encryptedData) }
    }

    @RepeatedTest(20)
    fun can_encrypt_then_decrypt_random_data() {
        val length = Random.nextInt(100..2000)
        val clearData = Random.nextBytes(length)

        val encryptedData = cipher.encrypt(clearData)

        assertThat(encryptedData)
            .hasSize(clearData.size + 12 + 16)

        val decryptedData = cipher.decrypt(encryptedData)
        assertThat(decryptedData)
            .isEqualTo(clearData)
    }

    @Nested
    inner class Compatibility {

        private val legacyTooling = CryptoAESGCM(secretKey.encoded)

        @Test
        fun decrypt_should_produce_same_result_as_legacy_tooling() {
            val encryptedData = "zF95PZaO7BKhWMzcX7jFvppalbEvBLbuynL0gDMUcstviUdyPFLEgDDCy0qb2A==".base64Decode()
            assertThat(legacyTooling.decrypt(encryptedData).decodeToString())
                .isEqualTo("clear text message")
        }

        @Test
        fun something_encrypted_with_legacy_tooling_should_be_decryptable() {
            val length = Random.nextInt(100..2000)
            val clearData = Random.nextBytes(length)

            val encryptedData = cipher.encrypt(clearData)

            assertThat(encryptedData)
                .hasSize(clearData.size + 12 + 16)

            val decryptedData = legacyTooling.decrypt(encryptedData)
            assertThat(decryptedData)
                .isEqualTo(clearData)
        }
    }
}
