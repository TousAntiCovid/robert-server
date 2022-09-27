package fr.gouv.stopc.robertserver.crypto;

import com.google.protobuf.ByteString;
import fr.gouv.stopc.robert.crypto.grpc.server.client.service.ICryptoServerGrpcClient;
import fr.gouv.stopc.robert.crypto.grpc.server.client.service.impl.CryptoServerGrpcClient;
import fr.gouv.stopc.robert.crypto.grpc.server.messaging.GetIdFromAuthRequest;
import fr.gouv.stopc.robert.server.common.DigestSaltEnum;
import fr.gouv.stopc.robert.server.common.service.RobertClock.RobertInstant;
import fr.gouv.stopc.robert.server.common.utils.ByteUtils;
import fr.gouv.stopc.robertserver.crypto.test.IntegrationTest;
import lombok.Builder;
import lombok.SneakyThrows;
import lombok.Value;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.stream.Stream;

import static fr.gouv.stopc.robertserver.crypto.test.ClockManager.clock;
import static fr.gouv.stopc.robertserver.crypto.test.KeystoreManager.cipherForEbidAtEpoch;
import static fr.gouv.stopc.robertserver.crypto.test.PostgreSqlManager.*;
import static fr.gouv.stopc.robertserver.crypto.test.matchers.GrpcResponseMatcher.*;
import static java.time.temporal.ChronoUnit.*;
import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
public class GetIdFromAuthTest {

    private final ICryptoServerGrpcClient robertCryptoClient = new CryptoServerGrpcClient("localhost", 9090);

    @Value
    @Builder(toBuilder = true)
    static class AuthBundle {

        String title;

        DigestSaltEnum requestType;

        String idA;

        RobertInstant time;

        Integer epochId;

        public static AuthBundleBuilder withDefaults(String title) {
            final var now = clock().now();
            final var randomIdA = new byte[5];
            new SecureRandom().nextBytes(randomIdA);
            return builder()
                    .title(title)
                    .idA(Base64.getEncoder().encodeToString(randomIdA))
                    .timeAndEpoch(now);
        }

        public static class AuthBundleBuilder {

            public AuthBundleBuilder timeAndEpoch(RobertInstant time) {
                this.time = time;
                this.epochId = time.asEpochId();
                return this;
            }
        }

