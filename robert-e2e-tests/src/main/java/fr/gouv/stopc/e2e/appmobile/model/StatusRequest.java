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

    int epochId;

    /**
     * The encrypted country code (8 bits)
     */
    byte[] ebid;

    /**
     * Hello Message generation time.
     */
    byte[] time;

    /**
     * a HMAC − SHA256(KA,c1 | MA,i) truncated to 40 bits (c1 is the 8-bit prefix
     * ”01”)
     */
    byte[] mac;

    @Override
    public String toString() {
        final var base64 = Base64.getEncoder();
        final var ebidB64 = base64.encodeToString(ebid);
        final var macB64 = base64.encodeToString(mac);
        return String.format("HelloMessage(ebid=%s, ecc=%s, time=%s, mac=%s)", ebidB64, time, macB64);
    }

    public static StatusRequest.StatusRequestBuilder builder(final Instant instantTime, DigestSaltEnum digestSalt,
            byte[] macKey, int epochId) {
        final var macCipher = new CryptoHMACSHA256(macKey);
        final var instantTimes = generateTime32(instantTime);
        return new StatusRequest.StatusRequestBuilderWithMac(digestSalt, macCipher, epochId, instantTimes);
    }

    public static byte[] generateTime32(Instant instantTime) {
        var tsInSeconds = longToBytes(TimeUtils.convertUnixMillistoNtpSeconds(instantTime.toEpochMilli()));
        var time = new byte[4];
        System.arraycopy(tsInSeconds, 4, time, 0, 4);

        return time;
    }

    @RequiredArgsConstructor
    public static class StatusRequestBuilderWithMac extends StatusRequest.StatusRequestBuilder {

        private final DigestSaltEnum digestSalt;

        private final CryptoHMACSHA256 macCipher;

        private final int epochId;

        private final byte[] instantTime;

        @Override
        public StatusRequest.StatusRequestBuilder mac(byte[] mac) {
            throw new UnsupportedOperationException(
                    "'mac' is an attributed derived from 'ebid', 'ecc' and 'time' using builder's 'macKey'"
            );
        }

        public StatusRequest build() {
            final var original = super.build();
            final var ebid = original.getEbid();
            final var time = instantTime;

            // Merge arrays
            var agg = new byte[8 + 4 + 4];
            System.arraycopy(ebid, 0, agg, 0, ebid.length);
            System.arraycopy(ByteUtils.intToBytes(epochId), 0, agg, ebid.length, Integer.BYTES);
            System.arraycopy(time, 0, agg, ebid.length + Integer.BYTES, time.length);

            byte[] mac = new byte[32];
            try {
                mac = this.generateHMAC(
                        macCipher, agg, digestSalt
                );
            } catch (Exception e) {
                log.info("Problem generating SHA256");
            }
            return new StatusRequest(epochId, ebid, time, mac);
        }

        @SneakyThrows
        private byte[] generateHMAC(final CryptoHMACSHA256 cryptoHMACSHA256S, final byte[] argument,
                DigestSaltEnum saltEnum) {
            return cryptoHMACSHA256S.encrypt(addAll(new byte[] { saltEnum.getValue() }, argument));
        }
    }
}
