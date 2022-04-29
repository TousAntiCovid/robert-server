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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStore.SecretKeyEntry;
import java.security.KeyStoreException;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Date;

import static java.time.LocalDate.now;
import static java.time.format.DateTimeFormatter.BASIC_ISO_DATE;

@Slf4j
public class KeystoreManager implements TestExecutionListener {

    private static final String keystoreSrc = "target/test-classes/keystore.p12";

    private static final Path keystorePath = Paths.get(new File(keystoreSrc).getPath());

    private static final String password = "1234";

    private static KeyPair ecKeyPair;

    static {

        System.setProperty("robert.crypto.server.keystore.password", password);
        System.setProperty("robert.crypto.server.keystore.config.file", "classpath:config/SoftHSMv2/softhsm2.cfg");
        System.setProperty("robert.server.time-start", "20200601");
        System.setProperty("robert.protocol.hello-message-timestamp-tolerance", "180");
        System.setProperty(
                "robert.crypto.server.keystore.file", String.format("classpath:%s", keystorePath.getFileName())
        );
        System.setProperty("robert.crypto.server.keystore.type", "PKCS12");

        clearKeystore();
        generateRegistrationKey();
        generateAESKey("federation-key", 256);
        generateAESKey("key-encryption-key", 256);
        generateAESKeys();

    }

    @SneakyThrows
    private static void generateAESKeys() {

        for (LocalDate date = now().minusDays(15); date.isBefore(now().plusDays(6)); date = date.plusDays(1)) {
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
                .from(LocalDate.now().plus(365, ChronoUnit.DAYS).atStartOfDay().toInstant(ZoneOffset.UTC));
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
        OutputStream writer = new FileOutputStream(keystoreSrc);
        keystore.store(writer, password.toCharArray());
        writer.close();

    }

    @SneakyThrows
    private static KeyStore loadKeystore() {
        final var keystore = KeyStore.getInstance("pkcs12");
        keystore.load(new FileInputStream(keystoreSrc), password.toCharArray());
        return keystore;
    }

    @SneakyThrows
    public static void clearKeystore() {
        final var keystore = loadKeystore();

        Collections.list(keystore.aliases()).forEach(alias -> {
            try {
                keystore.deleteEntry(alias);
            } catch (KeyStoreException e) {
                throw new RuntimeException(e);
            }
        });

        keystore.store(new FileOutputStream(keystoreSrc), password.toCharArray());

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
    public static void storeKey(final SecretKeySpec serverKey, final SecretKeySpec federationKey,
            final String serverKeyAlias) {
        final var serverKeyEntry = new SecretKeyEntry(serverKey);
        final var federationKeyEntry = new SecretKeyEntry(federationKey);
        final var passwordProtection = new KeyStore.PasswordProtection(password.toCharArray());

        final var keystore = loadKeystore();
        keystore.setEntry("federation-key", federationKeyEntry, passwordProtection);
        keystore.setEntry(serverKeyAlias, serverKeyEntry, passwordProtection);

        OutputStream writer = new FileOutputStream(keystoreSrc);
        keystore.store(writer, password.toCharArray());
        writer.close();
    }

}
