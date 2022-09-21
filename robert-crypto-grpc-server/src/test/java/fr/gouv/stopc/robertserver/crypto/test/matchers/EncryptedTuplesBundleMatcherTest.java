package fr.gouv.stopc.robertserver.crypto.test.matchers;

import fr.gouv.stopc.robert.server.common.service.RobertClock;
import fr.gouv.stopc.robert.server.common.service.RobertClock.RobertInstant;
import fr.gouv.stopc.robertserver.crypto.test.CountryCode;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.crypto.spec.SecretKeySpec;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

import static fr.gouv.stopc.robertserver.crypto.test.KeystoreManager.KEYSTORE;
import static fr.gouv.stopc.robertserver.crypto.test.matchers.EncryptedTuplesBundleMatcher.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EncryptedTuplesBundleMatcherTest {

    private static final String SERVER_KEY_BASE64 = "/T2imPBNO8h7IR3avVJpH6frrqSrarkM";

    private static final String FEDERATION_KEY_BASE64 = "9ZBbyujJnobnh1raCjXpNiAbQ7XnfMyxn17RbuCft7U=";

    private static final RobertInstant TEST_INSTANT = RobertClock.parse("2022-04-28T09:45:00Z=66855E");

    private static final EphemeralTuple VALID_EPHEMERAL_TUPLE = new EphemeralTuple(
            66855,
            new EphemeralTuple.TupleKey("yqfVsPunrqc=", "wg==")
    );

    private static ByteArrayOutputStream keystoreBackup;

    @BeforeAll
    static void customizeKeystoreKeys() throws Exception {
        keystoreBackup = new ByteArrayOutputStream();
        KEYSTORE.store(keystoreBackup, "1234".toCharArray());
        keystoreBackup.close();

        final var decodedFederationKey = Base64.getDecoder().decode(FEDERATION_KEY_BASE64);
        final var federationKey = new SecretKeySpec(decodedFederationKey, 0, decodedFederationKey.length, "AES");
        KEYSTORE.setKeyEntry("federation-key", federationKey, "1234".toCharArray(), null);

        final var decodedServerKey = Base64.getDecoder().decode(SERVER_KEY_BASE64);
        final var serverKey = new SecretKeySpec(decodedServerKey, 0, decodedServerKey.length, "AES");
        KEYSTORE.setKeyEntry("server-key-20220428", serverKey, "1234".toCharArray(), null);
    }

    @AfterAll
    static void restoreOriginalKeystore() throws Exception {
        KEYSTORE.load(new ByteArrayInputStream(keystoreBackup.toByteArray()), "1234".toCharArray());
    }

    @Test
    void can_verify_valid_tuple() {
        assertThat(VALID_EPHEMERAL_TUPLE)
                .has(countryCode(CountryCode.FRANCE))
                .has(ebidConstistentWithTupleEpoch())
                .has(idA("30Liauw="));
    }

    @Test
    void can_detect_inconsistent_epochId() {
        final var ebid = "ASdbJwGbxRw=";
        final var ecc = "wg==";

        assertThatThrownBy(
                () -> assertThat(new EphemeralTuple(66855, new EphemeralTuple.TupleKey(ebid, ecc)))
                        .has(ebidConstistentWithTupleEpoch())
        )
                .hasMessage(
                        "\n" +
                                "Expecting actual:\n" +
                                "  EphemeralTuple(epochId=66855, key.ebid=ASdbJwGbxRw=, key.ecc=wg==)\n" +
                                "to have a tuple with an EBID corresponding to the unencrypted epoch but EphemeralTuple(epochId=66855, key.ebid=ASdbJwGbxRw=, key.ecc=wg==) was a tuple with an EBID for epoch 3188169"
                );
    }

    @Test
    void can_detect_invalid_ecc() {
        final var ebid = "yqfVsPunrqc=";
        final var ecc = "Zg==";

        assertThatThrownBy(
                () -> assertThat(new EphemeralTuple(66855, new EphemeralTuple.TupleKey(ebid, ecc)))
                        .has(countryCode(CountryCode.GERMANY))
        )
                .hasMessage(
                        "\n" +
                                "Expecting actual:\n" +
                                "  EphemeralTuple(epochId=66855, key.ebid=yqfVsPunrqc=, key.ecc=Zg==)\n" +
                                "to have a tuple with an ECC corresponding to GERMANY(49) but EphemeralTuple(epochId=66855, key.ebid=yqfVsPunrqc=, key.ecc=Zg==) has an ECC corresponding to -123"
                );
    }

    @Test
    void can_detect_invalid_idA() {
        assertThatThrownBy(
                () -> assertThat(VALID_EPHEMERAL_TUPLE)
                        .has(idA("wrong_idA"))
        )
                .hasMessage(
                        "\n" +
                                "Expecting actual:\n" +
                                "  EphemeralTuple(epochId=66855, key.ebid=yqfVsPunrqc=, key.ecc=wg==)\n" +
                                "to have a tuple with an EBID corresponding to idA wrong_idA but idA of tuple EphemeralTuple(epochId=66855, key.ebid=yqfVsPunrqc=, key.ecc=wg==) was 30Liauw="
                );
    }
}
