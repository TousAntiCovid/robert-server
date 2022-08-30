package fr.gouv.stopc.robert.crypto.grpc.server.test.matchers;

import fr.gouv.stopc.robert.server.common.service.RobertClock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.spec.SecretKeySpec;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Base64;

import static fr.gouv.stopc.robert.crypto.grpc.server.test.KeystoreManager.createKeystore;
import static fr.gouv.stopc.robert.crypto.grpc.server.test.KeystoreManager.storeKey;
import static fr.gouv.stopc.robert.crypto.grpc.server.test.matchers.EphemeralTupleMatcher.isValidTuple;
import static java.time.format.DateTimeFormatter.BASIC_ISO_DATE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class EphemeralTupleMatcherTest {

    private RobertClock clock;

    private final String timeStart = "2020-06-01";

    @BeforeEach
    void setup() {
        clock = new RobertClock(timeStart);
    }

    @Test
    void can_detect_valid_tuple() {
        final var epochid = 66855;
        final var ebid = "yqfVsPunrqc=";
        final var ecc = "wg==";
        final var base64ServerKey = "/T2imPBNO8h7IR3avVJpH6frrqSrarkM";
        final var base64FederationKey = "9ZBbyujJnobnh1raCjXpNiAbQ7XnfMyxn17RbuCft7U=";

        final var decodedServerKey = Base64.getDecoder().decode(base64ServerKey);
        final var serverKey = new SecretKeySpec(decodedServerKey, 0, decodedServerKey.length, "AES");

        final var decodedFederationKey = Base64.getDecoder().decode(base64FederationKey);
        final var federationKey = new SecretKeySpec(decodedFederationKey, 0, decodedFederationKey.length, "AES");

        var dateFormatted = LocalDate.ofInstant(clock.atEpoch(epochid).asInstant(), ZoneOffset.UTC)
                .format(BASIC_ISO_DATE);
        var alias = String.format("server-key-%s", dateFormatted);

        createKeystore();
        storeKey(serverKey, federationKey, alias);

        assertThat(
                new EphemeralTupleMatcher.EphemeralTuple(epochid, new EphemeralTupleMatcher.TupleKey(ebid, ecc)),
                isValidTuple(clock)
        );

    }

    @Test
    void can_detect_invalid_ebid() {
        final var epochid = 66855;
        final var ebid = "ASdbJwGbxRw=";
        final var ecc = "wg==";
        final var base64ServerKey = "/T2imPBNO8h7IR3avVJpH6frrqSrarkM";
        final var base64FederationKey = "9ZBbyujJnobnh1raCjXpNiAbQ7XnfMyxn17RbuCft7U=";

        final var decodedServerKey = Base64.getDecoder().decode(base64ServerKey);
        final var serverKey = new SecretKeySpec(decodedServerKey, 0, decodedServerKey.length, "AES");

        final var decodedFederationKey = Base64.getDecoder().decode(base64FederationKey);
        final var federationKey = new SecretKeySpec(decodedFederationKey, 0, decodedFederationKey.length, "AES");

        var dateFormatted = LocalDate.ofInstant(clock.atEpoch(epochid).asInstant(), ZoneOffset.UTC)
                .format(BASIC_ISO_DATE);
        var alias = String.format("server-key-%s", dateFormatted);

        createKeystore();
        storeKey(serverKey, federationKey, alias);

        final var error = assertThrows(
                AssertionError.class,
                () -> assertThat(
                        new EphemeralTupleMatcher.EphemeralTuple(
                                epochid, new EphemeralTupleMatcher.TupleKey(ebid, ecc)
                        ),
                        isValidTuple(clock)
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
        final var epochid = 66855;
        final var ebid = "yqfVsPunrqc=";
        final var ecc = "Zg==";
        final var base64ServerKey = "/T2imPBNO8h7IR3avVJpH6frrqSrarkM";
        final var base64FederationKey = "9ZBbyujJnobnh1raCjXpNiAbQ7XnfMyxn17RbuCft7U=";

        final var decodedServerKey = Base64.getDecoder().decode(base64ServerKey);
        final var serverKey = new SecretKeySpec(decodedServerKey, 0, decodedServerKey.length, "AES");

        final var decodedFederationKey = Base64.getDecoder().decode(base64FederationKey);
        final var federationKey = new SecretKeySpec(decodedFederationKey, 0, decodedFederationKey.length, "AES");

        var dateFormatted = LocalDate.ofInstant(clock.atEpoch(epochid).asInstant(), ZoneOffset.UTC)
                .format(BASIC_ISO_DATE);
        var alias = String.format("server-key-%s", dateFormatted);

        createKeystore();
        storeKey(serverKey, federationKey, alias);

        final var error = assertThrows(
                AssertionError.class,
                () -> assertThat(
                        new EphemeralTupleMatcher.EphemeralTuple(
                                epochid, new EphemeralTupleMatcher.TupleKey(ebid, ecc)
                        ),
                        isValidTuple(clock)
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
