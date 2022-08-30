package fr.gouv.stopc.robert.crypto.grpc.server.test.matchers;

import fr.gouv.stopc.robert.server.common.service.RobertClock;
import fr.gouv.stopc.robert.server.common.utils.ByteUtils;
import fr.gouv.stopc.robert.server.crypto.structure.impl.CryptoAESECB;
import fr.gouv.stopc.robert.server.crypto.structure.impl.CryptoSkinny64;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeDiagnosingMatcher;

import java.util.Base64;

import static fr.gouv.stopc.robert.crypto.grpc.server.test.CryptoManager.decryptCountryCode;
import static fr.gouv.stopc.robert.crypto.grpc.server.test.DataManager.SERVER_COUNTRY_CODE;
import static fr.gouv.stopc.robert.crypto.grpc.server.test.KeystoreManager.getFederationKey;
import static fr.gouv.stopc.robert.crypto.grpc.server.test.KeystoreManager.getServerKey;
import static java.time.ZoneOffset.UTC;

@Slf4j
public class EphemeralTupleMatcher extends TypeSafeDiagnosingMatcher<EphemeralTupleMatcher.EphemeralTuple> {

    private byte[] serverKey;

    private byte[] federationKey;

    private final RobertClock clock;

    public EphemeralTupleMatcher(final RobertClock clock) {
        this.clock = clock;
        this.federationKey = getFederationKey().getEncoded();
    }

    public static EphemeralTupleMatcher isValidTuple(final RobertClock clock) {
        return new EphemeralTupleMatcher(clock);
    }

    @SneakyThrows
    @Override
    protected boolean matchesSafely(final EphemeralTuple item, final Description mismatchDescription) {

        final var dateOfTupleEpochId = clock.atEpoch(item.getEpochId()).asInstant().atOffset(UTC).toLocalDate();
        this.serverKey = getServerKey(dateOfTupleEpochId).getEncoded();

        final var cryptoServerKey = new CryptoSkinny64(serverKey);
        final var decodedEbid = Base64.getDecoder().decode(item.getKey().ebid);
        final var decryptedEbid = cryptoServerKey.decrypt(decodedEbid);

        int epochIdFromDecryptedEbid = getEpochIdFromDecryptedEBID(decryptedEbid);
        if (epochIdFromDecryptedEbid != item.getEpochId()) {
            mismatchDescription.appendText("was an ephemeral tuple with an ebid containing epochId ")
                    .appendValue(epochIdFromDecryptedEbid)
                    .appendText(" instead of the epochId of the tuple : ")
                    .appendValue(item.getEpochId());
            return false;
        }

        final var decodedEcc = Base64.getDecoder().decode(item.getKey().ecc)[0];
        final var federationKeyForEcc = new CryptoAESECB(federationKey);
        final var decryptEcc = decryptCountryCode(federationKeyForEcc, decodedEbid, decodedEcc);

        if (SERVER_COUNTRY_CODE != decryptEcc) {
            mismatchDescription.appendText("was an ephemeral tuple with country code ")
                    .appendValue(decryptEcc)
                    .appendText(" instead of country code ")
                    .appendValue(SERVER_COUNTRY_CODE);
            return false;
        }

        return true;
    }

    @Override
    public void describeTo(final Description description) {
        description.appendText("a consistent ephemeral tuple");
    }

    private static int getEpochIdFromDecryptedEBID(byte[] ebid) {
        byte[] epochId = new byte[3];
        System.arraycopy(ebid, 0, epochId, 0, epochId.length);
        return ByteUtils.convertEpoch24bitsToInt(epochId);
    }

    @Value
    public static class EphemeralTuple {

        int epochId;

        TupleKey key;

    }

    @Value
    public static class TupleKey {

        String ebid;

        String ecc;
    }
}
