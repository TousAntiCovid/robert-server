package fr.gouv.stopc.robert.crypto.grpc.server.test.matchers;

import fr.gouv.stopc.robert.server.common.service.RobertClock;
import fr.gouv.stopc.robert.server.crypto.service.CryptoService;
import fr.gouv.stopc.robert.server.crypto.service.impl.CryptoServiceImpl;
import fr.gouv.stopc.robert.server.crypto.structure.impl.CryptoAESECB;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
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

public class EphemeralTupleMatcherTest {

    private CryptoService cryptoService;

    private RobertClock clock;

    private final String timeStart = "2020-06-01";

    @BeforeEach
    void setup() {

        cryptoService = new CryptoServiceImpl();

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
                isValidTuple(cryptoService, clock)
        );

    }

    @SneakyThrows
    @Test
    void test_decryptEcc() {
        final var ebid = "QIrFSPWmW8w=";
        final var federationKey = "9NCVDxYWUHaCwNxjC+6NtUNVrDgg9tpRpf7SujUQkls=";
        final var federationKeyForEcc = new CryptoAESECB(Base64.getDecoder().decode(federationKey));

        final var serverCC = new byte[] { (byte) 0x21 };
        final var encryptedServerCC = cryptoService
                .encryptCountryCode(federationKeyForEcc, Base64.getDecoder().decode(ebid), serverCC[0]);
        final var decryptedServerCC = cryptoService
                .decryptCountryCode(federationKeyForEcc, Base64.getDecoder().decode(ebid), encryptedServerCC[0]);

        Assertions.assertEquals(serverCC[0], decryptedServerCC[0]);

    }

}
