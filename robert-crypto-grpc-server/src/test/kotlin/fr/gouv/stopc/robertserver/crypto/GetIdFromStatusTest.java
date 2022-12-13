package fr.gouv.stopc.robertserver.crypto;

import com.google.protobuf.ByteString;
import fr.gouv.stopc.robert.crypto.grpc.server.client.service.ICryptoServerGrpcClient;
import fr.gouv.stopc.robert.crypto.grpc.server.client.service.impl.CryptoServerGrpcClient;
import fr.gouv.stopc.robert.crypto.grpc.server.messaging.GetIdFromStatusRequest;
import fr.gouv.stopc.robert.server.common.DigestSaltEnum;
import fr.gouv.stopc.robertserver.crypto.test.AuthBundleManager;
import fr.gouv.stopc.robertserver.crypto.test.IntegrationTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.stream.Stream;

import static fr.gouv.stopc.robert.server.common.DigestSaltEnum.STATUS;
import static fr.gouv.stopc.robertserver.crypto.test.AuthBundleManager.AuthBundle;
import static fr.gouv.stopc.robertserver.crypto.test.AuthBundleManager.valid_auth_bundle;
import static fr.gouv.stopc.robertserver.crypto.test.ClockManager.clock;
import static fr.gouv.stopc.robertserver.crypto.test.CountryCode.FRANCE;
import static fr.gouv.stopc.robertserver.crypto.test.PostgreSqlManager.*;
import static fr.gouv.stopc.robertserver.crypto.test.matchers.EncryptedTuplesBundleMatcher.*;
import static fr.gouv.stopc.robertserver.crypto.test.matchers.GrpcResponseMatcher.grpcErrorResponse;
import static fr.gouv.stopc.robertserver.crypto.test.matchers.GrpcResponseMatcher.noGrpcError;
import static java.time.temporal.ChronoUnit.DAYS;
import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
class GetIdFromStatusTest {

    private final ICryptoServerGrpcClient robertCryptoClient = new CryptoServerGrpcClient("localhost", 9090);

    public static Stream<AuthBundle> valid_status_auth_bundle() {
        return valid_auth_bundle(STATUS);
    }

    public static Stream<AuthBundle> valid_but_not_status_auth_bundle() {
        return Arrays.stream(DigestSaltEnum.values())
                .filter(t -> t != STATUS)
                .flatMap(AuthBundleManager::valid_auth_bundle);
    }

    /**
     * Returns a GetIdFromStatusRequest builder with acceptable default values.
     */
    static GetIdFromStatusRequest.Builder givenValidStatusRequest(AuthBundle auth) {
        return GetIdFromStatusRequest.newBuilder()
                .setEbid(auth.getEbid())
                .setEpochId(auth.getEpochId())
                .setTime(auth.getTime().asNtpTimestamp())
                .setMac(auth.getMac())
                .setFromEpochId(clock().now().asEpochId())
                .setNumberOfDaysForEpochBundles(5)
                .setServerCountryCode(FRANCE.asByteString());
    }

    @ParameterizedTest
    @MethodSource("valid_status_auth_bundle")
    void can_produce_tuples_bundle(final AuthBundle auth) {

        givenIdentityExistsForIdA(auth.getIdA());

        final var request = givenValidStatusRequest(auth)
                .build();

        final var response = robertCryptoClient.getIdFromStatus(request)
                .orElseThrow();

        final var tuplesCipherForNewlyCreatedIdentity = getCipherForTuples(response.getIdA());

        assertThat(response).has(noGrpcError());
        assertThatTuplesBundle(response.getTuples())
                .isEncryptedWith(tuplesCipherForNewlyCreatedIdentity)
                .have(countryCode(FRANCE))
                .have(idA(response.getIdA().toByteArray()))
                .have(ebidConstistentWithTupleEpoch())
                .is(
                        aBundleWithEpochs(
                                clock().atEpoch(request.getFromEpochId()),
                                clock().now().plus(5, DAYS).truncatedTo(DAYS)
                        )
                );
    }

