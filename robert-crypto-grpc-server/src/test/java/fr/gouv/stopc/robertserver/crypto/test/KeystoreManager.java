package fr.gouv.stopc.robertserver.crypto.test;

import fr.gouv.stopc.robert.crypto.grpc.server.storage.database.model.ClientIdentifier;
import fr.gouv.stopc.robert.crypto.grpc.server.storage.model.ClientIdentifierBundle;
import fr.gouv.stopc.robert.server.common.DigestSaltEnum;
import fr.gouv.stopc.robert.server.common.utils.ByteUtils;
import fr.gouv.stopc.robert.server.crypto.structure.ICryptoStructure;
import fr.gouv.stopc.robert.server.crypto.structure.impl.CryptoAESECB;
import fr.gouv.stopc.robert.server.crypto.structure.impl.CryptoHMACSHA256;
import fr.gouv.stopc.robert.server.crypto.structure.impl.CryptoSkinny64;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.springframework.test.context.TestExecutionListener;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;

import java.io.FileOutputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;
import java.security.cert.Certificate;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;

import static fr.gouv.stopc.robertserver.crypto.test.ClockManager.clock;
import static java.lang.String.format;
import static java.time.ZoneOffset.UTC;
import static java.time.format.DateTimeFormatter.BASIC_ISO_DATE;
import static java.time.temporal.ChronoUnit.DAYS;

/**
 * Creates a keystore file to make robert-crypto application able to run
 */
@Slf4j
public class KeystoreManager implements TestExecutionListener {

    private static final KeyPair ROBERT_SERVER_KEYPAIR = generateKeyPair();

    private static final Certificate ROBERT_SERVER_CERTIFICATE = generateSelfSignedCertificate(ROBERT_SERVER_KEYPAIR);

    public static final KeyStore KEYSTORE;

    private static final Path KEYSTORE_PATH;

    private static final String KEYSTORE_PASSWORD = "1234";

