package fr.gouv.stopc.robert.crypto.grpc.server.test;

import fr.gouv.stopc.robert.crypto.grpc.server.storage.model.ClientIdentifierBundle;
import fr.gouv.stopc.robert.server.crypto.exception.RobertServerCryptoException;
import fr.gouv.stopc.robert.server.crypto.structure.CryptoCipherStructureAbstract;
import lombok.SneakyThrows;
import org.bson.internal.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;

import java.security.InvalidAlgorithmParameterException;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.ECGenParameterSpec;
import java.util.Arrays;

import static fr.gouv.stopc.robert.crypto.grpc.server.test.KeystoreManager.getEncryptionKey;
import static org.assertj.core.api.Assertions.assertThat;

public class CryptoManager {

    private static final int IV_LENGTH = 12;

    public static KeyPair generateECDHKeyPair(String ecSpec) {
        try {
            // Generate ephemeral ECDH keypair
            KeyPairGenerator kpg;
            kpg = KeyPairGenerator.getInstance("EC");
            kpg.initialize(new ECGenParameterSpec(ecSpec));
            KeyPair keyPair = kpg.generateKeyPair();

            return keyPair;

        } catch (NoSuchAlgorithmException | IllegalStateException | InvalidAlgorithmParameterException e) {
            throw new RuntimeException("Unable to generate ECDH public key", e);
        }
    }

    public static KeyPair generateDHKeyPair() {
        try {
            KeyPairGenerator kpg;
            kpg = KeyPairGenerator.getInstance("DH");
            kpg.initialize(1024, new SecureRandom());
            KeyPair keyPair = kpg.generateKeyPair();

            return keyPair;

        } catch (NoSuchAlgorithmException | IllegalStateException e) {
            throw new RuntimeException("Unable to generate ECDH public key", e);
        }
    }

    public static int encryptCountryCode(final CryptoCipherStructureAbstract cryptoForECC, final byte[] ebid,
            final byte countryCode) throws RobertServerCryptoException {
        assertThat(ebid.length).isEqualTo(8);

        // Pad to 128-bits
        byte[] payloadToEncrypt = Arrays.copyOf(ebid, 128 / 8);

        // AES Encryption of the payload to encrypt
        byte[] encryptedPayload = cryptoForECC.encrypt(payloadToEncrypt);

        // Truncate to 8 bits
        // Equivalent to MSB in ROBert spec
        byte truncatedEncryptedPayload = encryptedPayload[0];

        return (truncatedEncryptedPayload ^ countryCode);
    }

    public static int decryptCountryCode(final CryptoCipherStructureAbstract cryptoForECC, final byte[] ebid,
            final byte encryptedCountryCode) throws RobertServerCryptoException {
        // decrypt method is same as encrypt
        return encryptCountryCode(cryptoForECC, ebid, encryptedCountryCode);
    }

    public static ClientIdentifierBundle findClientById(String id) {

        final var client = PostgreSqlManager.findClientById(id);
        final var encryptionKey = getEncryptionKey();

        final byte[] decryptedKeyForMac = decryptStoredKeyWithAES256GCMAndKek(
                Base64.decode(client.getKeyForMac()),
                encryptionKey
        );

        final byte[] decryptedKeyForTuples = decryptStoredKeyWithAES256GCMAndKek(
                Base64.decode(client.getKeyForTuples()),
                encryptionKey
        );

        return ClientIdentifierBundle.builder()
                .id(Arrays.copyOf(id.getBytes(), id.getBytes().length))
                .keyForMac(Arrays.copyOf(decryptedKeyForMac, decryptedKeyForMac.length))
                .keyForTuples(Arrays.copyOf(decryptedKeyForTuples, decryptedKeyForTuples.length))
                .build();

    }

    @SneakyThrows
    private static byte[] decryptStoredKeyWithAES256GCMAndKek(byte[] storedKey, Key kek) {
        final AlgorithmParameterSpec algorithmParameterSpec = new GCMParameterSpec(128, storedKey, 0, IV_LENGTH);
        final byte[] toDecrypt = new byte[storedKey.length - IV_LENGTH];
        System.arraycopy(storedKey, IV_LENGTH, toDecrypt, 0, storedKey.length - IV_LENGTH);

        final var cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, kek, algorithmParameterSpec);
        return cipher.doFinal(toDecrypt);

    }

}