    @ParameterizedTest
    @MethodSource("valid_but_not_status_auth_bundle")
    void cant_produce_tuples_bundle_for_non_status_request(final AuthBundle invalidAuth) {

        givenIdentityExistsForIdA(invalidAuth.getIdA());

        final var request = givenValidStatusRequest(invalidAuth)
                .build();

        final var response = robertCryptoClient.getIdFromStatus(request)
                .orElseThrow();

        assertThat(response).has(grpcErrorResponse(400, "Invalid MAC"));
    }

    @ParameterizedTest
    @MethodSource("valid_status_auth_bundle")
    void cant_produce_tuples_bundle_for_malformed_ebid(final AuthBundle auth) {
        givenIdentityExistsForIdA(auth.getIdA());
        final var malformedEbid = new byte[8];
        new SecureRandom().nextBytes(malformedEbid);

        final var request = givenValidStatusRequest(auth)
                .setEbid(ByteString.copyFrom(malformedEbid))
                .build();

        final var response = robertCryptoClient.getIdFromStatus(request)
                .orElseThrow();

        assertThat(response)
                .is(grpcErrorResponse(400, "Could not decrypt ebid content"));
    }

    @ParameterizedTest
    @MethodSource("valid_status_auth_bundle")
    void cant_produce_tuples_bundle_when_the_epoch_is_different_from_ebid(final AuthBundle auth) {
        givenIdentityExistsForIdA(auth.getIdA());
        final var inconsistentEpoch = auth.getEpochId() - 1;

        final var request = givenValidStatusRequest(auth)
                .setEpochId(inconsistentEpoch)
                .build();

        final var response = robertCryptoClient.getIdFromStatus(request)
                .orElseThrow();

        assertThat(response)
                .is(grpcErrorResponse(400, "Could not decrypt ebid content"));
    }

    @ParameterizedTest
    @MethodSource("valid_status_auth_bundle")
    void cant_produce_tuples_bundle_when_the_ebid_belongs_to_an_epoch_at_a_date_the_server_is_missing_the_serverkey(
            final AuthBundle auth) {
        final var oneMonthOldAuth = auth.toBuilder()
                .title("30 days in the past: " + auth.getTitle())
                .timeAndEpoch(clock().now().minus(30, DAYS))
                .build();
        givenIdentityExistsForIdA(oneMonthOldAuth.getIdA());

        final var request = givenValidStatusRequest(oneMonthOldAuth)
                .build();

        final var response = robertCryptoClient.getIdFromStatus(request)
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
    @MethodSource("valid_status_auth_bundle")
    void cant_produce_tuples_bundle_for_unknown_idA(final AuthBundle auth) {
        // create the identity to be able to compute mac with regular tools
        givenIdentityExistsForIdA(auth.getIdA());

        final var request = givenValidStatusRequest(auth)
                .build();

        // delete the identity to simulate an unknown idA
        givenIdentityDoesntExistForIdA(auth.getIdA());

        final var response = robertCryptoClient.getIdFromStatus(request)
                .orElseThrow();

        assertThat(response)
                .is(grpcErrorResponse(404, "Could not find id"));
    }

    @ParameterizedTest
    @MethodSource("valid_status_auth_bundle")
    void cant_produce_tuples_bundle_for_incorrect_mac(final AuthBundle auth) {
        givenIdentityExistsForIdA(auth.getIdA());

        final var request = givenValidStatusRequest(auth)
                .setMac(ByteString.copyFromUtf8("Incorrect MAC value"))
                .build();

        final var response = robertCryptoClient.getIdFromStatus(request)
                .orElseThrow();

        assertThat(response)
                .is(grpcErrorResponse(400, "Invalid MAC"));

    }

    @ParameterizedTest
    @MethodSource("valid_status_auth_bundle")
    void cant_produce_tuples_bundle_for_an_ebid_larger_than_64bits(final AuthBundle auth) {
        givenIdentityExistsForIdA(auth.getIdA());

        final var largerEbid = Arrays.copyOf(auth.getEbid().toByteArray(), 9);

        final var request = givenValidStatusRequest(auth)
                .setEbid(ByteString.copyFrom(largerEbid))
                .build();

        final var response = robertCryptoClient.getIdFromStatus(request)
                .orElseThrow();

        assertThat(response)
                .is(grpcErrorResponse(500, "Error validating authenticated request"));
    }
}
