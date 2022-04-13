package fr.gouv.stopc.robertserver.ws.test;

import org.springframework.test.context.TestExecutionListener;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class JwtKeysManager implements TestExecutionListener {

    public static final KeyPair JWT_KEYS;

    public static final KeyPair JWT_KEYS_DECLARATION;

    public static final KeyPair JWT_KEYS_ANALYTICS;

    static {
        try {
            final var rsaGenerator = KeyPairGenerator.getInstance("RSA");
            JWT_KEYS = rsaGenerator.generateKeyPair();
            JWT_KEYS_DECLARATION = rsaGenerator.generateKeyPair();
            JWT_KEYS_ANALYTICS = rsaGenerator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Unable to generate JWT keys", e);
        }
        System.setProperty("robert.jwt.privatekey", base64PrivateKey(JWT_KEYS));
        System.setProperty("robert.jwt.declare.private-key", base64PrivateKey(JWT_KEYS_DECLARATION));
        System.setProperty("robert.jwt.analytics.token.private-key", base64PrivateKey(JWT_KEYS_ANALYTICS));
    }

    private static String base64PrivateKey(final KeyPair keys) {
        return Base64.getEncoder()
                .encodeToString(keys.getPrivate().getEncoded());
    }
}
