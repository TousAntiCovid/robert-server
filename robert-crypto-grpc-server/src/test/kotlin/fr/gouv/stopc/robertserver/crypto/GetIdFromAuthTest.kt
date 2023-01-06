package fr.gouv.stopc.robertserver.crypto

import com.google.protobuf.ByteString
import fr.gouv.stopc.robert.crypto.grpc.server.messaging.GetIdFromAuthRequest
import fr.gouv.stopc.robert.crypto.grpc.server.messaging.GetIdFromAuthRequest.Builder
import fr.gouv.stopc.robertserver.common.base64Encode
import fr.gouv.stopc.robertserver.crypto.test.AuthBundle
import fr.gouv.stopc.robertserver.crypto.test.IntegrationTest
import fr.gouv.stopc.robertserver.crypto.test.clock
import fr.gouv.stopc.robertserver.crypto.test.givenIdentityDoesntExistForIdA
import fr.gouv.stopc.robertserver.crypto.test.givenIdentityExistsForIdA
import fr.gouv.stopc.robertserver.crypto.test.matchers.grpcBinaryField
import fr.gouv.stopc.robertserver.crypto.test.matchers.grpcErrorResponse
import fr.gouv.stopc.robertserver.crypto.test.matchers.grpcField
import fr.gouv.stopc.robertserver.crypto.test.matchers.noGrpcError
import fr.gouv.stopc.robertserver.crypto.test.whenRobertCryptoClient
import fr.gouv.stopc.robertserver.test.assertThatErrorLogs
import fr.gouv.stopc.robertserver.test.assertThatInfoLogs
import fr.gouv.stopc.robertserver.test.assertj.containsPattern
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.security.SecureRandom
import java.time.format.DateTimeFormatter.BASIC_ISO_DATE
import java.time.temporal.ChronoUnit.DAYS
import java.util.Arrays

@IntegrationTest
class GetIdFromAuthTest {

    /**
     * Returns a GetIdFromAuthRequest builder with acceptable default values.
     */
    fun givenAuthRequest(auth: AuthBundle): Builder {
        return GetIdFromAuthRequest.newBuilder()
            .setEbid(auth.ebid)
            .setEpochId(auth.epochId)
            .setTime(auth.time.asNtpTimestamp())
            .setMac(auth.mac)
            .setRequestType(auth.requestType.salt.toInt())
    }

    @ParameterizedTest
    @MethodSource("fr.gouv.stopc.robertserver.crypto.test.AuthBundleKt#valid_auth_bundle")
    fun can_authenticate_valid_request(auth: AuthBundle) {
        givenIdentityExistsForIdA(auth.idA)
        val request = givenAuthRequest(auth)
            .build()
        val response = whenRobertCryptoClient().getIdFromAuth(request)
        assertThat(response)
            .has(noGrpcError())
            .has(grpcBinaryField("idA", auth.idA))
            .has(grpcField("epochId", auth.epochId))
    }

    @ParameterizedTest
    @MethodSource("fr.gouv.stopc.robertserver.crypto.test.AuthBundleKt#valid_auth_bundle")
    fun cant_authenticate_with_unknown_request_type(auth: AuthBundle) {
        givenIdentityExistsForIdA(auth.idA)
        val request = givenAuthRequest(auth)
            .setRequestType(0)
            .build()
        val response = whenRobertCryptoClient().getIdFromAuth(request)
        assertThat(response)
            .has(grpcErrorResponse(400, "Unknown request type: 0"))
        assertThatInfoLogs()
            .contains("Status 400: Unknown request type: 0")
    }

    @ParameterizedTest
    @MethodSource("fr.gouv.stopc.robertserver.crypto.test.AuthBundleKt#valid_auth_bundle")
    fun cant_authenticate_with_malformed_ebid(auth: AuthBundle) {
        givenIdentityExistsForIdA(auth.idA)
        val malformedEbid = ByteArray(8)
        SecureRandom().nextBytes(malformedEbid)
        val request = givenAuthRequest(auth)
            .setEbid(ByteString.copyFrom(malformedEbid))
            .build()
        val response = whenRobertCryptoClient().getIdFromAuth(request)
        assertThat(response)
            .has(grpcErrorResponse(400, "Could not decrypt EBID content"))
        assertThatInfoLogs()
            .containsPattern("Status 400: Could not decrypt EBID content: .*")
    }

