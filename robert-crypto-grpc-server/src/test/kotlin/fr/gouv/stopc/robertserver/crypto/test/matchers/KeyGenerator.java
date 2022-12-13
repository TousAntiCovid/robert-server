package fr.gouv.stopc.robertserver.crypto.test.matchers;

import lombok.RequiredArgsConstructor;

import java.security.*;
import java.security.spec.ECGenParameterSpec;

@RequiredArgsConstructor
public enum KeyGenerator {

    ECDH_secp256r1("EC") {

        @Override
        protected void initialize(KeyPairGenerator keyPairGenerator) throws GeneralSecurityException {
            keyPairGenerator.initialize(new ECGenParameterSpec("secp256r1"));
        }
    },
    ECDH_secp256k1("EC") {

        @Override
        protected void initialize(KeyPairGenerator keyPairGenerator) throws GeneralSecurityException {
            keyPairGenerator.initialize(new ECGenParameterSpec("secp256k1"));
        }
    },
    DH_1024("DH") {

        @Override
        protected void initialize(KeyPairGenerator keyPairGenerator) {
            keyPairGenerator.initialize(1024, new SecureRandom());
        }
    };

    private final String algorithm;

    public KeyPair generateKeyPair() {
        try {
            final var keyPairGenerator = KeyPairGenerator.getInstance(algorithm);
            initialize(keyPairGenerator);
            return keyPairGenerator.generateKeyPair();
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }

    protected abstract void initialize(KeyPairGenerator keyPairGenerator) throws GeneralSecurityException;
}