    static {

        try {
            KEYSTORE_PATH = Files.createTempFile("keystore", ".p12");
            KEYSTORE_PATH.toFile().deleteOnExit();

            KEYSTORE = KeyStore.getInstance("pkcs12");
            KEYSTORE.load(null, null);
            generateRegisterKey();
            generateAESKey("federation-key", 256);
            generateAESKey("key-encryption-key", 256);
            LocalDate.now().datesUntil(LocalDate.now().plusDays(5))
                    .map(date -> date.format(BASIC_ISO_DATE))
                    .forEach(date -> generateAESKey("server-key-" + date, 192));
            try (final var fos = new FileOutputStream(KEYSTORE_PATH.toString())) {
                KEYSTORE.store(fos, KEYSTORE_PASSWORD.toCharArray());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        System.setProperty("robert.crypto.server.keystore.password", KEYSTORE_PASSWORD);
        System.setProperty("robert.server.time-start", "20200601");
        System.setProperty("robert.protocol.hello-message-timestamp-tolerance", "180");
        System.setProperty("robert.crypto.server.keystore.file", format("file:%s", KEYSTORE_PATH));
        System.setProperty("robert.crypto.server.keystore.type", "PKCS12");
    }

    @SneakyThrows
    private static void generateRegisterKey() {
        KEYSTORE.setKeyEntry(
                "register-key", ROBERT_SERVER_KEYPAIR.getPrivate(), KEYSTORE_PASSWORD.toCharArray(),
                new Certificate[] { ROBERT_SERVER_CERTIFICATE }
        );
    }

    @SneakyThrows
    private static Certificate generateSelfSignedCertificate(KeyPair keyPair) {
        // Generate keys
        final var privateKey = keyPair.getPrivate();
        final var publicKey = keyPair.getPublic();

        // Generate certificate
        final var x500Name = new X500Name("CN=StopCovid");
        final var pubKeyInfo = SubjectPublicKeyInfo.getInstance(publicKey.getEncoded());
        final var startDate = new Date();
        final var endDate = Date
                .from(Instant.now().plus(365, DAYS));
        final var certificateBuilder = new X509v3CertificateBuilder(
                x500Name, new BigInteger(10, new SecureRandom()), startDate, endDate, x500Name, pubKeyInfo
        );
        final var contentSigner = new JcaContentSignerBuilder("SHA256withECDSA")
                .build(privateKey);
        return new JcaX509CertificateConverter().setProvider(new BouncyCastleProvider())
                .getCertificate(certificateBuilder.build(contentSigner));
    }

    @SneakyThrows
    public static ICryptoStructure<Cipher, Mac> cipherForStoredKey() {
        return new ICryptoStructure<>() {

            @Override
            public Key getSecretKey() {
                throw new UnsupportedOperationException("Not implemented!");
            }

            @Override
            @SneakyThrows
            public byte[] encrypt(byte[] dataToEncrypt) {
                final var keyEncryptionKey = KEYSTORE.getKey("key-encryption-key", KEYSTORE_PASSWORD.toCharArray());
                final var cipher = Cipher.getInstance("AES/GCM/NoPadding");
                cipher.init(Cipher.ENCRYPT_MODE, keyEncryptionKey);
                final var cipherText = cipher.doFinal(dataToEncrypt);
                return ByteUtils.addAll(cipher.getIV(), cipherText);

            }

            @Override
            @SneakyThrows
            public byte[] decrypt(byte[] encryptedData) {
                final var ivLength = 12;
                final var keyEncryptionKey = KEYSTORE.getKey("key-encryption-key", KEYSTORE_PASSWORD.toCharArray());
                final var gcmParams = new GCMParameterSpec(128, encryptedData, 0, ivLength);

                final var partToDecrypt = Arrays.copyOfRange(encryptedData, 12, encryptedData.length);

                final var cipher = Cipher.getInstance("AES/GCM/NoPadding");
                cipher.init(Cipher.DECRYPT_MODE, keyEncryptionKey, gcmParams);
                return cipher.doFinal(partToDecrypt);
            }
        };
    }

    @SneakyThrows
    public static CryptoSkinny64 cipherForEbidAtEpoch(final int epochId) {
        final var serverKey = getServerKey(epochId);
        return new CryptoSkinny64(serverKey.getEncoded());
    }

    @SneakyThrows
    public static CryptoAESECB cipherForEcc() {
        return new CryptoAESECB(KEYSTORE.getKey("federation-key", KEYSTORE_PASSWORD.toCharArray()));
    }

    @SneakyThrows
    public static Key getServerKey(final int epochId) {
        final var epochDate = clock().atEpoch(epochId)
                .asInstant()
                .atZone(UTC)
                .toLocalDate()
                .format(BASIC_ISO_DATE);
        return KEYSTORE.getKey(format("server-key-%s", epochDate), KEYSTORE_PASSWORD.toCharArray());
    }

    @SneakyThrows
    public static byte[] generateEbid(byte[] id, int epochId, byte[] ks) {
        final var ebid = new byte[8];
        System.arraycopy(ByteUtils.intToBytes(epochId), 1, ebid, 0, Integer.BYTES - 1);
        System.arraycopy(id, 0, ebid, Integer.BYTES - 1, id.length);

        CryptoSkinny64 cryptoSkinny64 = new CryptoSkinny64(ks);

        return cryptoSkinny64.encrypt(ebid);
    }

    @SneakyThrows
    public static byte[] generateMac(byte[] ebid, int epochId, long time, byte[] keyForMac, DigestSaltEnum digestSalt) {
        final var digest = new byte[] { digestSalt.getValue() };
        final var toHash = new byte[digest.length + ebid.length + Integer.BYTES + Integer.BYTES];
        System.arraycopy(digest, 0, toHash, 0, digest.length);
        System.arraycopy(ebid, 0, toHash, digest.length, ebid.length);
        System.arraycopy(ByteUtils.intToBytes(epochId), 0, toHash, digest.length + ebid.length, Integer.BYTES);
        System.arraycopy(
                ByteUtils.longToBytes(time), 4, toHash, digest.length + ebid.length + Integer.BYTES, Integer.BYTES
        );

        CryptoHMACSHA256 hmacsha256 = new CryptoHMACSHA256(keyForMac);
        return hmacsha256.encrypt(toHash);

    }

    @SneakyThrows
    public static ClientIdentifierBundle createClientId() {
        byte[] keyForMac = new byte[32];
        byte[] keyForTuples = new byte[32];
        final var idA = new byte[5];
        new SecureRandom().nextBytes(idA);
        final var encryptedKeyForMac = cipherForStoredKey().encrypt(keyForMac);
        final var encryptedKeyForTuples = cipherForStoredKey().encrypt(keyForTuples);

        ClientIdentifier clientIdentifier = ClientIdentifier.builder()
                .idA(Base64.getEncoder().encodeToString(idA))
                .keyForMac(Base64.getEncoder().encodeToString(encryptedKeyForMac))
                .keyForTuples(Base64.getEncoder().encodeToString(encryptedKeyForTuples))
                .build();
        PostgreSqlManager.insert(clientIdentifier);

        return ClientIdentifierBundle.builder()
                .id(idA)
                .keyForMac(keyForMac)
                .keyForTuples(keyForTuples)
                .build();
    }

    private static KeyPair generateKeyPair() {
        try {
            return KeyPairGenerator.getInstance("EC").generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private static void generateAESKey(final String alias, final int keySize) {
        try {
            final var rsaKeyGenerator = KeyGenerator.getInstance("AES");
            rsaKeyGenerator.init(keySize);
            KEYSTORE.setKeyEntry(alias, rsaKeyGenerator.generateKey(), KEYSTORE_PASSWORD.toCharArray(), null);
        } catch (NoSuchAlgorithmException | KeyStoreException e) {
            throw new RuntimeException(e);
        }
    }
}
