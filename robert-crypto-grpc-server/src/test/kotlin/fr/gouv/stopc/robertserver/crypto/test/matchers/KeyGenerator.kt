package fr.gouv.stopc.robertserver.crypto.test.matchers

import lombok.RequiredArgsConstructor
import java.security.GeneralSecurityException
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.SecureRandom
import java.security.spec.ECGenParameterSpec

@RequiredArgsConstructor
enum class KeyGenerator(private val algorithm: String) {

    ECDH_SECP256R1("EC") {
        override fun initialize(keyPairGenerator: KeyPairGenerator) =
            keyPairGenerator.initialize(ECGenParameterSpec("secp256r1"))
    },

    ECDH_SECP256K1("EC") {
        override fun initialize(keyPairGenerator: KeyPairGenerator) =
            keyPairGenerator.initialize(ECGenParameterSpec("secp256k1"))
    },

    DH_1024("DH") {
        override fun initialize(keyPairGenerator: KeyPairGenerator) =
            keyPairGenerator.initialize(1024, SecureRandom())
    };

    fun generateKeyPair(): KeyPair = try {
        val keyPairGenerator = KeyPairGenerator.getInstance(algorithm)
        initialize(keyPairGenerator)
        keyPairGenerator.generateKeyPair()
    } catch (e: GeneralSecurityException) {
        throw RuntimeException(e)
    }

    protected abstract fun initialize(keyPairGenerator: KeyPairGenerator)
}
