package fr.gouv.stopc.robertserver.crypto

import com.google.protobuf.ByteString
import fr.gouv.stopc.robert.crypto.grpc.server.messaging.GetIdFromStatusRequest
import fr.gouv.stopc.robert.crypto.grpc.server.messaging.GetIdFromStatusRequest.Builder
import fr.gouv.stopc.robertserver.common.RobertRequestType
import fr.gouv.stopc.robertserver.common.RobertRequestType.STATUS
import fr.gouv.stopc.robertserver.common.base64Encode
import fr.gouv.stopc.robertserver.crypto.test.AuthBundle
import fr.gouv.stopc.robertserver.crypto.test.CountryCode.FRANCE
import fr.gouv.stopc.robertserver.crypto.test.IntegrationTest
import fr.gouv.stopc.robertserver.crypto.test.clock
import fr.gouv.stopc.robertserver.crypto.test.getCipherForTuples
import fr.gouv.stopc.robertserver.crypto.test.givenIdentityDoesntExistForIdA
import fr.gouv.stopc.robertserver.crypto.test.givenIdentityExistsForIdA
import fr.gouv.stopc.robertserver.crypto.test.matchers.aBundleWithEpochs
import fr.gouv.stopc.robertserver.crypto.test.matchers.assertThatTuplesBundle
import fr.gouv.stopc.robertserver.crypto.test.matchers.countryCode
import fr.gouv.stopc.robertserver.crypto.test.matchers.ebidConstistentWithTupleEpoch
import fr.gouv.stopc.robertserver.crypto.test.matchers.grpcErrorResponse
import fr.gouv.stopc.robertserver.crypto.test.matchers.idA
import fr.gouv.stopc.robertserver.crypto.test.matchers.noGrpcError
import fr.gouv.stopc.robertserver.crypto.test.valid_auth_bundle
import fr.gouv.stopc.robertserver.crypto.test.whenRobertCryptoClient
import fr.gouv.stopc.robertserver.test.assertThatErrorLogs
import fr.gouv.stopc.robertserver.test.assertThatInfoLogs
import fr.gouv.stopc.robertserver.test.assertj.containsPattern
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.security.SecureRandom
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit.DAYS
import java.util.Arrays

@IntegrationTest
class GetIdFromStatusTest {

    companion object {

        @JvmStatic
        fun valid_status_auth_bundle(): List<AuthBundle> = valid_auth_bundle(STATUS)

        @JvmStatic
        fun valid_but_not_status_auth_bundle() = RobertRequestType.values()
            .filter { it != STATUS }
            .flatMap(::valid_auth_bundle)
    }

    /**
     * Returns a GetIdFromStatusRequest builder with acceptable default values.
     */
    fun givenValidStatusRequest(auth: AuthBundle): Builder {
        return GetIdFromStatusRequest.newBuilder()
            .setEbid(auth.ebid)
            .setEpochId(auth.epochId)
            .setTime(auth.time.asNtpTimestamp())
            .setMac(auth.mac)
            .setFromEpochId(clock.now().asEpochId())
            .setNumberOfDaysForEpochBundles(5)
            .setServerCountryCode(FRANCE.asByteString())
    }

    @ParameterizedTest
    @MethodSource("valid_status_auth_bundle")
    fun can_produce_tuples_bundle(auth: AuthBundle) {
        givenIdentityExistsForIdA(auth.idA)
        val request = givenValidStatusRequest(auth)
            .build()
        val response = whenRobertCryptoClient().getIdFromStatus(request)
        val tuplesCipherForNewlyCreatedIdentity = getCipherForTuples(response.idA)
        assertThat(response).has(noGrpcError())
        assertThatTuplesBundle(response.tuples)
            .isEncryptedWith(tuplesCipherForNewlyCreatedIdentity)
            .have(countryCode(FRANCE))
            .have(idA(response.idA.toByteArray()))
            .have(ebidConstistentWithTupleEpoch())
            .`is`(
                aBundleWithEpochs(
                    clock.atEpoch(request.fromEpochId),
                    clock.now().plus(5, DAYS).truncatedTo(DAYS)
                )
            )
    }

    @ParameterizedTest
    @MethodSource("valid_but_not_status_auth_bundle")
    fun cant_produce_tuples_bundle_for_non_status_request(invalidAuth: AuthBundle) {
        givenIdentityExistsForIdA(invalidAuth.idA)
        val request = givenValidStatusRequest(invalidAuth)
            .build()
        val response = whenRobertCryptoClient().getIdFromStatus(request)
        assertThat(response).has(grpcErrorResponse(400, "Invalid MAC"))
        val base64Mac = invalidAuth.mac.toByteArray().base64Encode()
        assertThatInfoLogs()
            .contains("Status 400: Invalid MAC: $base64Mac don't match expected checksum")
    }

