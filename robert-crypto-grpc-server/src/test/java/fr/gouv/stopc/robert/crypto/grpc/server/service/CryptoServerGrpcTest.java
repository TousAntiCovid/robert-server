package fr.gouv.stopc.robert.crypto.grpc.server.service;

import com.google.protobuf.ByteString;
import fr.gouv.stopc.robert.crypto.grpc.server.client.service.ICryptoServerGrpcClient;
import fr.gouv.stopc.robert.crypto.grpc.server.client.service.impl.CryptoServerGrpcClient;
import fr.gouv.stopc.robert.crypto.grpc.server.messaging.CreateRegistrationRequest;
import fr.gouv.stopc.robert.crypto.grpc.server.test.IntegrationTest;
import fr.gouv.stopc.robert.server.common.service.RobertClock;
import lombok.extern.slf4j.Slf4j;
import org.bson.internal.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigInteger;
import java.security.SecureRandom;

import static fr.gouv.stopc.robert.crypto.grpc.server.test.CryptoManager.findClientById;
import static fr.gouv.stopc.robert.crypto.grpc.server.test.CryptoManager.generateDHKeyPair;
import static fr.gouv.stopc.robert.crypto.grpc.server.test.CryptoManager.generateECDHKeyPair;
import static fr.gouv.stopc.robert.crypto.grpc.server.test.DataManager.FRENCH_COUNTRY_CODE;
import static fr.gouv.stopc.robert.crypto.grpc.server.test.DataManager.NOW;
import static fr.gouv.stopc.robert.crypto.grpc.server.test.DataManager.NUMBER_OF_DAYS_FOR_BUNDLES;
import static fr.gouv.stopc.robert.crypto.grpc.server.test.EncryptedMatcher.isEncrypted;
import static fr.gouv.stopc.robert.crypto.grpc.server.test.JsonNodeMatcher.isJson;
import static fr.gouv.stopc.robert.crypto.grpc.server.test.matchers.EphemeralTupleMatcher.isValidTuple;
import static java.time.temporal.ChronoUnit.DAYS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertThrows;

@IntegrationTest
@Slf4j
class CryptoServerGrpcTest {

    private ICryptoServerGrpcClient cryptoServerClient;

    @Autowired
    private RobertClock clock;

    @BeforeEach
    public void setup() {

        cryptoServerClient = new CryptoServerGrpcClient("localhost", 9090);

    }

    @Test
    void createRegistration_succeed() {

        final var request = CreateRegistrationRequest
                .newBuilder()
                .setClientPublicKey(ByteString.copyFrom(generateECDHKeyPair("secp256r1").getPublic().getEncoded()))
                .setFromEpochId(NOW.asEpochId())
                .setNumberOfDaysForEpochBundles(NUMBER_OF_DAYS_FOR_BUNDLES)
                .setServerCountryCode(ByteString.copyFrom(BigInteger.valueOf(FRENCH_COUNTRY_CODE).toByteArray()))
                .build();

        final var createRegistrationResponse = cryptoServerClient
                .createRegistration(request).orElseThrow();

        assertThat(createRegistrationResponse.hasError()).isFalse();

        final var keyForTuples = findClientById(Base64.encode(createRegistrationResponse.getIdA().toByteArray()))
                .getKeyForTuples();

        final var expectedSize = NOW.plus(4, DAYS).truncatedTo(DAYS).asEpochId() - NOW.asEpochId();

        assertThat(
                createRegistrationResponse.getTuples().toByteArray(), isEncrypted(
                        isJson(
                                hasSize(expectedSize)
                        ),
                        keyForTuples
                )
        );

        // TODO : faire les vérifs par jour
        // prendre des lots de 96 tuples correspondant à une journée ainsi que la clé
        // associé

        assertThat(
                createRegistrationResponse.getTuples().toByteArray(), isEncrypted(
                        isJson(
                                everyItem(isValidTuple(clock))
                        ),
                        keyForTuples
                )
        );
    }

