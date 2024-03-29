package fr.gouv.stopc.e2e.mobileapplication.model;

import fr.gouv.stopc.e2e.external.common.enums.DigestSaltEnum;
import fr.gouv.stopc.e2e.external.common.utils.ByteUtils;
import fr.gouv.stopc.e2e.external.crypto.CryptoHMACSHA256;
import fr.gouv.stopc.e2e.external.crypto.exception.RobertServerCryptoException;
import fr.gouv.stopc.e2e.mobileapplication.EpochClock.RobertInstant;
import fr.gouv.stopc.robert.client.model.AuthentifiedRequest;
import fr.gouv.stopc.robert.client.model.AuthentifiedRequest.AuthentifiedRequestBuilder;
import fr.gouv.stopc.robert.client.model.ExposureStatusRequest;
import fr.gouv.stopc.robert.client.model.ExposureStatusRequest.ExposureStatusRequestBuilder;
import fr.gouv.stopc.robert.client.model.UnregisterRequest;
import fr.gouv.stopc.robert.client.model.UnregisterRequest.UnregisterRequestBuilder;
import lombok.RequiredArgsConstructor;
import lombok.Value;

import static fr.gouv.stopc.e2e.external.common.enums.DigestSaltEnum.*;
import static lombok.AccessLevel.PRIVATE;

/**
 * Robert API requests contains MAC checksums. This class aims to handle
 * identification and MAC checksum attributes.
 */
@RequiredArgsConstructor(access = PRIVATE)
public class RobertRequestBuilder {

    private final CryptoHMACSHA256 macCipher;

    public static RobertRequestBuilder withMacKey(final byte[] macKey) {
        return new RobertRequestBuilder(new CryptoHMACSHA256(macKey));
    }

    public ExposureStatusRequestBuilder exposureStatusRequest(final byte[] ebid, final RobertInstant time) {
        final var auth = new RequestAuth(STATUS, ebid, time);
        return ExposureStatusRequest.builder()
                .ebid(auth.ebid)
                .epochId(auth.epochId)
                .time(auth.time)
                .mac(auth.mac);
    }

    public AuthentifiedRequestBuilder deleteExposureHistory(final byte[] ebid, final RobertInstant time) {
        final var auth = new RequestAuth(DELETE_HISTORY, ebid, time);
        return AuthentifiedRequest.builder()
                .ebid(auth.ebid)
                .epochId(auth.epochId)
                .time(auth.time)
                .mac(auth.mac);
    }

    public UnregisterRequestBuilder unregisterRequest(final byte[] ebid, final RobertInstant time) {
        final var auth = new RequestAuth(UNREGISTER, ebid, time);
        return UnregisterRequest.builder()
                .ebid(auth.ebid)
                .epochId(auth.epochId)
                .time(auth.time)
                .mac(auth.mac);
    }

    /**
     * The structure used for MAC is:
     *
     * <pre>
     *     +----------------------------------------------------------------------------------+
     *     |                             StatusRequest (128 bits)                             |
     *     ++---------------------------------------------------------------------------------+
     *     | FixedPrefix     | EBID          | *EpochId      | Time           | MAC           |
     *     |        (8 bits) |     (64 bits) |      (8 bits) |      (32 bits) |     (32 bits) |
     *     ++---------------------------------------------------------------------------------+
     * </pre>
     * <p>
     * Note about EpochId: The Robert Protocol documentation doesn't mention the use
     * of the EpochId in the MAC checksum computation but the actual Robert server
     * implementation use it.
     *
     * @see <a href=
     *      "https://github.com/ROBERT-proximity-tracing/documents/blob/master/ROBERT-specification-EN-v1_1.pdf">Robert
     *      Protocol 1.1</a> §7 Exposure Status Request (ESR)
     */
    @Value
    private class RequestAuth {

        /**
         * Ephemeral Bluetooth ID at epoch i, generated by the back-end (64 bits)
         */
        byte[] ebid;

        /**
         * The epoch identifier (8 bits)
         */
        int epochId;

        /**
         * Status Request generation time.
         */
        byte[] time;

        /**
         * mac = HMAC -SHA256(KA; c2 j EBIDA;i j Time)]
         */
        byte[] mac;

        private RequestAuth(final DigestSaltEnum fixedPrefix, final byte[] ebid, final RobertInstant time) {
            final var fixedPrefixBytes = new byte[] { fixedPrefix.getValue() };

            final var epochIdBytes = ByteUtils.intToBytes(time.asEpochId());

            final var timeAs64BitsNtpTimestamp = ByteUtils.longToBytes(time.asNtpTimestamp());
            final var time32MostSignificantBits = new byte[4];
            System.arraycopy(timeAs64BitsNtpTimestamp, 4, time32MostSignificantBits, 0, 4);

            // value to use for MAC checksum
            var macSource = new byte[fixedPrefixBytes.length + ebid.length + epochIdBytes.length
                    + time32MostSignificantBits.length];
            System.arraycopy(fixedPrefixBytes, 0, macSource, 0, fixedPrefixBytes.length);
            System.arraycopy(ebid, 0, macSource, fixedPrefixBytes.length, ebid.length);
            System.arraycopy(epochIdBytes, 0, macSource, fixedPrefixBytes.length + ebid.length, Integer.BYTES);
            System.arraycopy(
                    time32MostSignificantBits, 0, macSource,
                    fixedPrefixBytes.length + ebid.length + epochIdBytes.length, time32MostSignificantBits.length
            );

            try {
                final byte[] mac = macCipher.encrypt(macSource);
                this.ebid = ebid;
                this.epochId = time.asEpochId();
                this.time = time32MostSignificantBits;
                this.mac = mac;
            } catch (RobertServerCryptoException e) {
                throw new RuntimeException("error computing the HMAC", e);
            }
        }
    }
}
