package fr.gouv.stopc.robertserver.crypto

import com.google.protobuf.ByteString
import fr.gouv.stopc.robert.crypto.grpc.server.messaging.DeleteIdRequest
import fr.gouv.stopc.robert.crypto.grpc.server.messaging.DeleteIdRequest.Builder
import fr.gouv.stopc.robert.server.common.DigestSaltEnum
import fr.gouv.stopc.robert.server.common.DigestSaltEnum.UNREGISTER
import fr.gouv.stopc.robertserver.crypto.test.AuthBundle
import fr.gouv.stopc.robertserver.crypto.test.IntegrationTest
import fr.gouv.stopc.robertserver.crypto.test.assertThatAllIdentities
import fr.gouv.stopc.robertserver.crypto.test.clock
import fr.gouv.stopc.robertserver.crypto.test.givenIdentityDoesntExistForIdA
import fr.gouv.stopc.robertserver.crypto.test.givenIdentityExistsForIdA
import fr.gouv.stopc.robertserver.crypto.test.matchers.grpcBinaryField
import fr.gouv.stopc.robertserver.crypto.test.matchers.grpcErrorResponse
import fr.gouv.stopc.robertserver.crypto.test.matchers.noGrpcError
import fr.gouv.stopc.robertserver.crypto.test.valid_auth_bundle
import fr.gouv.stopc.robertserver.crypto.test.whenRobertCryptoClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.security.SecureRandom
import java.time.temporal.ChronoUnit.DAYS
import java.util.Arrays

@IntegrationTest
class DeleteIdTest {

    companion object {

        @JvmStatic
        fun valid_unregister_auth_bundle() = valid_auth_bundle(UNREGISTER)

        @JvmStatic
        fun valid_but_not_unregister_auth_bundle() = DigestSaltEnum.values()
            .filter { it != UNREGISTER }
            .flatMap(::valid_auth_bundle)
    }

    /**
     * Returns a DeleteIdRequest builder with acceptable default values.
     */
    private fun givenUnregisterRequest(auth: AuthBundle): Builder {
        return DeleteIdRequest.newBuilder()
            .setEbid(auth.ebid)
            .setEpochId(auth.epochId)
            .setTime(auth.time.asNtpTimestamp())
            .setMac(auth.mac)
    }

    @ParameterizedTest
    @MethodSource("valid_unregister_auth_bundle")
    fun can_delete_identity(auth: AuthBundle) {
        givenIdentityExistsForIdA("AAAAAAA=")
        givenIdentityExistsForIdA(auth.idA)
        val request = givenUnregisterRequest(auth)
            .build()
        val response = whenRobertCryptoClient().deleteId(request)
        assertThat(response)
            .has(noGrpcError())
            .has(grpcBinaryField("idA", auth.idA))
        assertThatAllIdentities()
            .extracting("idA")
            .containsExactlyInAnyOrder("AAAAAAA=")
    }

    @ParameterizedTest
    @MethodSource("valid_but_not_unregister_auth_bundle")
    fun cant_unregister_for_non_unregister_request(invalidAuth: AuthBundle) {
        givenIdentityExistsForIdA(invalidAuth.idA)
        val request = givenUnregisterRequest(invalidAuth)
            .build()
        val response = whenRobertCryptoClient().deleteId(request)
        assertThat(response).has(grpcErrorResponse(400, "Invalid MAC"))
    }

    @ParameterizedTest
    @MethodSource("valid_unregister_auth_bundle")
    fun cant_unregister_with_malformed_ebid(auth: AuthBundle) {
        givenIdentityExistsForIdA(auth.idA)
        val malformedEbid = ByteArray(8)
        SecureRandom().nextBytes(malformedEbid)
        val request = givenUnregisterRequest(auth)
            .setEbid(ByteString.copyFrom(malformedEbid))
            .build()
        val response = whenRobertCryptoClient().deleteId(request)
        assertThat(response)
            .has(grpcErrorResponse(400, "Could not decrypt ebid content"))
    }

    @ParameterizedTest
    @MethodSource("valid_unregister_auth_bundle")
    fun cant_unregister_with_an_epoch_different_from_ebid(auth: AuthBundle) {
        givenIdentityExistsForIdA(auth.idA)
        val inconsistentEpoch = auth.epochId - 1
        val request = givenUnregisterRequest(auth)
            .setEpochId(inconsistentEpoch)
            .build()
        val response = whenRobertCryptoClient().deleteId(request)
        assertThat(response)
            .has(grpcErrorResponse(400, "Could not decrypt ebid content"))
    }

    @ParameterizedTest
    @MethodSource("valid_unregister_auth_bundle")
    fun cant_unregister_when_the_ebid_belongs_to_an_epoch_at_a_date_the_server_is_missing_the_serverkey(
        auth: AuthBundle
    ) {
        val oneMonthOldAuth = AuthBundle(
            "30 days in the past: " + auth.title,
            auth.requestType,
            auth.idA,
            auth.time.minus(30, DAYS),
            clock.atEpoch(auth.epochId).minus(30, DAYS).asEpochId()
        )
        givenIdentityExistsForIdA(oneMonthOldAuth.idA)
        val request = givenUnregisterRequest(oneMonthOldAuth)
            .build()
        val response = whenRobertCryptoClient().deleteId(request)
        assertThat(response)
            .has(
                grpcErrorResponse(
                    430,
                    "No server key found from cryptographic storage : " + oneMonthOldAuth.epochId
                )
            )
    }

    @ParameterizedTest
    @MethodSource("valid_unregister_auth_bundle")
    fun cant_unregister_unknown_idA(auth: AuthBundle) {
        // create the identity to be able to compute mac with regular tools
        givenIdentityExistsForIdA(auth.idA)
        val request = givenUnregisterRequest(auth)
            .build()

        // delete the identity to simulate an unknown idA
        givenIdentityDoesntExistForIdA(auth.idA)
        val response = whenRobertCryptoClient().deleteId(request)
        assertThat(response)
            .has(grpcErrorResponse(404, "Could not find id"))
    }

    @ParameterizedTest
    @MethodSource("valid_unregister_auth_bundle")
    fun cant_unregister_with_incorrect_mac(auth: AuthBundle) {
        givenIdentityExistsForIdA(auth.idA)
        val request = givenUnregisterRequest(auth)
            .setMac(ByteString.copyFromUtf8("Incorrect MAC value"))
            .build()
        val response = whenRobertCryptoClient().deleteId(request)
        assertThat(response)
            .has(grpcErrorResponse(400, "Invalid MAC"))
    }

    @ParameterizedTest
    @MethodSource("valid_unregister_auth_bundle")
    fun cant_unregister_with_an_ebid_larger_than_64bits(auth: AuthBundle) {
        givenIdentityExistsForIdA(auth.idA)
        val largerEbid = Arrays.copyOf(auth.ebid.toByteArray(), 9)
        val request = givenUnregisterRequest(auth)
            .setEbid(ByteString.copyFrom(largerEbid))
            .build()
        val response = whenRobertCryptoClient().deleteId(request)
        assertThat(response)
            .has(grpcErrorResponse(500, "Error validating authenticated request"))
    }
}
