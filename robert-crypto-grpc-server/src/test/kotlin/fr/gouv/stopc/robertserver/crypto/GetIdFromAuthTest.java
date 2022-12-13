package fr.gouv.stopc.robertserver.crypto;

import com.google.protobuf.ByteString;
import fr.gouv.stopc.robert.crypto.grpc.server.client.service.ICryptoServerGrpcClient;
import fr.gouv.stopc.robert.crypto.grpc.server.client.service.impl.CryptoServerGrpcClient;
import fr.gouv.stopc.robert.crypto.grpc.server.messaging.GetIdFromAuthRequest;
import fr.gouv.stopc.robertserver.crypto.test.IntegrationTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.security.SecureRandom;
import java.util.Arrays;

import static fr.gouv.stopc.robertserver.crypto.test.AuthBundleManager.AuthBundle;
import static fr.gouv.stopc.robertserver.crypto.test.ClockManager.clock;
import static fr.gouv.stopc.robertserver.crypto.test.PostgreSqlManager.givenIdentityDoesntExistForIdA;
import static fr.gouv.stopc.robertserver.crypto.test.PostgreSqlManager.givenIdentityExistsForIdA;
import static fr.gouv.stopc.robertserver.crypto.test.matchers.GrpcResponseMatcher.*;
import static java.time.temporal.ChronoUnit.DAYS;
import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
class GetIdFromAuthTest {

    private final ICryptoServerGrpcClient robertCryptoClient = new CryptoServerGrpcClient("localhost", 9090);

    /**
     * Returns a GetIdFromAuthRequest builder with acceptable default values.
     */
    static GetIdFromAuthRequest.Builder givenAuthRequest(AuthBundle auth) {
        return GetIdFromAuthRequest.newBuilder()
                .setEbid(auth.getEbid())
                .setEpochId(auth.getEpochId())
                .setTime(auth.getTime().asNtpTimestamp())
                .setMac(auth.getMac())
                .setRequestType(auth.getRequestType().getValue());
    }

    @ParameterizedTest
    @MethodSource("fr.gouv.stopc.robertserver.crypto.test.AuthBundleManager#valid_auth_bundle")
    void can_authenticate_valid_request(final AuthBundle auth) {
        givenIdentityExistsForIdA(auth.getIdA());

        final var request = givenAuthRequest(auth)
                .build();

        final var response = robertCryptoClient.getIdFromAuth(request)
                .orElseThrow();

        assertThat(response)
                .has(noGrpcError())
                .has(grpcBinaryField("idA", auth.getIdA()))
                .has(grpcField("epochId", auth.getEpochId()));

    }

    @ParameterizedTest
    @MethodSource("fr.gouv.stopc.robertserver.crypto.test.AuthBundleManager#valid_auth_bundle")
    void cant_authenticate_with_unknown_request_type(final AuthBundle auth) {
        givenIdentityExistsForIdA(auth.getIdA());

        final var request = givenAuthRequest(auth)
                .setRequestType(0)
                .build();

        final var response = robertCryptoClient.getIdFromAuth(request)
                .orElseThrow();

        assertThat(response)
                .is(grpcErrorResponse(400, "Unknown request type 0"));
    }

    @ParameterizedTest
    @MethodSource("fr.gouv.stopc.robertserver.crypto.test.AuthBundleManager#valid_auth_bundle")
    void cant_authenticate_with_malformed_ebid(final AuthBundle auth) {
        givenIdentityExistsForIdA(auth.getIdA());
        final var malformedEbid = new byte[8];
        new SecureRandom().nextBytes(malformedEbid);

        final var request = givenAuthRequest(auth)
                .setEbid(ByteString.copyFrom(malformedEbid))
                .build();

        final var response = robertCryptoClient.getIdFromAuth(request)
                .orElseThrow();

        assertThat(response)
                .is(grpcErrorResponse(400, "Could not decrypt ebid content"));
    }

    @ParameterizedTest
    @MethodSource("fr.gouv.stopc.robertserver.crypto.test.AuthBundleManager#valid_auth_bundle")
    void cant_authenticate_with_an_epoch_different_from_ebid(final AuthBundle auth) {
        givenIdentityExistsForIdA(auth.getIdA());
        final var inconsistentEpoch = auth.getEpochId() - 1;

        final var request = givenAuthRequest(auth)
                .setEpochId(inconsistentEpoch)
                .build();

        final var response = robertCryptoClient.getIdFromAuth(request)
                .orElseThrow();

        assertThat(response)
                .is(grpcErrorResponse(400, "Could not decrypt ebid content"));
    }

    @ParameterizedTest
    @MethodSource("fr.gouv.stopc.robertserver.crypto.test.AuthBundleManager#valid_auth_bundle")
    void cant_authenticate_when_the_ebid_belongs_to_an_epoch_at_a_date_the_server_is_missing_the_serverkey(
            final AuthBundle auth) {
        final var oneMonthOldAuth = auth.toBuilder()
                .title("30 days in the past: " + auth.getTitle())
                .timeAndEpoch(clock().now().minus(30, DAYS))
                .build();
        givenIdentityExistsForIdA(oneMonthOldAuth.getIdA());

        final var request = givenAuthRequest(oneMonthOldAuth)
                .build();

        final var response = robertCryptoClient.getIdFromAuth(request)
                .orElseThrow();

        assertThat(response)
                .is(
                        grpcErrorResponse(
                                430, "No server key found from cryptographic storage : %s",
                                oneMonthOldAuth.getEpochId()
                        )
                );
    }

    @ParameterizedTest
    @MethodSource("fr.gouv.stopc.robertserver.crypto.test.AuthBundleManager#valid_auth_bundle")
    void cant_authenticate_with_unknown_idA(final AuthBundle auth) {
        // create the identity to be able to compute mac with regular tools
        givenIdentityExistsForIdA(auth.getIdA());

        final var request = givenAuthRequest(auth)
                .build();

        // delete the identity to simulate an unknown idA
        givenIdentityDoesntExistForIdA(auth.getIdA());

        final var response = robertCryptoClient.getIdFromAuth(request)
                .orElseThrow();

        assertThat(response)
                .is(grpcErrorResponse(404, "Could not find id"));
    }

    @ParameterizedTest
    @MethodSource("fr.gouv.stopc.robertserver.crypto.test.AuthBundleManager#valid_auth_bundle")
    void cant_authenticate_with_incorrect_mac(final AuthBundle auth) {
        givenIdentityExistsForIdA(auth.getIdA());

        final var request = givenAuthRequest(auth)
                .setMac(ByteString.copyFromUtf8("Incorrect MAC value"))
                .build();

        final var response = robertCryptoClient.getIdFromAuth(request)
                .orElseThrow();

        assertThat(response)
                .is(grpcErrorResponse(400, "Invalid MAC"));

    }

    @ParameterizedTest
    @MethodSource("fr.gouv.stopc.robertserver.crypto.test.AuthBundleManager#valid_auth_bundle")
    void cant_authenticate_with_an_ebid_larger_than_64bits(final AuthBundle auth) {
        givenIdentityExistsForIdA(auth.getIdA());

        final var largerEbid = Arrays.copyOf(auth.getEbid().toByteArray(), 9);

        final var request = givenAuthRequest(auth)
                .setEbid(ByteString.copyFrom(largerEbid))
                .build();

        final var response = robertCryptoClient.getIdFromAuth(request)
                .orElseThrow();

        assertThat(response)
                .is(grpcErrorResponse(500, "Error validating authenticated request"));
    }
}
