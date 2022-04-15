package fr.gouv.stopc.robert.crypto.grpc.server.test;

import lombok.extern.slf4j.Slf4j;
import org.springframework.test.context.TestExecutionListener;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.time.LocalDate.now;
import static java.time.format.DateTimeFormatter.BASIC_ISO_DATE;

@Slf4j
public class KeystoreManager implements TestExecutionListener {

    private static final String keystorePath = "src/test/resources/keystore.p12";

    private static Path newKeystorePath;

    static {

        try {
            copyKeystore();
            generateAESKey("federation-key", 256);
            generateAESKey("key-encryption-key", 256);
            generateAESKeys();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

        System.setProperty("robert.crypto.server.keystore.password", "1234");
        System.setProperty("robert.crypto.server.keystore.config.file", "classpath:config/SoftHSMv2/softhsm2.cfg");
        System.setProperty("robert.server.time-start", "20200601");
        System.setProperty("robert.protocol.hello-message-timestamp-tolerance", "180");
        System.setProperty(
                "robert.crypto.server.keystore.file", String.format("classpath:%s", newKeystorePath.getFileName())
        );
        System.setProperty("robert.crypto.server.keystore.type", "PKCS12");

    }

    private static void generateAESKeys() throws IOException, InterruptedException {

        for (LocalDate date = now().minusDays(15); date.isBefore(now().plusDays(6)); date = date.plusDays(1)) {
            var dateFormatted = date.format(BASIC_ISO_DATE);
            var alias = String.format("server-key-%s", dateFormatted);
            generateAESKey(alias, 192);

        }
    }

    private static void generateAESKey(String alias, int size) throws IOException, InterruptedException {

        log.info("Generating {}", alias);
        final var command = String.format(
                "keytool -genseckey -alias \"%s\" -keyalg AES -keysize \"%d\" -keystore %s -storepass 1234 -storetype PKCS12",
                alias, size, newKeystorePath.toAbsolutePath()
        );
        var process = Runtime.getRuntime().exec(command);
        process.waitFor();

    }

    private static void copyKeystore() throws IOException {
        var originalPath = new File("src/main/docker/keystore.p12").toPath();
        newKeystorePath = Paths.get(keystorePath);
        Files.copy(originalPath, newKeystorePath, REPLACE_EXISTING);

    }

}
