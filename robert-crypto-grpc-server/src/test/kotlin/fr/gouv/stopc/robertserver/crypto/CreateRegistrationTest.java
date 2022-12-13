package fr.gouv.stopc.robertserver.crypto;

import com.google.protobuf.ByteString;
import fr.gouv.stopc.robert.crypto.grpc.server.client.service.ICryptoServerGrpcClient;
import fr.gouv.stopc.robert.crypto.grpc.server.client.service.impl.CryptoServerGrpcClient;
import fr.gouv.stopc.robert.crypto.grpc.server.messaging.CreateRegistrationRequest;
import fr.gouv.stopc.robertserver.crypto.test.CountryCode;
import fr.gouv.stopc.robertserver.crypto.test.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.security.SecureRandom;

import static fr.gouv.stopc.robertserver.crypto.test.ClockManagerKt.getClock;
import static fr.gouv.stopc.robertserver.crypto.test.CountryCode.FRANCE;
import static fr.gouv.stopc.robertserver.crypto.test.PostgresqlManagerKt.getCipherForTuples;
import static fr.gouv.stopc.robertserver.crypto.test.matchers.EncryptedTuplesBundleMatcherKt.assertThatTuplesBundle;
import static fr.gouv.stopc.robertserver.crypto.test.matchers.EncryptedTuplesBundleMatcherKt.ebidConstistentWithTupleEpoch;
import static fr.gouv.stopc.robertserver.crypto.test.matchers.EncryptedTuplesBundleMatcherKt.countryCode;
import static fr.gouv.stopc.robertserver.crypto.test.matchers.EncryptedTuplesBundleMatcherKt.aBundleWithEpochs;
import static fr.gouv.stopc.robertserver.crypto.test.matchers.EncryptedTuplesBundleMatcherKt.idA;
import static fr.gouv.stopc.robertserver.crypto.test.matchers.GrpcResponseMatcherKt.grpcErrorResponse;
import static fr.gouv.stopc.robertserver.crypto.test.matchers.GrpcResponseMatcherKt.noGrpcError;
import static fr.gouv.stopc.robertserver.crypto.test.matchers.KeyGenerator.DH_1024;
import static fr.gouv.stopc.robertserver.crypto.test.matchers.KeyGenerator.ECDH_SECP256K1;
import static fr.gouv.stopc.robertserver.crypto.test.matchers.KeyGenerator.ECDH_SECP256R1;
import static java.time.temporal.ChronoUnit.DAYS;
import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
class CreateRegistrationTest {

    private final ICryptoServerGrpcClient robertCryptoClient = new CryptoServerGrpcClient("localhost", 9090);

    /**
     * Returns a CreateRegistrationRequest builder with acceptable default values:
     * <ul>
     * <li>a valid public key</li>
     * <li>the current epoch</li>
     * <li>a request for a 5 days bundle</li>
     * <li>the french server country code</li>
     * </ul>
     */
    private static CreateRegistrationRequest.Builder givenValidCreateRegistrationRequest() {
        return CreateRegistrationRequest
                .newBuilder()
                .setClientPublicKey(ByteString.copyFrom(ECDH_SECP256R1.generateKeyPair().getPublic().getEncoded()))
                .setFromEpochId(getClock().now().asEpochId())
                .setNumberOfDaysForEpochBundles(5)
                .setServerCountryCode(FRANCE.asByteString());
    }

    @ParameterizedTest
    @CsvSource({
            "FRANCE",
            "GERMANY"
    })
    void can_create_a_registration_and_return_a_valid_5_days_tuples_bundle(final CountryCode countryCode) {
        final var now = getClock().now();

        final var request = givenValidCreateRegistrationRequest()
                .setServerCountryCode(countryCode.asByteString())
                .build();
        final var response = robertCryptoClient.createRegistration(request)
                .orElseThrow();

        final var tuplesCipherForNewlyCreatedIdentity = getCipherForTuples(response.getIdA());

        assertThat(response).has(noGrpcError());
        assertThatTuplesBundle(response.getTuples())
                .isEncryptedWith(tuplesCipherForNewlyCreatedIdentity)
                .have(countryCode(countryCode))
                .have(idA(response.getIdA().toByteArray()))
                .have(ebidConstistentWithTupleEpoch())
                .is(aBundleWithEpochs(now, now.plus(5, DAYS).truncatedTo(DAYS)));
    }

