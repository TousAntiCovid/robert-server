package fr.gouv.stopc.robertserver.crypto

import com.google.protobuf.ByteString
import fr.gouv.stopc.robert.crypto.grpc.server.messaging.CreateRegistrationRequest
import fr.gouv.stopc.robert.crypto.grpc.server.messaging.CreateRegistrationRequest.Builder
import fr.gouv.stopc.robertserver.crypto.test.CountryCode
import fr.gouv.stopc.robertserver.crypto.test.CountryCode.FRANCE
import fr.gouv.stopc.robertserver.crypto.test.IntegrationTest
import fr.gouv.stopc.robertserver.crypto.test.assertThatInfoLogs
import fr.gouv.stopc.robertserver.crypto.test.assertThatWarnLogs
import fr.gouv.stopc.robertserver.crypto.test.clock
import fr.gouv.stopc.robertserver.crypto.test.getCipherForTuples
import fr.gouv.stopc.robertserver.crypto.test.matchers.KeyGenerator.DH_1024
import fr.gouv.stopc.robertserver.crypto.test.matchers.KeyGenerator.ECDH_SECP256K1
import fr.gouv.stopc.robertserver.crypto.test.matchers.KeyGenerator.ECDH_SECP256R1
import fr.gouv.stopc.robertserver.crypto.test.matchers.aBundleWithEpochs
import fr.gouv.stopc.robertserver.crypto.test.matchers.assertThatTuplesBundle
import fr.gouv.stopc.robertserver.crypto.test.matchers.countryCode
import fr.gouv.stopc.robertserver.crypto.test.matchers.ebidConstistentWithTupleEpoch
import fr.gouv.stopc.robertserver.crypto.test.matchers.grpcErrorResponse
import fr.gouv.stopc.robertserver.crypto.test.matchers.idA
import fr.gouv.stopc.robertserver.crypto.test.matchers.noGrpcError
import fr.gouv.stopc.robertserver.crypto.test.whenRobertCryptoClient
import fr.gouv.stopc.robertserver.test.assertj.containsPattern
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import java.security.SecureRandom
import java.time.LocalDate
import java.time.temporal.ChronoUnit.DAYS

@IntegrationTest
class CreateRegistrationTest {
    /**
     * Returns a CreateRegistrationRequest builder with acceptable default values:
     * - a valid public key
     * - the current epoch
     * - a request for a 5 days bundle
     * - the french server country code
     */
    private fun givenValidCreateRegistrationRequest(): Builder {
        return CreateRegistrationRequest
            .newBuilder()
            .setClientPublicKey(ByteString.copyFrom(ECDH_SECP256R1.generateKeyPair().public.encoded))
            .setFromEpochId(clock.now().asEpochId())
            .setNumberOfDaysForEpochBundles(5)
            .setServerCountryCode(FRANCE.asByteString())
    }

    @ParameterizedTest
    @CsvSource("FRANCE", "GERMANY")
    fun can_create_a_registration_and_return_a_valid_5_days_tuples_bundle(countryCode: CountryCode) {
        val now = clock.now()
        val request = givenValidCreateRegistrationRequest()
            .setServerCountryCode(countryCode.asByteString())
            .build()
        val response = whenRobertCryptoClient().createRegistration(request)
        val tuplesCipherForNewlyCreatedIdentity = getCipherForTuples(response.idA)
        assertThat(response).has(noGrpcError())
        assertThatTuplesBundle(response.tuples)
            .isEncryptedWith(tuplesCipherForNewlyCreatedIdentity)
            .have(countryCode(countryCode))
            .have(idA(response.idA.toByteArray()))
            .have(ebidConstistentWithTupleEpoch())
            .`is`(aBundleWithEpochs(now, now.plus(5, DAYS).truncatedTo(DAYS)))
    }

    @Test
    fun can_create_a_registration_and_return_a_valid_2_days_tuples_bundle() {
        val request = givenValidCreateRegistrationRequest()
            .setNumberOfDaysForEpochBundles(2)
            .build()
        val response = whenRobertCryptoClient().createRegistration(request)
        val tuplesCipherForNewlyCreatedIdentity = getCipherForTuples(response.idA)
        assertThat(response).has(noGrpcError())
        assertThatTuplesBundle(response.tuples)
            .isEncryptedWith(tuplesCipherForNewlyCreatedIdentity)
            .have(countryCode(FRANCE))
            .have(idA(response.idA.toByteArray()))
            .have(ebidConstistentWithTupleEpoch())
            .`is`(aBundleWithEpochs(clock.now(), clock.now().plus(2, DAYS).truncatedTo(DAYS)))
    }

