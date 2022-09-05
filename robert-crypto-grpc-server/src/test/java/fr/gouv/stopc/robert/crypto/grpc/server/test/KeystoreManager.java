package fr.gouv.stopc.robert.crypto.grpc.server.test;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.springframework.test.context.TestExecutionListener;

import javax.crypto.spec.SecretKeySpec;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;
import java.security.KeyStore.SecretKeyEntry;
import java.security.cert.Certificate;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Date;

import static java.time.LocalDate.now;
import static java.time.format.DateTimeFormatter.BASIC_ISO_DATE;
import static java.time.temporal.ChronoUnit.DAYS;
import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
public class KeystoreManager implements TestExecutionListener {

    public static final Path KEYSTORE_PATH;

    private static final String password = "1234";

    private static KeyPair ecKeyPair;

    static {

        try {
            KEYSTORE_PATH = Files.createTempFile("keystore", ".p12");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        System.setProperty("robert.crypto.server.keystore.password", password);
        System.setProperty("robert.server.time-start", "20200601");
        System.setProperty("robert.protocol.hello-message-timestamp-tolerance", "180");
        System.setProperty(
                "robert.crypto.server.keystore.file", String.format("file:%s", KEYSTORE_PATH.toString())
        );
        System.setProperty("robert.crypto.server.keystore.type", "PKCS12");

        createKeystore();
        generateRegistrationKey();
        generateAESKey("federation-key", 256);
        generateAESKey("key-encryption-key", 256);
        generateAESKeys();

    }

    @SneakyThrows
    public static void createKeystore() {
        final var keystore = KeyStore.getInstance("pkcs12");
        keystore.load(null, password.toCharArray());
        try (final var fos = new FileOutputStream(KEYSTORE_PATH.toString())) {
            keystore.store(fos, password.toCharArray());
        }
    }

    @SneakyThrows
    private static void generateAESKeys() {
        for (var date = now(); date.isBefore(now().plusDays(5)); date = date.plusDays(1)) {
            final var dateFormatted = date.format(BASIC_ISO_DATE);
            final var alias = String.format("server-key-%s", dateFormatted);
            generateAESKey(alias, 192);
        }
    }

    @SneakyThrows
    private static void generateAESKey(String alias, int size) {
        log.info("Generating {}", alias);
        final var command = new String[] {
                "keytool", "-genseckey", "-alias", alias, "-keyalg", "AES", "-keysize", String.valueOf(size),
                "-keystore", KEYSTORE_PATH.toAbsolutePath().toString(), "-storepass", password, "-storetype", "PKCS12"
        };
        var process = Runtime.getRuntime().exec(command);
        assertThat(process.waitFor())
                .as("keytool exit code for '%s' key generation", alias)
                .isEqualTo(0);
    }

    @SneakyThrows
    private static void generateRegistrationKey() {

        log.info("Generating registration key");

        // Generate keys
        final var ecGenerator = KeyPairGenerator.getInstance("EC");
        ecKeyPair = ecGenerator.generateKeyPair();
        final var privateKey = ecKeyPair.getPrivate();
        final var publicKey = ecKeyPair.getPublic();

        // Generate certificate
        final var x500Name = new X500Name("CN=StopCovid");
        final var pubKeyInfo = SubjectPublicKeyInfo.getInstance(publicKey.getEncoded());
        final var startDate = new Date();
        final var endDate = Date
                .from(Instant.now().plus(365, DAYS));
        final var certificateBuilder = new X509v3CertificateBuilder(
                x500Name, new BigInteger(10, new SecureRandom()), startDate, endDate, x500Name, pubKeyInfo
        );
        final var contentSigner = new JcaContentSignerBuilder("SHA256withECDSA").build(privateKey);
        final var certificate = new JcaX509CertificateConverter().setProvider(new BouncyCastleProvider())
                .getCertificate(certificateBuilder.build(contentSigner));

        // Store certificate
        final var keystore = loadKeystore();
        final var privateKeyEntry = new KeyStore.PrivateKeyEntry(privateKey, new Certificate[] { certificate });
        keystore.setEntry("register-key", privateKeyEntry, new KeyStore.PasswordProtection(password.toCharArray()));

        // Save the keystore
        try (final var writer = new FileOutputStream(KEYSTORE_PATH.toString())) {
            keystore.store(writer, password.toCharArray());
        }
    }

    @SneakyThrows
    private static KeyStore loadKeystore() {
        final var keystore = KeyStore.getInstance("pkcs12");
        keystore.load(new FileInputStream(KEYSTORE_PATH.toString()), password.toCharArray());
        return keystore;
    }

    @SneakyThrows
    public static Key getServerKey(final LocalDate date) {
        final var keystore = loadKeystore();
        final var alias = String.format("server-key-%s", date.format(BASIC_ISO_DATE));
        return keystore.getKey(alias, password.toCharArray());
    }

    @SneakyThrows
    public static Key getFederationKey() {
        final var keystore = loadKeystore();
        return keystore.getKey("federation-key", password.toCharArray());
    }

    @SneakyThrows
    public static Key getEncryptionKey() {
        final var keystore = loadKeystore();
        return keystore.getKey("key-encryption-key", password.toCharArray());
    }

    @SneakyThrows
    public static void storeKey(final SecretKeySpec serverKey, final SecretKeySpec federationKey,
            final String serverKeyAlias) {
        final var serverKeyEntry = new SecretKeyEntry(serverKey);
        final var federationKeyEntry = new SecretKeyEntry(federationKey);
        final var passwordProtection = new KeyStore.PasswordProtection(password.toCharArray());

        final var keystore = loadKeystore();
        keystore.setEntry("federation-key", federationKeyEntry, passwordProtection);
        keystore.setEntry(serverKeyAlias, serverKeyEntry, passwordProtection);

        try (final var writer = new FileOutputStream(KEYSTORE_PATH.toString())) {
            keystore.store(writer, password.toCharArray());
        }
    }

}