    @Test
    void createRegistration_succeed_with_number_of_days_higher_than_number_of_serverkeys_generated() {
        final var request = CreateRegistrationRequest
                .newBuilder()
                .setClientPublicKey(ByteString.copyFrom(generateECDHKeyPair("secp256r1").getPublic().getEncoded()))
                .setFromEpochId(NOW.asEpochId())
                .setNumberOfDaysForEpochBundles(20)
                .setServerCountryCode(ByteString.copyFrom(BigInteger.valueOf(FRENCH_COUNTRY_CODE).toByteArray()))
                .build();

        final var createRegistrationResponse = cryptoServerClient.createRegistration(request).orElseThrow();

        final var keyForTuples = findClientById(Base64.encode(createRegistrationResponse.getIdA().toByteArray()))
                .getKeyForTuples();

        final var expectedTwentyDaysSize = NOW.plus(20, DAYS).truncatedTo(DAYS).asEpochId() - NOW.asEpochId();
        final var expectedActualSize = NOW.plus(5, DAYS).truncatedTo(DAYS).asEpochId() - NOW.asEpochId();

        assertThrows(
                AssertionError.class,
                () -> assertThat(
                        createRegistrationResponse.getTuples().toByteArray(), isEncrypted(
                                isJson(
                                        hasSize(expectedTwentyDaysSize)
                                ),
                                keyForTuples
                        )
                )
        );

        assertThat(
                createRegistrationResponse.getTuples().toByteArray(), isEncrypted(
                        isJson(
                                hasSize(expectedActualSize)
                        ),
                        keyForTuples
                )
        );

        assertThat(
                createRegistrationResponse.getTuples().toByteArray(), isEncrypted(
                        isJson(
                                everyItem(isValidTuple(clock))
                        ),
                        keyForTuples
                )
        );
    }

    @Test
    void createRegistration_failed_with_fake_clientPublicKey() {
        byte[] fakeKey = new byte[32];
        new SecureRandom().nextBytes(fakeKey);

        CreateRegistrationRequest request = CreateRegistrationRequest
                .newBuilder()
                .setClientPublicKey(ByteString.copyFrom(fakeKey))
                .setFromEpochId(NOW.asEpochId())
                .setNumberOfDaysForEpochBundles(NUMBER_OF_DAYS_FOR_BUNDLES)
                .setServerCountryCode(ByteString.copyFrom(BigInteger.valueOf(FRENCH_COUNTRY_CODE).toByteArray()))
                .build();

        final var createRegistrationResponse = cryptoServerClient.createRegistration(request).orElseThrow();

        assertThat(createRegistrationResponse.hasError()).isTrue();
        assertThat(createRegistrationResponse.getError().getCode()).isEqualTo(400);
    }

    @Test
    void createRegistration_failed_with_clientPublicKey_not_ECDH() {

        CreateRegistrationRequest request = CreateRegistrationRequest
                .newBuilder()
                .setClientPublicKey(ByteString.copyFrom(generateDHKeyPair().getPublic().getEncoded()))
                .setFromEpochId(NOW.asEpochId())
                .setNumberOfDaysForEpochBundles(NUMBER_OF_DAYS_FOR_BUNDLES)
                .setServerCountryCode(ByteString.copyFrom(BigInteger.valueOf(FRENCH_COUNTRY_CODE).toByteArray()))
                .build();

        final var createRegistrationResponse = cryptoServerClient.createRegistration(request).orElseThrow();

        assertThat(createRegistrationResponse.hasError()).isTrue();
        assertThat(createRegistrationResponse.getError().getCode()).isEqualTo(400);

    }

    @Test
    void createRegistration_failed_with_incorrect_EC_clientPublicKey() {
        // Client public key generated with EC curve "secp256k1" instead of server's
        // choice of "secp256*r*1"
        CreateRegistrationRequest request = CreateRegistrationRequest
                .newBuilder()
                .setClientPublicKey(ByteString.copyFrom(generateECDHKeyPair("secp256k1").getPublic().getEncoded()))
                .setFromEpochId(NOW.asEpochId())
                .setNumberOfDaysForEpochBundles(NUMBER_OF_DAYS_FOR_BUNDLES)
                .setServerCountryCode(ByteString.copyFrom(BigInteger.valueOf(FRENCH_COUNTRY_CODE).toByteArray()))
                .build();

        final var createRegistrationResponse = cryptoServerClient.createRegistration(request).orElseThrow();

        assertThat(createRegistrationResponse.hasError()).isTrue();
        assertThat(createRegistrationResponse.getError().getCode()).isEqualTo(400);

    }

