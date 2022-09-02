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
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStore.SecretKeyEntry;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Date;

import static java.time.LocalDate.now;
import static java.time.format.DateTimeFormatter.BASIC_ISO_DATE;
import static java.time.temporal.ChronoUnit.DAYS;

@Slf4j
public class KeystoreManager implements TestExecutionListener {

    private static Path keystorePath;

    private static final String password = "1234";

    private static KeyPair ecKeyPair;

    static {

        try {
            keystorePath = Files.createTempFile("keystore", ".p12");
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.setProperty("robert.crypto.server.keystore.password", password);
        System.setProperty("robert.server.time-start", "20200601");
        System.setProperty("robert.protocol.hello-message-timestamp-tolerance", "180");
        System.setProperty(
                "robert.crypto.server.keystore.file", String.format("file:%s", keystorePath.toString())
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
        FileOutputStream fos = new FileOutputStream(keystorePath.toString());
        keystore.store(fos, password.toCharArray());
        fos.close();
    }

    @SneakyThrows
    private static void generateAESKeys() {

        for (LocalDate date = now().minusDays(20); date.isBefore(now().plusDays(5)); date = date.plusDays(1)) {
            var dateFormatted = date.format(BASIC_ISO_DATE);
            var alias = String.format("server-key-%s", dateFormatted);
            generateAESKey(alias, 192);

        }
    }

    @SneakyThrows
    private static void generateAESKey(String alias, int size) {
        log.info("Generating {}", alias);
        final var command = String.format(
                "keytool -genseckey -alias \"%s\" -keyalg AES -keysize \"%d\" -keystore %s -storepass %s -storetype PKCS12",
                alias, size, keystorePath.toAbsolutePath(), password
        );
        var process = Runtime.getRuntime().exec(command);
        process.waitFor();
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
        OutputStream writer = new FileOutputStream(keystorePath.toString());
        keystore.store(writer, password.toCharArray());
        writer.close();

    }

    @SneakyThrows
    private static KeyStore loadKeystore() {
        final var keystore = KeyStore.getInstance("pkcs12");
        keystore.load(new FileInputStream(keystorePath.toString()), password.toCharArray());
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

        OutputStream writer = new FileOutputStream(keystorePath.toString());
        keystore.store(writer, password.toCharArray());
        writer.close();
    }

}