    @ParameterizedTest
    @MethodSource("valid_status_auth_bundle")
    fun cant_produce_tuples_bundle_for_malformed_ebid(auth: AuthBundle) {
        givenIdentityExistsForIdA(auth.idA)
        val malformedEbid = ByteArray(8)
        SecureRandom().nextBytes(malformedEbid)
        val request = givenValidStatusRequest(auth)
            .setEbid(ByteString.copyFrom(malformedEbid))
            .build()
        val response = whenRobertCryptoClient().getIdFromStatus(request)
        assertThat(response)
            .`is`(grpcErrorResponse(400, "Could not decrypt EBID content"))
        assertThatInfoLogs()
            .containsPattern("Status 400: Could not decrypt EBID content: .*")
    }

    @ParameterizedTest
    @MethodSource("valid_status_auth_bundle")
    fun cant_produce_tuples_bundle_when_the_epoch_is_different_from_ebid(auth: AuthBundle) {
        givenIdentityExistsForIdA(auth.idA)
        val inconsistentEpoch = auth.epochId - 1
        val request = givenValidStatusRequest(auth)
            .setEpochId(inconsistentEpoch)
            .build()
        val response = whenRobertCryptoClient().getIdFromStatus(request)
        assertThat(response)
            .`is`(grpcErrorResponse(400, "Could not decrypt EBID content"))
        assertThatInfoLogs()
            .containsPattern("Status 400: Could not decrypt EBID content: .*")
    }

    @ParameterizedTest
    @MethodSource("valid_status_auth_bundle")
    fun cant_produce_tuples_bundle_when_the_ebid_belongs_to_an_epoch_at_a_date_the_server_is_missing_the_serverkey(
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
        val request = givenValidStatusRequest(oneMonthOldAuth)
            .build()
        val response = whenRobertCryptoClient().getIdFromStatus(request)
        assertThat(response)
            .has(grpcErrorResponse(430, "Missing server key"))
        val oneMonthOldTime = clock.atEpoch(auth.epochId).minus(30, DAYS)
        assertThatInfoLogs()
            .contains("Status 430: Missing server key: No server key for ${oneMonthOldTime.toUtcLocalDate()}")
        val date = oneMonthOldTime.toUtcLocalDate().format(DateTimeFormatter.BASIC_ISO_DATE)
        assertThatErrorLogs()
            .contains("Keystore does not contain key for alias 'server-key-$date'")
    }

    @ParameterizedTest
    @MethodSource("valid_status_auth_bundle")
    fun cant_produce_tuples_bundle_for_unknown_idA(auth: AuthBundle) {
        // create the identity to be able to compute mac with regular tools
        givenIdentityExistsForIdA(auth.idA)
        val request = givenValidStatusRequest(auth)
            .build()

        // delete the identity to simulate an unknown idA
        givenIdentityDoesntExistForIdA(auth.idA)
        val response = whenRobertCryptoClient().getIdFromStatus(request)
        assertThat(response)
            .`is`(grpcErrorResponse(404, "Could not find idA"))
    }

    @ParameterizedTest
    @MethodSource("valid_status_auth_bundle")
    fun cant_produce_tuples_bundle_for_incorrect_mac(auth: AuthBundle) {
        givenIdentityExistsForIdA(auth.idA)
        val request = givenValidStatusRequest(auth)
            .setMac(ByteString.copyFromUtf8("Incorrect MAC value"))
            .build()
        val response = whenRobertCryptoClient().getIdFromStatus(request)
        assertThat(response)
            .`is`(grpcErrorResponse(400, "Invalid MAC"))
        assertThatInfoLogs()
            .contains("Status 400: Invalid MAC")
    }

    @ParameterizedTest
    @MethodSource("valid_status_auth_bundle")
    fun cant_produce_tuples_bundle_for_an_ebid_larger_than_64bits(auth: AuthBundle) {
        givenIdentityExistsForIdA(auth.idA)
        val largerEbid = Arrays.copyOf(auth.ebid.toByteArray(), 9)
        val request = givenValidStatusRequest(auth)
            .setEbid(ByteString.copyFrom(largerEbid))
            .build()
        val response = whenRobertCryptoClient().getIdFromStatus(request)
        assertThat(response)
            .`is`(grpcErrorResponse(400, "Invalid EBID"))
        assertThatInfoLogs()
            .contains("Status 400: Invalid EBID")
    }
}