        /**
         * Assemble and encrypt epochId and idA to return an EBID.
         * <p>
         * The unencrypted EBID is made of 8 bytes.
         * 
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
         *      "https://github.com/ROBERT-proximity-tracing/documents/blob/master/ROBERT-specification-EN-v1_1.pdf">Robert
         *      Protocol 1.1</a> §4
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
         * 
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
         *      "https://github.com/ROBERT-proximity-tracing/documents/blob/master/ROBERT-specification-EN-v1_1.pdf">Robert
         *      Protocol 1.1</a> §7 and §C
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

    static Stream<AuthBundle> valid_auth_bundle() {
        final var now = clock().now();
        return Arrays.stream(DigestSaltEnum.values())
                .flatMap(
                        requestType -> Stream.of(
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
                        )
                );
    }

    @ParameterizedTest
    @MethodSource("valid_auth_bundle")
    void can_authenticate_valid_request(final AuthBundle auth) {
        givenIdentityExistsForIdA(auth.getIdA());

        final var request = GetIdFromAuthRequest.newBuilder()
                .setEbid(auth.getEbid())
                .setEpochId(auth.getEpochId())
                .setTime(auth.getTime().asNtpTimestamp())
                .setMac(auth.getMac())
                .setRequestType(auth.getRequestType().getValue())
                .build();

        final var response = robertCryptoClient.getIdFromAuth(request).orElseThrow();

        assertThat(response)
                .has(noGrpcError())
                .has(grpcBinaryField("idA", auth.getIdA()))
                .has(grpcField("epochId", auth.getEpochId()));

    }

    @ParameterizedTest
    @MethodSource("valid_auth_bundle")
    void cant_authenticate_with_unknown_request_type(final AuthBundle auth) {
        givenIdentityExistsForIdA(auth.getIdA());

        final var request = GetIdFromAuthRequest.newBuilder()
                .setEbid(auth.getEbid())
                .setEpochId(auth.getEpochId())
                .setTime(auth.getTime().asNtpTimestamp())
                .setMac(auth.getMac())
                .setRequestType(0)
                .build();

        final var response = robertCryptoClient.getIdFromAuth(request).orElseThrow();

        assertThat(response)
                .is(grpcErrorResponse(400, "Unknown request type 0"));
    }

    @ParameterizedTest
    @MethodSource("valid_auth_bundle")
    void cant_authenticate_with_malformed_ebid(final AuthBundle auth) {
        givenIdentityExistsForIdA(auth.getIdA());
        final var malformedEbid = new byte[8];
        new SecureRandom().nextBytes(malformedEbid);

        final var request = GetIdFromAuthRequest.newBuilder()
                .setEbid(ByteString.copyFrom(malformedEbid))
                .setEpochId(auth.getEpochId())
                .setTime(auth.getTime().asNtpTimestamp())
                .setMac(auth.getMac())
                .setRequestType(auth.getRequestType().getValue())
                .build();

        final var response = robertCryptoClient.getIdFromAuth(request).orElseThrow();

        assertThat(response)
                .is(grpcErrorResponse(400, "Could not decrypt ebid content"));
    }

    @ParameterizedTest
    @MethodSource("valid_auth_bundle")
    void cant_authenticate_with_an_epoch_different_from_ebid(final AuthBundle auth) {
        givenIdentityExistsForIdA(auth.getIdA());
        final var inconsistentEpoch = auth.getEpochId() - 1;

        final var request = GetIdFromAuthRequest.newBuilder()
                .setEbid(auth.getEbid())
                .setEpochId(inconsistentEpoch)
                .setTime(auth.getTime().asNtpTimestamp())
                .setMac(auth.getMac())
                .setRequestType(auth.getRequestType().getValue())
                .build();

        final var response = robertCryptoClient.getIdFromAuth(request).orElseThrow();

        assertThat(response)
                .is(grpcErrorResponse(400, "Could not decrypt ebid content"));
    }

    @ParameterizedTest
    @MethodSource("valid_auth_bundle")
    void cant_authenticate_when_the_ebid_belongs_to_an_epoch_at_a_date_the_server_is_missing_the_serverkey(
            final AuthBundle auth) {
        final var oneMonthOldAuth = auth.toBuilder()
                .title("30 days in the past: " + auth.getTitle())
                .timeAndEpoch(clock().now().minus(30, DAYS))
                .build();
        givenIdentityExistsForIdA(oneMonthOldAuth.getIdA());

        final var request = GetIdFromAuthRequest.newBuilder()
                .setEbid(oneMonthOldAuth.getEbid())
                .setEpochId(oneMonthOldAuth.getEpochId())
                .setTime(oneMonthOldAuth.getTime().asNtpTimestamp())
                .setMac(oneMonthOldAuth.getMac())
                .setRequestType(oneMonthOldAuth.getRequestType().getValue())
                .build();

        final var response = robertCryptoClient.getIdFromAuth(request).orElseThrow();

        assertThat(response)
                .is(
                        grpcErrorResponse(
                                430, "No server key found from cryptographic storage : %s",
                                oneMonthOldAuth.getEpochId()
                        )
                );
    }

    @ParameterizedTest
    @MethodSource("valid_auth_bundle")
    void cant_authenticate_with_unknown_idA(final AuthBundle auth) {
        // create the identity to be able to compute mac with regular tools
        givenIdentityExistsForIdA(auth.getIdA());
        final var request = GetIdFromAuthRequest.newBuilder()
                .setEbid(auth.getEbid())
                .setEpochId(auth.getEpochId())
                .setTime(auth.getTime().asNtpTimestamp())
                .setMac(auth.getMac())
                .setRequestType(auth.getRequestType().getValue())
                .build();

        // delete the identity to simulate an unknown idA
        givenIdentityDoesntExistForIdA(auth.getIdA());

        final var response = robertCryptoClient.getIdFromAuth(request).orElseThrow();

        assertThat(response)
                .is(grpcErrorResponse(404, "Could not find id"));
    }

    @ParameterizedTest
    @MethodSource("valid_auth_bundle")
    void cant_authenticate_with_incorrect_mac(final AuthBundle auth) {
        givenIdentityExistsForIdA(auth.getIdA());

        final var request = GetIdFromAuthRequest
                .newBuilder()
                .setEbid(auth.getEbid())
                .setEpochId(auth.getEpochId())
                .setTime(auth.getTime().asNtpTimestamp())
                .setMac(ByteString.copyFromUtf8("Incorrect MAC value"))
                .setRequestType(auth.getRequestType().getValue())
                .build();

        final var response = robertCryptoClient.getIdFromAuth(request).orElseThrow();

        assertThat(response)
                .is(grpcErrorResponse(400, "Invalid MAC"));

    }

    @ParameterizedTest
    @MethodSource("valid_auth_bundle")
    void cant_authenticate_with_an_ebid_larger_than_64bits(final AuthBundle auth) {
        givenIdentityExistsForIdA(auth.getIdA());

        final var largerEbid = Arrays.copyOf(auth.getEbid().toByteArray(), 9);

        final var request = GetIdFromAuthRequest
                .newBuilder()
                .setEbid(ByteString.copyFrom(largerEbid))
                .setEpochId(auth.getEpochId())
                .setTime(auth.getTime().asNtpTimestamp())
                .setMac(auth.getMac())
                .setRequestType(auth.getRequestType().getValue())
                .build();

        final var response = robertCryptoClient.getIdFromAuth(request).orElseThrow();

        assertThat(response)
                .is(grpcErrorResponse(500, "Error validating authenticated request"));
    }
}