    @ParameterizedTest
    @MethodSource("fr.gouv.stopc.robertserver.crypto.test.AuthBundleKt#valid_auth_bundle")
    fun cant_authenticate_with_an_epoch_different_from_ebid(auth: AuthBundle) {
        givenIdentityExistsForIdA(auth.idA)
        val inconsistentEpoch = auth.epochId - 1
        val request = givenAuthRequest(auth)
            .setEpochId(inconsistentEpoch)
            .build()
        val response = whenRobertCryptoClient().getIdFromAuth(request)
        assertThat(response)
            .has(grpcErrorResponse(400, "Could not decrypt EBID content"))
        assertThatInfoLogs()
            .contains("Status 400: Could not decrypt EBID content: Request epoch $inconsistentEpoch and EBID epoch ${auth.epochId} don't match")
    }

    @ParameterizedTest
    @MethodSource("fr.gouv.stopc.robertserver.crypto.test.AuthBundleKt#valid_auth_bundle")
    fun cant_authenticate_when_the_ebid_belongs_to_an_epoch_at_a_date_the_server_is_missing_the_serverkey(
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
        val request = givenAuthRequest(oneMonthOldAuth)
            .build()
        val response = whenRobertCryptoClient().getIdFromAuth(request)
        assertThat(response)
            .has(
                grpcErrorResponse(430, "Missing server key")
            )
        val oneMonthOldTime = clock.atEpoch(auth.epochId).minus(30, DAYS)
        assertThatInfoLogs()
            .contains("Status 430: Missing server key: No server key for ${oneMonthOldTime.toUtcLocalDate()}")
        val date = oneMonthOldTime.toUtcLocalDate().format(BASIC_ISO_DATE)
        assertThatErrorLogs()
            .contains("Keystore does not contain key for alias 'server-key-$date'")
    }

    @ParameterizedTest
    @MethodSource("fr.gouv.stopc.robertserver.crypto.test.AuthBundleKt#valid_auth_bundle")
    fun cant_authenticate_with_unknown_idA(auth: AuthBundle) {
        // create the identity to be able to compute mac with regular tools
        givenIdentityExistsForIdA(auth.idA)
        val request = givenAuthRequest(auth)
            .build()

        // delete the identity to simulate an unknown idA
        givenIdentityDoesntExistForIdA(auth.idA)
        val response = whenRobertCryptoClient().getIdFromAuth(request)
        assertThat(response)
            .has(grpcErrorResponse(404, "Could not find idA"))
        val base64Ebid = request.ebid.toByteArray().base64Encode()
        assertThatInfoLogs()
            .contains("Status 404: Could not find idA: IdA contained in EBID($base64Ebid) was not found in database")
    }

    @ParameterizedTest
    @MethodSource("fr.gouv.stopc.robertserver.crypto.test.AuthBundleKt#valid_auth_bundle")
    fun cant_authenticate_with_incorrect_mac(auth: AuthBundle) {
        givenIdentityExistsForIdA(auth.idA)
        val request = givenAuthRequest(auth)
            .setMac(ByteString.copyFromUtf8("Incorrect MAC value"))
            .build()
        val response = whenRobertCryptoClient().getIdFromAuth(request)
        assertThat(response)
            .has(grpcErrorResponse(400, "Invalid MAC"))
        assertThatInfoLogs()
            .contains("Status 400: Invalid MAC")
    }

    @ParameterizedTest
    @MethodSource("fr.gouv.stopc.robertserver.crypto.test.AuthBundleKt#valid_auth_bundle")
    fun cant_authenticate_with_an_ebid_larger_than_64bits(auth: AuthBundle) {
        givenIdentityExistsForIdA(auth.idA)
        val largerEbid = Arrays.copyOf(auth.ebid.toByteArray(), 9)
        val request = givenAuthRequest(auth)
            .setEbid(ByteString.copyFrom(largerEbid))
            .build()
        val response = whenRobertCryptoClient().getIdFromAuth(request)
        assertThat(response)
            .has(grpcErrorResponse(400, "Invalid EBID"))
        assertThatInfoLogs()
            .contains("Status 400: Invalid EBID")
    }
}
