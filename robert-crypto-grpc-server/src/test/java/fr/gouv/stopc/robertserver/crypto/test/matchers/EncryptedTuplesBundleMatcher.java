package fr.gouv.stopc.robertserver.crypto.test.matchers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.ByteString;
import fr.gouv.stopc.robert.server.common.service.RobertClock.RobertInstant;
import fr.gouv.stopc.robert.server.common.utils.ByteUtils;
import fr.gouv.stopc.robert.server.crypto.exception.RobertServerCryptoException;
import fr.gouv.stopc.robert.server.crypto.structure.impl.CryptoAESGCM;
import fr.gouv.stopc.robertserver.crypto.test.CountryCode;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.Value;
import org.assertj.core.api.Condition;
import org.assertj.core.api.ObjectArrayAssert;

import java.io.IOException;
import java.util.Arrays;
import java.util.Base64;

import static fr.gouv.stopc.robertserver.crypto.test.KeystoreManager.*;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.condition.VerboseCondition.verboseCondition;

@RequiredArgsConstructor
public class EncryptedTuplesBundleMatcher {

    private final byte[] encryptedTuplesBundle;

    public static EncryptedTuplesBundleMatcher assertThatTuplesBundle(ByteString encryptedTuplesBundle) {
        return new EncryptedTuplesBundleMatcher(encryptedTuplesBundle.toByteArray());
    }

    public static Condition<EphemeralTuple> countryCode(CountryCode expectedCountryCode) {
        return verboseCondition(
                ephemeralTuple -> ephemeralTuple.getDecryptedCountryCode() == expectedCountryCode.getNumericCode(),
                "a tuple with an ECC corresponding to " + expectedCountryCode,
                ephemeralTuple -> format(
                        " but %s has an ECC corresponding to %d", ephemeralTuple,
                        ephemeralTuple.getDecryptedCountryCode()
                )
        );
    }

    public static Condition<EphemeralTuple> idA(String expectedBase64EncodedIdA) {
        return verboseCondition(
                ephemeralTuple -> expectedBase64EncodedIdA.equals(ephemeralTuple.getDecryptedBase64IdA()),
                "a tuple with an EBID corresponding to idA " + expectedBase64EncodedIdA,
                ephemeralTuple -> format(
                        " but idA of tuple %s was %s", ephemeralTuple,
                        ephemeralTuple.getDecryptedBase64IdA()
                )
        );
    }

    public static Condition<EphemeralTuple> idA(byte[] expectedIdA) {
        final var expectedBase64EncodedIdA = Base64.getEncoder().encodeToString(expectedIdA);
        return idA(expectedBase64EncodedIdA);
    }

    public static Condition<EphemeralTuple> ebidConstistentWithTupleEpoch() {
        return verboseCondition(
                ephemeralTuple -> ephemeralTuple.getEpochId() == ephemeralTuple.getDecryptedEbidEpoch(),
                "a tuple with an EBID corresponding to the unencrypted epoch",
                ephemeralTuple -> format(
                        " but %s was a tuple with an EBID for epoch %d",
                        ephemeralTuple, ephemeralTuple.getDecryptedEbidEpoch()
                )
        );
    }

    public static Condition<? super EphemeralTuple[]> aBundleWithEpochs(RobertInstant bundleStart,
            RobertInstant bundleEnd) {
        final var expectedEpochIds = bundleStart.epochsUntil(bundleEnd)
                .map(RobertInstant::asEpochId)
                .toArray(Integer[]::new);
        return verboseCondition(
                ephemeralTuples -> {
                    final var actualEpochs = Arrays.stream(ephemeralTuples)
                            .map(EphemeralTuple::getEpochId)
                            .toArray(Integer[]::new);
                    return Arrays.equals(actualEpochs, expectedEpochIds);
                },
                format("a bundle with all epochs from %s to %s", bundleStart, bundleEnd),
                ephemeralTuples -> format(" but is was a bundle containing epochs %s", Arrays.toString(ephemeralTuples))
        );
    }

    public ObjectArrayAssert<EphemeralTuple> isEncryptedWith(CryptoAESGCM cipherForTuples) {
        try {
            final var decryptedTuplesBundle = cipherForTuples.decrypt(encryptedTuplesBundle);
            final var listOfEphemeralTuples = new ObjectMapper()
                    .readValue(decryptedTuplesBundle, EphemeralTuple[].class);
            return assertThat(listOfEphemeralTuples)
                    .as("decrypted tuples bundle");
        } catch (RobertServerCryptoException e) {
            fail("tuples bundle can't be decrypted", e);
        } catch (IOException e) {
            fail("decrypted tuples bundle can't parsed", e);
        }
        return null;
    }

    @Value
    public static class EphemeralTuple {

        int epochId;

        TupleKey key;

        @Value
        public static class TupleKey {

            String ebid;

            String ecc;
        }

        @SneakyThrows
        public int getDecryptedEbidEpoch() {
            final var ebid = Base64.getDecoder().decode(key.ebid);
            final var decryptedEbid = cipherForEbidAtEpoch(epochId).decrypt(ebid);
            final var epochId = Arrays.copyOf(decryptedEbid, 3);
            return ByteUtils.convertEpoch24bitsToInt(epochId);
        }

        @SneakyThrows
        public String getDecryptedBase64IdA() {
            final var ebid = Base64.getDecoder().decode(key.ebid);
            final var decryptedEbid = cipherForEbidAtEpoch(epochId).decrypt(ebid);
            final var idA = Arrays.copyOfRange(decryptedEbid, 3, 8);
            return Base64.getEncoder().encodeToString(idA);
        }

        @SneakyThrows
        public int getDecryptedCountryCode() {
            final var ebid = Base64.getDecoder().decode(key.ebid);
            final var ecc = Base64.getDecoder().decode(key.ecc);

            // Pad to 128-bits
            final var payloadToEncrypt = Arrays.copyOf(ebid, 128 / 8);
            // AES Encryption of the payload to encrypt
            final var encryptedPayload = cipherForEcc().encrypt(payloadToEncrypt);

            // Truncate to 8 bits
            // Equivalent to MSB in ROBert spec
            final var truncatedEncryptedPayload = encryptedPayload[0];
            final var truncatedEncryptedEcc = ecc[0];

            return (truncatedEncryptedPayload ^ truncatedEncryptedEcc);
        }

        public String toString() {
            return format("EphemeralTuple(epochId=%d, key.ebid=%s, key.ecc=%s)", epochId, key.ebid, key.ecc);
        }
    }
}