    @Test
    void createRegistration_failed_with_epochId_in_the_past() {

        final var request = CreateRegistrationRequest
                .newBuilder()
                .setClientPublicKey(ByteString.copyFrom(generateECDHKeyPair("secp256r1").getPublic().getEncoded()))
                .setFromEpochId(clock.now().minus(25, DAYS).asEpochId())
                .setNumberOfDaysForEpochBundles(NUMBER_OF_DAYS_FOR_BUNDLES)
                .setServerCountryCode(ByteString.copyFrom(BigInteger.valueOf(FRENCH_COUNTRY_CODE).toByteArray()))
                .build();

        final var createRegistrationResponse = cryptoServerClient.createRegistration(request).orElseThrow();

        assertThat(createRegistrationResponse.hasError()).isTrue();
        assertThat(createRegistrationResponse.getError().getCode()).isEqualTo(500);
    }

    @Test
    void createRegistration_failed_with_epochId_in_the_future() {
        final var request = CreateRegistrationRequest
                .newBuilder()
                .setClientPublicKey(ByteString.copyFrom(generateECDHKeyPair("secp256r1").getPublic().getEncoded()))
                .setFromEpochId(clock.now().plus(10, DAYS).asEpochId())
                .setNumberOfDaysForEpochBundles(NUMBER_OF_DAYS_FOR_BUNDLES)
                .setServerCountryCode(ByteString.copyFrom(BigInteger.valueOf(FRENCH_COUNTRY_CODE).toByteArray()))
                .build();

        final var createRegistrationResponse = cryptoServerClient.createRegistration(request).orElseThrow();

        assertThat(createRegistrationResponse.hasError()).isTrue();
        assertThat(createRegistrationResponse.getError().getCode()).isEqualTo(500);
    }

    @Test
    void createRegistration_failed_with_number_of_days_null() {
        final var request = CreateRegistrationRequest
                .newBuilder()
                .setClientPublicKey(ByteString.copyFrom(generateECDHKeyPair("secp256r1").getPublic().getEncoded()))
                .setFromEpochId(NOW.asEpochId())
                .setNumberOfDaysForEpochBundles(0)
                .setServerCountryCode(ByteString.copyFrom(BigInteger.valueOf(FRENCH_COUNTRY_CODE).toByteArray()))
                .build();

        final var createRegistrationResponse = cryptoServerClient.createRegistration(request).orElseThrow();

        assertThat(createRegistrationResponse.hasError()).isTrue();
        assertThat(createRegistrationResponse.getError().getCode()).isEqualTo(500);
    }

    @Test
    void createRegistration_failed_with_incorrect_ecc() {
        final var request = CreateRegistrationRequest
                .newBuilder()
                .setClientPublicKey(ByteString.copyFrom(generateECDHKeyPair("secp256r1").getPublic().getEncoded()))
                .setFromEpochId(NOW.asEpochId())
                .setNumberOfDaysForEpochBundles(NUMBER_OF_DAYS_FOR_BUNDLES)
                .setServerCountryCode(ByteString.copyFrom("incorrect ecc".getBytes()))
                .build();

        final var createRegistrationResponse = cryptoServerClient.createRegistration(request).orElseThrow();

        assertThat(createRegistrationResponse.hasError()).isFalse();

        final var keyForTuples = findClientById(Base64.encode(createRegistrationResponse.getIdA().toByteArray()))
                .getKeyForTuples();

        final var expectedSize = NOW.plus(4, DAYS).truncatedTo(DAYS).asEpochId() - NOW.asEpochId();

        assertThat(
                createRegistrationResponse.getTuples().toByteArray(), isEncrypted(
                        isJson(
                                hasSize(expectedSize)
                        ),
                        keyForTuples
                )
        );

        final var error = assertThrows(
                AssertionError.class,
                () -> assertThat(
                        createRegistrationResponse.getTuples().toByteArray(), isEncrypted(
                                isJson(
                                        everyItem(isValidTuple(clock))
                                ),
                                keyForTuples
                        )
                )
        );

        assertThat(
                error.getMessage().replace("\r", ""), equalTo(
                        "\n" +
                                "Expected: an encrypted value with a json with every item is a consistent ephemeral tuple\n"
                                +
                                "     but: was an encrypted value with a json which an item was an ephemeral tuple with country code <105> instead of country code <33>"
                )
        );

    }

}
