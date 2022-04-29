package fr.gouv.stopc.robert.crypto.grpc.server.test.matchers;

import fr.gouv.stopc.robert.server.common.service.RobertClock;
import fr.gouv.stopc.robert.server.common.utils.ByteUtils;
import fr.gouv.stopc.robert.server.crypto.service.CryptoService;
import fr.gouv.stopc.robert.server.crypto.structure.impl.CryptoAESECB;
import fr.gouv.stopc.robert.server.crypto.structure.impl.CryptoSkinny64;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeDiagnosingMatcher;

import java.util.Base64;

import static fr.gouv.stopc.robert.crypto.grpc.server.test.DataManager.SERVER_COUNTRY_CODE;
import static fr.gouv.stopc.robert.crypto.grpc.server.test.KeystoreManager.getFederationKey;
import static fr.gouv.stopc.robert.crypto.grpc.server.test.KeystoreManager.getServerKey;
import static java.time.ZoneOffset.UTC;

@Slf4j
public class EphemeralTupleMatcher extends TypeSafeDiagnosingMatcher<EphemeralTupleMatcher.EphemeralTuple> {

    private byte[] serverKey;

    private byte[] federationKey;

    private final CryptoService cryptoService;

    private final RobertClock clock;

    public EphemeralTupleMatcher(final CryptoService cryptoService, final RobertClock clock) {
        this.cryptoService = cryptoService;
        this.clock = clock;
        this.federationKey = getFederationKey().getEncoded();
    }

    public static EphemeralTupleMatcher isValidTuple(final CryptoService cryptoService, final RobertClock clock) {
        return new EphemeralTupleMatcher(cryptoService, clock);
    }

    @SneakyThrows
    @Override
    protected boolean matchesSafely(final EphemeralTuple item, final Description mismatchDescription) {

        final var dateOfTupleEpochId = clock.atEpoch(item.getEpochId()).asInstant().atOffset(UTC).toLocalDate();
        this.serverKey = getServerKey(dateOfTupleEpochId).getEncoded();

        final var cryptoServerKey = new CryptoSkinny64(serverKey);
        final var decodedEbid = Base64.getDecoder().decode(item.getKey().ebid);
        final var decryptedEbid = cryptoServerKey.decrypt(decodedEbid);

        int epochIdFromMessage = getEpochIdFromDecryptedEBID(decryptedEbid);
        if (epochIdFromMessage != item.getEpochId()) {
            mismatchDescription.appendText("a ephemeral tuple with epochId ")
                    .appendValue(item.getEpochId())
                    .appendText(" and ebid ")
                    .appendValue(item.getKey().ebid)
                    .appendText(" and with an ebid containing epochId ")
                    .appendValue(epochIdFromMessage);
            return false;
        }

        final var decodedEcc = Base64.getDecoder().decode(item.getKey().ecc)[0];
        final var federationKeyForEcc = new CryptoAESECB(federationKey);
        final var decryptEcc = cryptoService.decryptCountryCode(federationKeyForEcc, decodedEbid, decodedEcc);

        if (SERVER_COUNTRY_CODE != decryptEcc[0]) {
            mismatchDescription.appendText("a ephemeral tuple with country code ")
                    .appendValue(decryptEcc[0])
                    .appendText("instead of country code ")
                    .appendValue(SERVER_COUNTRY_CODE);
            return false;
        }

        return true;
    }

    @Override
    public void describeTo(final Description description) {
        description.appendText("a consistent ephemeral tuple ");
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