    @Test
    fun doesnt_generate_tuples_for_unknown_server_keys() {
        // given a request for a 9 days bundle
        // then the response contains a bundle for 5 days
        // because the server doesn't have keys for the following 4 days
        val request = givenValidCreateRegistrationRequest()
            .setNumberOfDaysForEpochBundles(9)
            .build()
        val response = whenRobertCryptoClient().createRegistration(request)
        val tuplesCipherForNewlyCreatedIdentity = getCipherForTuples(response.idA)
        assertThat(response).has(noGrpcError())
        assertThatTuplesBundle(response.tuples)
            .isEncryptedWith(tuplesCipherForNewlyCreatedIdentity)
            .have(countryCode(FRANCE))
            .have(idA(response.idA.toByteArray()))
            .have(ebidConstistentWithTupleEpoch())
            .describedAs("a bundle with 5 days of tuples")
            .`is`(aBundleWithEpochs(clock.now(), clock.now().plus(5, DAYS).truncatedTo(DAYS)))
        val today = LocalDate.now()
        val end = today.plusDays(8)
        val missingKey1 = today.plusDays(5)
        val missingKey2 = today.plusDays(6)
        val missingKey3 = today.plusDays(7)
        val missingKey4 = today.plusDays(8)
        assertThatWarnLogs()
            .containsPattern(
                "Tuples request from $today[0-9T:Z=E]+ until ${end}T23:45:00Z=\\d+E can't be honored: missing server-keys $missingKey1, $missingKey2, $missingKey3, $missingKey4"
            )
    }

    @Test
    fun cant_create_a_registration_for_a_malformed_public_key() {
        val malformedKey = ByteArray(32)
        SecureRandom().nextBytes(malformedKey)
        val request = givenValidCreateRegistrationRequest()
            .setClientPublicKey(ByteString.copyFrom(malformedKey))
            .build()
        val response = whenRobertCryptoClient().createRegistration(request)
        assertThat(response)
            .`is`(
                grpcErrorResponse(400, "Unable to load client public key")
            )
        assertThatInfoLogs()
            .containsPattern("Status 400: Unable to load client public key: .*")
    }

    @Test
    fun cant_create_a_registration_with_a_non_ecdh_public_key() {
        val request = givenValidCreateRegistrationRequest()
            .setClientPublicKey(ByteString.copyFrom(DH_1024.generateKeyPair().public.encoded))
            .build()
        val response = whenRobertCryptoClient().createRegistration(request)
        assertThat(response)
            .has(grpcErrorResponse(400, "Unable to load client public key"))
        assertThatInfoLogs()
            .contains("Status 400: Unable to load client public key: java.security.InvalidKeyException: EC domain parameters must be encoded in the algorithm identifier")
    }

    @Test
    fun cant_create_a_registration_with_a_public_key_having_the_wrong_ec_spec() {
        // Client public key generated with EC curve "secp256k1" instead of server's
        // choice of "secp256*r*1"
        val request = givenValidCreateRegistrationRequest()
            .setClientPublicKey(ByteString.copyFrom(ECDH_SECP256K1.generateKeyPair().public.encoded))
            .build()
        val response = whenRobertCryptoClient().createRegistration(request)
        assertThat(response)
            .has(grpcErrorResponse(400, "Unable to derive keys from client public key"))
        assertThatInfoLogs()
            .contains("Status 400: Unable to derive keys from client public key: point is not on curve")
    }

    @ParameterizedTest
    @ValueSource(ints = [-25, -15, -10, -9, -8, -7, -6, 5, 6, 7, 8, 9, 10, 15, 25])
    fun cant_create_a_registration_producing_a_bundle_with_zero_tuples(bundleStartDayDrift: Int) {
        val request = givenValidCreateRegistrationRequest()
            .setNumberOfDaysForEpochBundles(1)
            .setFromEpochId(clock.now().plus(bundleStartDayDrift.toLong(), DAYS).asEpochId())
            .build()
        val response = whenRobertCryptoClient().createRegistration(request)
        assertThat(response)
            .`is`(grpcErrorResponse(500, "Internal error"))
        assertThatWarnLogs()
            .contains("Status 500: Internal error: 0 ephemeral tuples were generated")
    }

    @Test
    fun cant_create_a_registration_with_an_empty_bundle() {
        val request = givenValidCreateRegistrationRequest()
            .setNumberOfDaysForEpochBundles(0)
            .build()
        val response = whenRobertCryptoClient().createRegistration(request)
        assertThat(response)
            .`is`(grpcErrorResponse(500, "Internal error"))
        assertThatWarnLogs().contains("Status 500: Internal error: 0 ephemeral tuples were generated")
    }
}
