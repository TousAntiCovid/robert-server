package fr.gouv.stopc.robertserver.crypto.cipher

import fr.gouv.stopc.robert.server.crypto.structure.impl.CryptoAESECB
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

class AesEcbCipherTest {

    private val secretKey = "256 bits AES key for java unit tests"
        .toByteArray()
        .take(256 / 8)
        .let { SecretKeySpec(it.toByteArray(), "AES") }

    private val cipher = AesEcbCipher(secretKey)

    // input must be a multiple of 16 bytes
    private val clearData = "1234567800000000".toByteArray()

    @Test
    fun can_encrypt_data() {
        // can't use a fixed value due to random IV, then we ensure the operation is reversible
        val encryptedData = cipher.encrypt(clearData)

        assertThat(encryptedData)
            .hasSameSizeAs(clearData)

        val decryptedData = cipher.decrypt(encryptedData)
        assertThat(decryptedData.decodeToString())
            .isEqualTo("1234567800000000")
    }

    @Test
    fun can_decrypt_data() {
        val encryptedData = "o4Ttag70OrkzzOaBPySgXA==".base64Decode()
        assertThat(cipher.decrypt(encryptedData).decodeToString())
            .isEqualTo("1234567800000000")
    }

    @RepeatedTest(20)
    fun can_encrypt_then_decrypt_random_data() {
        val length = Random.nextInt(1..20)
        val clearData = Random.nextBytes(16 * length)

        val encryptedData = cipher.encrypt(clearData)

        assertThat(encryptedData)
            .hasSameSizeAs(clearData)

        val decryptedData = cipher.decrypt(encryptedData)
        assertThat(decryptedData)
            .isEqualTo(clearData)
    }

    @Nested
    inner class Compatibility {

        private val legacyTooling = CryptoAESECB(secretKey.encoded)

        @Test
        fun decrypt_should_produce_same_result_as_legacy_tooling() {
            val encryptedData = "o4Ttag70OrkzzOaBPySgXA==".base64Decode()
            assertThat(legacyTooling.decrypt(encryptedData).decodeToString())
                .isEqualTo("1234567800000000")
        }

        @Test
        fun something_encrypted_with_legacy_tooling_should_be_decryptable() {
            val length = Random.nextInt(1..20)
            val clearData = Random.nextBytes(16 * length)

            val encryptedData = legacyTooling.encrypt(clearData)

            assertThat(encryptedData)
                .hasSameSizeAs(clearData)

            val decryptedData = cipher.decrypt(encryptedData)
            assertThat(decryptedData)
                .isEqualTo(clearData)
        }
    }
}
