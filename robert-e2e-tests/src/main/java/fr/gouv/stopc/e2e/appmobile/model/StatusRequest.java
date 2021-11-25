package fr.gouv.stopc.e2e.appmobile.model;

import fr.gouv.stopc.e2e.external.common.enums.DigestSaltEnum;
import fr.gouv.stopc.e2e.external.common.utils.ByteUtils;
import fr.gouv.stopc.e2e.external.common.utils.TimeUtils;
import fr.gouv.stopc.e2e.external.crypto.CryptoHMACSHA256;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.Base64;

import static fr.gouv.stopc.e2e.external.common.utils.ByteUtils.addAll;
import static fr.gouv.stopc.e2e.external.common.utils.ByteUtils.longToBytes;

@Value
@Builder
@Slf4j
public class StatusRequest {

    /**
     * The epoch identifier (number of epoch between startup and now)
     */
    int epochId;

    /**
     * The encrypted country code (8 bits)
     */
    byte[] ebid;

    /**
     * Status Request generation time.
     */
    byte[] time;

    /**
     * a HMAC − SHA256(ebid, epochId, date)
     */
    byte[] mac;

    @Override
    public String toString() {
        final var base64 = Base64.getEncoder();
        final var ebidB64 = base64.encodeToString(ebid);
        final var macB64 = base64.encodeToString(mac);
        return String
                .format("StatusRequest(epoch=%s, ebid=%s, time=%s, mac=%s)", epochId, ebidB64, time.toString(), macB64);
    }

    public static StatusRequest.StatusRequestBuilder builder(final Instant date, DigestSaltEnum digestSalt,
            byte[] macKey, int epochId) {
        final var macCipher = new CryptoHMACSHA256(macKey);
        final var byteTime = generateTime32(date);
        return new StatusRequest.StatusRequestBuilderWithMac(digestSalt, macCipher, epochId, byteTime);
    }

    public static byte[] generateTime32(Instant date) {
        var tsInSeconds = longToBytes(TimeUtils.convertUnixMillistoNtpSeconds(date.toEpochMilli()));
        var byteTime = new byte[4];
        System.arraycopy(tsInSeconds, 4, byteTime, 0, 4);

        return byteTime;
    }

    @RequiredArgsConstructor
    public static class StatusRequestBuilderWithMac extends StatusRequest.StatusRequestBuilder {

        private final DigestSaltEnum digestSalt;

        private final CryptoHMACSHA256 macCipher;

        private final int epochId;

        private final byte[] byteDate;

        @Override
        public StatusRequest.StatusRequestBuilder mac(byte[] mac) {
            throw new UnsupportedOperationException(
                    "'mac' is an attributed derived from 'ebid', 'epochId' and 'time' using builder's 'macKey'"
            );
        }

        public StatusRequest build() {
            final var original = super.build();
            final var ebid = original.getEbid();

            // Merge arrays
            var agg = new byte[ebid.length + Integer.BYTES + byteDate.length];
            System.arraycopy(ebid, 0, agg, 0, ebid.length);
            System.arraycopy(ByteUtils.intToBytes(epochId), 0, agg, ebid.length, Integer.BYTES);
            System.arraycopy(byteDate, 0, agg, ebid.length + Integer.BYTES, byteDate.length);

            byte[] mac = new byte[32];
            try {
                mac = this.generateHMAC(
                        macCipher, agg, digestSalt
                );
            } catch (Exception e) {
                log.info("Problem generating SHA256");
            }
            return new StatusRequest(epochId, ebid, byteDate, mac);
        }

        @SneakyThrows
        private byte[] generateHMAC(final CryptoHMACSHA256 cryptoHMACSHA256S, final byte[] argument,
                DigestSaltEnum saltEnum) {
            return cryptoHMACSHA256S.encrypt(addAll(new byte[] { saltEnum.getValue() }, argument));
        }
    }
}
