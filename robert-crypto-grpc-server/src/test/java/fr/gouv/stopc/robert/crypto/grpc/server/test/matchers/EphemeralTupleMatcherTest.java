package fr.gouv.stopc.robert.crypto.grpc.server.test.matchers;

import fr.gouv.stopc.robert.crypto.grpc.server.test.KeystoreManager;
import fr.gouv.stopc.robert.server.common.service.RobertClock;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.org.apache.commons.io.FileUtils;

import javax.crypto.spec.SecretKeySpec;

import java.io.IOException;
import java.util.Base64;

import static fr.gouv.stopc.robert.crypto.grpc.server.test.KeystoreManager.createKeystore;
import static fr.gouv.stopc.robert.crypto.grpc.server.test.KeystoreManager.storeKey;
import static fr.gouv.stopc.robert.crypto.grpc.server.test.matchers.EphemeralTupleMatcher.isValidTuple;
import static java.time.ZoneOffset.UTC;
import static java.time.format.DateTimeFormatter.BASIC_ISO_DATE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class EphemeralTupleMatcherTest {

    private static final RobertClock CLOCK = new RobertClock("2020-06-01");

    private static final int EPOCH = 66855;

    private static byte[] ORIGINAL_KEYSTORE_FILE_BACKUP;

    @BeforeAll
    static void setupKeystore() throws IOException {
        ORIGINAL_KEYSTORE_FILE_BACKUP = FileUtils.readFileToByteArray(KeystoreManager.KEYSTORE_PATH.toFile());

        final var base64ServerKey = "/T2imPBNO8h7IR3avVJpH6frrqSrarkM";
        final var base64FederationKey = "9ZBbyujJnobnh1raCjXpNiAbQ7XnfMyxn17RbuCft7U=";

        final var decodedServerKey = Base64.getDecoder().decode(base64ServerKey);
        final var serverKey = new SecretKeySpec(decodedServerKey, 0, decodedServerKey.length, "AES");

        final var decodedFederationKey = Base64.getDecoder().decode(base64FederationKey);
        final var federationKey = new SecretKeySpec(decodedFederationKey, 0, decodedFederationKey.length, "AES");

        final var dateFormatted = CLOCK.atEpoch(EPOCH).asInstant().atZone(UTC).toLocalDate()
                .format(BASIC_ISO_DATE);
        var alias = String.format("server-key-%s", dateFormatted);

        createKeystore();
        storeKey(serverKey, federationKey, alias);
    }

    @AfterAll
    static void restoreKeystore() throws IOException {
        FileUtils.writeByteArrayToFile(KeystoreManager.KEYSTORE_PATH.toFile(), ORIGINAL_KEYSTORE_FILE_BACKUP);
    }

    @Test
    void can_detect_valid_tuple() {
        final var ebid = "yqfVsPunrqc=";
        final var ecc = "wg==";

        assertThat(
                new EphemeralTupleMatcher.EphemeralTuple(EPOCH, new EphemeralTupleMatcher.TupleKey(ebid, ecc)),
                isValidTuple(CLOCK)
        );

    }

    @Test
    void can_detect_invalid_ebid() {
        final var ebid = "ASdbJwGbxRw=";
        final var ecc = "wg==";

        final var error = assertThrows(
                AssertionError.class,
                () -> assertThat(
                        new EphemeralTupleMatcher.EphemeralTuple(
                                EPOCH, new EphemeralTupleMatcher.TupleKey(ebid, ecc)
                        ),
                        isValidTuple(CLOCK)
                )
        );

        assertThat(
                error.getMessage().replace("\r", ""), equalTo(
                        "\n" +
                                "Expected: a consistent ephemeral tuple\n" +
                                "     but: was an ephemeral tuple with an ebid containing epochId <3188169> instead of the epochId of the tuple : <66855>"
                )
        );

    }

    @Test
    void can_detect_invalid_ecc() {
        final var ebid = "yqfVsPunrqc=";
        final var ecc = "Zg==";

        final var error = assertThrows(
                AssertionError.class,
                () -> assertThat(
                        new EphemeralTupleMatcher.EphemeralTuple(
                                EPOCH, new EphemeralTupleMatcher.TupleKey(ebid, ecc)
                        ),
                        isValidTuple(CLOCK)
                )
        );

        assertThat(
                error.getMessage().replace("\r", ""), equalTo(
                        "\n" +
                                "Expected: a consistent ephemeral tuple\n" +
                                "     but: was an ephemeral tuple with country code <-123> instead of country code <33>"
                )
        );

    }

}