    @Test
    void can_create_a_registration_and_return_a_valid_2_days_tuples_bundle() {
        final var request = givenValidCreateRegistrationRequest()
                .setNumberOfDaysForEpochBundles(2)
                .build();
        final var response = robertCryptoClient.createRegistration(request)
                .orElseThrow();

        final var tuplesCipherForNewlyCreatedIdentity = getCipherForTuples(response.getIdA());

        assertThat(response).has(noGrpcError());
        assertThatTuplesBundle(response.getTuples())
                .isEncryptedWith(tuplesCipherForNewlyCreatedIdentity)
                .have(countryCode(FRANCE))
                .have(idA(response.getIdA().toByteArray()))
                .have(ebidConstistentWithTupleEpoch())
                .is(aBundleWithEpochs(getClock().now(), getClock().now().plus(2, DAYS).truncatedTo(DAYS)));
    }

    @Test
    void doesnt_generate_tuples_for_unknown_server_keys() {
        // given a request for a 20 days bundle
        // then the response contains a bundle for 5 days
        // because the server doesn't have keys for the following 15 days

        final var request = givenValidCreateRegistrationRequest()
                .setNumberOfDaysForEpochBundles(20)
                .build();
        final var response = robertCryptoClient.createRegistration(request)
                .orElseThrow();

        final var tuplesCipherForNewlyCreatedIdentity = getCipherForTuples(response.getIdA());

        assertThat(response).has(noGrpcError());
        assertThatTuplesBundle(response.getTuples())
                .isEncryptedWith(tuplesCipherForNewlyCreatedIdentity)
                .have(countryCode(FRANCE))
                .have(idA(response.getIdA().toByteArray()))
                .have(ebidConstistentWithTupleEpoch())
                .as("a bundle with 5 days of tuples")
                .is(aBundleWithEpochs(getClock().now(), getClock().now().plus(5, DAYS).truncatedTo(DAYS)));
    }

    @Test
    void cant_create_a_registration_for_a_malformed_public_key() {
        final var malformedKey = new byte[32];
        new SecureRandom().nextBytes(malformedKey);

        final var request = givenValidCreateRegistrationRequest()
                .setClientPublicKey(ByteString.copyFrom(malformedKey))
                .build();

        final var response = robertCryptoClient.createRegistration(request)
                .orElseThrow();

        assertThat(response)
                .is(
                        grpcErrorResponse(
                                400,
                                "Unable to derive keys from provided client public key for client registration"
                        )
                );
    }

    @Test
    void cant_create_a_registration_with_a_non_ecdh_public_key() {
        final var request = givenValidCreateRegistrationRequest()
                .setClientPublicKey(ByteString.copyFrom(DH_1024.generateKeyPair().getPublic().getEncoded()))
                .build();

        final var response = robertCryptoClient.createRegistration(request)
                .orElseThrow();

        assertThat(response)
                .is(
                        grpcErrorResponse(
                                400,
                                "Unable to derive keys from provided client public key for client registration"
                        )
                );
    }

    @Test
    void cant_create_a_registration_with_a_public_key_having_the_wrong_ec_spec() {
        // Client public key generated with EC curve "secp256k1" instead of server's
        // choice of "secp256*r*1"
        final var request = givenValidCreateRegistrationRequest()
                .setClientPublicKey(ByteString.copyFrom(ECDH_SECP256K1.generateKeyPair().getPublic().getEncoded()))
                .build();

        final var response = robertCryptoClient.createRegistration(request)
                .orElseThrow();

        assertThat(response)
                .is(
                        grpcErrorResponse(
                                400,
                                "Unable to derive keys from provided client public key for client registration"
                        )
                );

    }

    @ParameterizedTest
    @ValueSource(ints = { -25, -15, -10, -9, -8, -7, -6, 5, 6, 7, 8, 9, 10, 15, 25 })
    void cant_create_a_registration_producing_a_bundle_with_zero_tuples(int bundleStartDayDrift) {
        final var request = givenValidCreateRegistrationRequest()
                .setNumberOfDaysForEpochBundles(1)
                .setFromEpochId(getClock().now().plus(bundleStartDayDrift, DAYS).asEpochId())
                .build();

        final var response = robertCryptoClient.createRegistration(request)
                .orElseThrow();

        assertThat(response)
                .is(grpcErrorResponse(500, "Unhandled exception while creating registration"));
    }

    @Test
    void cant_create_a_registration_with_an_empty_bundle() {
        final var request = givenValidCreateRegistrationRequest()
                .setNumberOfDaysForEpochBundles(0)
                .build();

        final var response = robertCryptoClient.createRegistration(request)
                .orElseThrow();

        assertThat(response)
                .is(grpcErrorResponse(500, "Unhandled exception while creating registration"));
    }
}
