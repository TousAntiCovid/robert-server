package fr.gouv.stopc.e2e.appmobile.model;

import fr.gouv.stopc.e2e.external.crypto.CryptoHMACSHA256;
import fr.gouv.stopc.e2e.external.crypto.exception.RobertServerCryptoException;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.KeyAgreement;

import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Objects;
import java.util.Optional;

@Slf4j
public class EcdhUtils {

    private final static String HASH_MAC = "mac";

    private final static String HASH_TUPLES = "tuples";

    public static KeyPair generateKeyPair() throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {

        // Generate ephemeral ECDH keypair
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
        kpg.initialize(new ECGenParameterSpec("secp256r1"), new SecureRandom());
        return kpg.generateKeyPair();

    }

    /**
     * @param clientPublicKey
     * @return keys generated from shared secret and the server public key
     * @throws RobertServerCryptoException
     */
    public static Optional<ClientIdentifierBundle> deriveKeysFromBackendPublicKey(byte[] clientPublicKey,
            KeyPair appMobileKeyPair)
            throws RobertServerCryptoException {
        byte[] sharedSecret = generateSharedSecret(clientPublicKey, appMobileKeyPair);

        if (Objects.isNull(sharedSecret)) {
            return Optional.empty();
        }

        byte[] kaMac = deriveKeyForMacFromBackendPublicKey(sharedSecret);
        byte[] kaTuples = deriveKeyForTuplesFromBackendPublicKey(sharedSecret);

        if (Objects.isNull(kaMac) || Objects.isNull(kaTuples)) {
            return Optional.empty();
        }

        return Optional.of(
                ClientIdentifierBundle.builder()
                        .keyForMac(kaMac)
                        .keyForTuples(kaTuples)
                        .build()
        );
    }

    private static byte[] deriveKeyForMacFromBackendPublicKey(byte[] sharedSecret) throws RobertServerCryptoException {
        CryptoHMACSHA256 sha256Mac = new CryptoHMACSHA256(sharedSecret);
        return sha256Mac.encrypt(HASH_MAC.getBytes());
    }

    private static byte[] deriveKeyForTuplesFromBackendPublicKey(byte[] sharedSecret)
            throws RobertServerCryptoException {
        CryptoHMACSHA256 sha256Mac = new CryptoHMACSHA256(sharedSecret);
        return sha256Mac.encrypt(HASH_TUPLES.getBytes());
    }

    private static byte[] generateSharedSecret(byte[] backEndPublicKey, KeyPair appMobileKeyPair) {
        if (appMobileKeyPair == null) {
            log.error("Could not retrieve server key pair");
            return null;
        }

        PrivateKey appMobilePrivateKey = appMobileKeyPair.getPrivate();

        try {

            log.debug("GET BACKEND PUBLIC KEY");
            X509EncodedKeySpec pkSpec = new X509EncodedKeySpec(backEndPublicKey);
            KeyFactory kf = KeyFactory.getInstance("EC");
            PublicKey backEndPublicKeyAsKey = kf.generatePublic(pkSpec);

            log.debug("PERFORM KEY AGREEMENT");
            KeyAgreement keyAgreement = KeyAgreement.getInstance("ECDH");
            keyAgreement.init(appMobilePrivateKey);
            keyAgreement.doPhase(backEndPublicKeyAsKey, true);

            return keyAgreement.generateSecret();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException
                | InvalidKeyException | IllegalStateException | ProviderException e) {
            log.error("Unable to generate ECDH Keys due to {}", e.getMessage());
        }

        return null;
    }
}
