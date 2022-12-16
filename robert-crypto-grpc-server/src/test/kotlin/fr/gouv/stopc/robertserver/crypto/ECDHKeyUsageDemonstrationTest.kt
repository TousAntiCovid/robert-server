package fr.gouv.stopc.robertserver.crypto

import org.apache.commons.lang3.builder.ToStringBuilder
import org.apache.commons.lang3.builder.ToStringStyle
import org.assertj.core.api.Assertions.assertThat
import org.bouncycastle.pqc.math.linearalgebra.ByteUtils
import org.junit.jupiter.api.Test
import java.security.KeyPairGenerator
import java.security.spec.ECGenParameterSpec
import javax.crypto.KeyAgreement

/**
 * Demonstrates ECDH key exchange.
 */
internal class ECDHKeyUsageDemonstrationTest {

    @Test
    fun demonstrate_ecdh_key_derivation() {
        val kpg = KeyPairGenerator.getInstance("EC")
        kpg.initialize(ECGenParameterSpec("secp256r1"))
        val serverECDHKey = kpg.generateKeyPair()
        val serverPrivateKey = serverECDHKey.private
        assertThat(serverPrivateKey.format).isEqualTo("PKCS#8")
        assertThat(serverPrivateKey.algorithm).isEqualTo("EC")
        println("Server private key data as hex: " + ByteUtils.toHexString(serverPrivateKey.encoded))
        println("Server private key data as binary: " + ByteUtils.toBinaryString(serverPrivateKey.encoded))
        println(ToStringBuilder.reflectionToString(serverPrivateKey, ToStringStyle.MULTI_LINE_STYLE))

        // use server public key as client public key
        val clientPublicKey = serverECDHKey.public
        assertThat(clientPublicKey.format).isEqualTo("X.509")
        assertThat(clientPublicKey.algorithm).isEqualTo("EC")
        println("Server private key data as hex: " + ByteUtils.toHexString(clientPublicKey.encoded))
        println("Server private key data as binary: " + ByteUtils.toBinaryString(clientPublicKey.encoded))
        println(ToStringBuilder.reflectionToString(clientPublicKey, ToStringStyle.MULTI_LINE_STYLE))
        val ka = KeyAgreement.getInstance("ECDH")
        ka.init(serverPrivateKey)
        ka.doPhase(clientPublicKey, true)
        val generatedSecret = ka.generateSecret()
        println(ToStringBuilder.reflectionToString(ka, ToStringStyle.MULTI_LINE_STYLE))
        println("Generated shared secret: " + ByteUtils.toHexString(generatedSecret))
    }
}
