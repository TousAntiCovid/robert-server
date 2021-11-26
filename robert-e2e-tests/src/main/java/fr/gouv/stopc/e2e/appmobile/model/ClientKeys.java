package fr.gouv.stopc.e2e.appmobile.model;

import fr.gouv.stopc.e2e.external.crypto.CryptoHMACSHA256;
import fr.gouv.stopc.e2e.external.crypto.exception.RobertServerCryptoException;
import lombok.RequiredArgsConstructor;
import lombok.Value;

import javax.crypto.KeyAgreement;

import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Value
public class ClientKeys {

    KeyPair keyPair;

    byte[] keyForMac;

    byte[] keyForTuples;

    public static ClientIdentifierBundleBuilder builder(final String base64EncodedPublicKey) {
        final var key = Base64.getDecoder().decode(base64EncodedPublicKey);
        return new ClientIdentifierBundleBuilder(key);
    }

    @RequiredArgsConstructor
    public static class ClientIdentifierBundleBuilder {

        private static final String HASH_MAC = "mac";

        private static final String HASH_TUPLES = "tuples";

        private final byte[] backendPublicKey;

        public ClientKeys build() {
            final var keyPair = generateKeyPair();
            final var sharedSecret = generateSharedSecret(backendPublicKey, keyPair.getPrivate());
            final var hmacSha256 = new CryptoHMACSHA256(sharedSecret);
            try {
                final var kaMac = hmacSha256.encrypt(HASH_MAC.getBytes());
                final var kaTuples = hmacSha256.encrypt(HASH_TUPLES.getBytes());
                return new ClientKeys(keyPair, kaMac, kaTuples);
            } catch (RobertServerCryptoException e) {
                throw new IllegalStateException("Unable to generate client keys", e);
            }
        }

        /**
         * @return an ephemeral ECDH key pair
         */
        private static KeyPair generateKeyPair() {
            try {
                final var generator = KeyPairGenerator.getInstance("EC");
                generator.initialize(new ECGenParameterSpec("secp256r1"), new SecureRandom());
                return generator.generateKeyPair();
            } catch (InvalidAlgorithmParameterException | NoSuchAlgorithmException e) {
                throw new IllegalStateException("Unable to generate ephemeral ECDH key pair", e);
            }

        }

        private static byte[] generateSharedSecret(byte[] backendPublicKey, PrivateKey mobileApplicationPrivateKey) {
            try {
                final var backendPublicKeySpec = new X509EncodedKeySpec(backendPublicKey);
                final var keyFactory = KeyFactory.getInstance("EC");
                final var backendPublicKeyAsKey = keyFactory.generatePublic(backendPublicKeySpec);

                final var keyAgreement = KeyAgreement.getInstance("ECDH");
                keyAgreement.init(mobileApplicationPrivateKey);
                keyAgreement.doPhase(backendPublicKeyAsKey, true);

                return keyAgreement.generateSecret();
            } catch (NoSuchAlgorithmException | InvalidKeySpecException | InvalidKeyException e) {
                throw new IllegalStateException(
                        "Unable to generate a shared secret from backend public key and mobile application key", e
                );
            }
        }

    }
}
