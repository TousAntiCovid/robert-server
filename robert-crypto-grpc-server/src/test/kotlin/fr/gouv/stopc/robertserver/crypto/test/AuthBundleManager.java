package fr.gouv.stopc.robertserver.crypto.test;

import com.google.protobuf.ByteString;
import fr.gouv.stopc.robert.server.common.DigestSaltEnum;
import fr.gouv.stopc.robert.server.common.utils.ByteUtils;
import fr.gouv.stopc.robertserver.common.RobertClock;
import lombok.Builder;
import lombok.SneakyThrows;
import lombok.Value;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.stream.Stream;

import static fr.gouv.stopc.robertserver.crypto.test.ClockManager.clock;
import static fr.gouv.stopc.robertserver.crypto.test.KeystoreManager.cipherForEbidAtEpoch;
import static fr.gouv.stopc.robertserver.crypto.test.PostgreSqlManager.getCipherForMac;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.MINUTES;

public class AuthBundleManager {

    public static Stream<AuthBundle> valid_auth_bundle() {
        return Arrays.stream(DigestSaltEnum.values())
                .flatMap(AuthBundleManager::valid_auth_bundle);
    }

    public static Stream<AuthBundle> valid_auth_bundle(final DigestSaltEnum requestType) {
        final var now = clock().now();
        return Stream.of(
                AuthBundle.withDefaults("regular auth attributes")
                        .requestType(requestType)
                        .build(),
                AuthBundle.withDefaults("use current time but epoch-1")
                        .requestType(requestType)
                        .epochId(now.asEpochId() - 1)
                        .build(),
                AuthBundle.withDefaults("use current time but epoch-10")
                        .requestType(requestType)
                        .epochId(now.asEpochId() - 10)
                        .build(),
                AuthBundle.withDefaults("use current time but epoch-2days")
                        .requestType(requestType)
                        .epochId(now.minus(2, DAYS).asEpochId())
                        .build(),
                AuthBundle.withDefaults("use current time but epoch+1")
                        .requestType(requestType)
                        .epochId(now.asEpochId() + 1)
                        .build(),
                AuthBundle.withDefaults("use current time but epoch+10")
                        .requestType(requestType)
                        .epochId(now.asEpochId() + 10)
                        .build(),
                AuthBundle.withDefaults("use current time but epoch+2days")
                        .requestType(requestType)
                        .epochId(now.plus(2, DAYS).asEpochId())
                        .build(),
                AuthBundle.withDefaults("use current epoch but time-2days")
                        .requestType(requestType)
                        .time(now.plus(-2, DAYS))
                        .build(),
                AuthBundle.withDefaults("use current epoch but time-10min")
                        .requestType(requestType)
                        .time(now.plus(-10, MINUTES))
                        .build(),
                AuthBundle.withDefaults("use current epoch but time+10min")
                        .requestType(requestType)
                        .time(now.plus(10, MINUTES))
                        .build(),
                AuthBundle.withDefaults("use current epoch but time+2days")
                        .requestType(requestType)
                        .time(now.plus(2, DAYS))
                        .build(),
                AuthBundle.withDefaults("use current epoch but time at NTP timestamp=0")
                        .requestType(requestType)
                        .time(clock().atNtpTimestamp(0))
                        .build()
        );
    }

    @Value
    @Builder(toBuilder = true)
    public static class AuthBundle {

        String title;

        DigestSaltEnum requestType;

        String idA;

        RobertClock.RobertInstant time;

        Integer epochId;

        public static AuthBundleBuilder withDefaults(final String title) {
            final var now = clock().now();
            final var randomIdA = new byte[5];
            new SecureRandom().nextBytes(randomIdA);
            return builder()
                    .title(title)
                    .idA(Base64.getEncoder().encodeToString(randomIdA))
                    .timeAndEpoch(now);
        }

        public static class AuthBundleBuilder {

            public AuthBundleBuilder timeAndEpoch(final RobertClock.RobertInstant time) {
                this.time = time;
                this.epochId = time.asEpochId();
                return this;
            }
        }

        /**
         * Assemble and encrypt epochId and idA to return an EBID.
         * <p>
         * The unencrypted EBID is made of 8 bytes.
         * <pre>
         *     +---------------------------------+
         *     | Unencrypted EBID                |
         *     +------------+--------------------+
         *     | epochId    | idA                |
         *     |  (24 bits) |          (40 bits) |
         *     +------------+--------------------+
         * </pre>
         *
         * @see <a href=
         * "https://github.com/ROBERT-proximity-tracing/documents/blob/master/ROBERT-specification-EN-v1_1.pdf">Robert
         * Protocol 1.1</a> §4
         */
        @SneakyThrows
        public ByteString getEbid() {
            final var idABytes = Base64.getDecoder().decode(idA);
            final var ebid = new byte[8];
            System.arraycopy(ByteUtils.intToBytes(epochId), 1, ebid, 0, 3);
            System.arraycopy(idABytes, 0, ebid, 3, 5);
            final var encryptedEbid = cipherForEbidAtEpoch(epochId).encrypt(ebid);
            return ByteString.copyFrom(encryptedEbid);
        }

        /**
         * Computes the MAC for this EBID, epochId and time.
         * <pre>
         *     +----------------------------------------------------------------+
         *     |                    MAC structure (128 bits)                    |
         *     +----------------------------------------------------------------+
         *     | Req type     | EBID           | Epoch          | Time          |
         *     |     (8 bits) |      (64 bits) |      (32 bits) |     (32 bits) |
         *     +----------------------------------------------------------------+
         * </pre>
         *
         * @see <a href=
         * "https://github.com/ROBERT-proximity-tracing/documents/blob/master/ROBERT-specification-EN-v1_1.pdf">Robert
         * Protocol 1.1</a> §7 and §C
         */
        @SneakyThrows
        public ByteString getMac() {
            final var ebid = getEbid().toByteArray();
            final var data = new byte[1 + 8 + Integer.BYTES + Integer.BYTES];
            data[0] = requestType.getValue();
            System.arraycopy(ebid, 0, data, 1, 8);
            System.arraycopy(ByteUtils.intToBytes(epochId), 0, data, 1 + ebid.length, Integer.BYTES);
            System.arraycopy(time.asTime32(), 0, data, 1 + ebid.length + Integer.BYTES, Integer.BYTES);
            final var mac = getCipherForMac(idA).encrypt(data);
            return ByteString.copyFrom(mac);
        }

        public String toString() {
            return String.format(
                    "%s (idA=%s, requestType=%s, time=%s, epochId=%d)", title, idA, requestType, time,
                    epochId
            );
        }
    }
}
