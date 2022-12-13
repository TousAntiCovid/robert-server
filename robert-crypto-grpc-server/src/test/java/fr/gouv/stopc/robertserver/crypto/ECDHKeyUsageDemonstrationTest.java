package fr.gouv.stopc.robertserver.crypto;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.bouncycastle.pqc.math.linearalgebra.ByteUtils;
import org.junit.jupiter.api.Test;

import javax.crypto.KeyAgreement;

import java.security.KeyPairGenerator;
import java.security.spec.ECGenParameterSpec;

import static org.assertj.core.api.Assertions.*;

/**
 * Demonstrates ECDH key exchange.
 */
class ECDHKeyUsageDemonstrationTest {

    @Test
    void demonstrate_ecdh_key_derivation() throws Exception {

        final var kpg = KeyPairGenerator.getInstance("EC");
        kpg.initialize(new ECGenParameterSpec("secp256r1"));
        final var serverECDHKey = kpg.generateKeyPair();

        final var serverPrivateKey = serverECDHKey.getPrivate();
        assertThat(serverPrivateKey.getFormat()).isEqualTo("PKCS#8");
        assertThat(serverPrivateKey.getAlgorithm()).isEqualTo("EC");
        System.out.println("Server private key data as hex: " + ByteUtils.toHexString(serverPrivateKey.getEncoded()));
        System.out.println(
                "Server private key data as binary: " + ByteUtils.toBinaryString(serverPrivateKey.getEncoded())
        );
        System.out.println(ToStringBuilder.reflectionToString(serverPrivateKey, ToStringStyle.MULTI_LINE_STYLE));

        // use server public key as client public key
        final var clientPublicKey = serverECDHKey.getPublic();
        assertThat(clientPublicKey.getFormat()).isEqualTo("X.509");
        assertThat(clientPublicKey.getAlgorithm()).isEqualTo("EC");
        System.out.println("Server private key data as hex: " + ByteUtils.toHexString(clientPublicKey.getEncoded()));
        System.out.println(
                "Server private key data as binary: " + ByteUtils.toBinaryString(clientPublicKey.getEncoded())
        );
        System.out.println(ToStringBuilder.reflectionToString(clientPublicKey, ToStringStyle.MULTI_LINE_STYLE));

        final var ka = KeyAgreement.getInstance("ECDH");
        ka.init(serverPrivateKey);
        ka.doPhase(clientPublicKey, true);

        final var generatedSecret = ka.generateSecret();
        System.out.println(ToStringBuilder.reflectionToString(ka, ToStringStyle.MULTI_LINE_STYLE));
        System.out.println("Generated shared secret: " + ByteUtils.toHexString(generatedSecret));

    }
}
