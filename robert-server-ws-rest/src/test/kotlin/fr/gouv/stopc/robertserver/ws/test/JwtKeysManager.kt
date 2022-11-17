package fr.gouv.stopc.robertserver.ws.test

import org.springframework.test.context.TestExecutionListener
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.NoSuchAlgorithmException
import java.util.Base64

object JwtKeysManager : TestExecutionListener {

    val JWT_KEYS: KeyPair
    val JWT_KEYS_DECLARATION: KeyPair
    val JWT_KEYS_ANALYTICS: KeyPair

    init {
        try {
            val rsaGenerator = KeyPairGenerator.getInstance("RSA")
            JWT_KEYS = rsaGenerator.generateKeyPair()
            JWT_KEYS_DECLARATION = rsaGenerator.generateKeyPair()
            JWT_KEYS_ANALYTICS = rsaGenerator.generateKeyPair()
        } catch (e: NoSuchAlgorithmException) {
            throw IllegalStateException("Unable to generate JWT keys", e)
        }
        System.setProperty("robert.jwt.privatekey", base64PrivateKey(JWT_KEYS))
        System.setProperty("robert.jwt.declare.private-key", base64PrivateKey(JWT_KEYS_DECLARATION))
        System.setProperty("robert.jwt.analytics.token.private-key", base64PrivateKey(JWT_KEYS_ANALYTICS))
    }

    private fun base64PrivateKey(keys: KeyPair): String {
        return Base64.getEncoder()
            .encodeToString(keys!!.private.encoded)
    }
}
