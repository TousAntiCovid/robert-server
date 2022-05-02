package fr.gouv.stopc.robert.crypto.grpc.server.service;

import com.google.protobuf.ByteString;
import fr.gouv.stopc.robert.crypto.grpc.server.messaging.CreateRegistrationRequest;
import fr.gouv.stopc.robert.crypto.grpc.server.messaging.CryptoGrpcServiceImplGrpc;
import fr.gouv.stopc.robert.crypto.grpc.server.messaging.CryptoGrpcServiceImplGrpc.CryptoGrpcServiceImplBlockingStub;
import fr.gouv.stopc.robert.crypto.grpc.server.test.IntegrationTest;
import fr.gouv.stopc.robert.crypto.grpc.server.utils.CryptoTestUtils;
import fr.gouv.stopc.robert.server.common.utils.TimeUtils;
import io.grpc.ManagedChannelBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.security.SecureRandom;

import static com.spotify.hamcrest.jackson.IsJsonArray.jsonArray;
import static fr.gouv.stopc.robert.crypto.grpc.server.test.DataManager.NUMBER_OF_DAYS_FOR_BUNDLES;
import static fr.gouv.stopc.robert.crypto.grpc.server.test.DataManager.SERVER_COUNTRY_CODE;
import static fr.gouv.stopc.robert.crypto.grpc.server.test.DataManager.clientStorageService;
import static fr.gouv.stopc.robert.crypto.grpc.server.test.DataManager.currentEpochId;
import static fr.gouv.stopc.robert.crypto.grpc.server.test.matchers.EncryptedMatcher.isEncrypted;
import static fr.gouv.stopc.robert.crypto.grpc.server.test.matchers.JsonNodeMatcher.isJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;

@IntegrationTest
public class CryptoServerGrpcTest {

    private CryptoGrpcServiceImplBlockingStub cryptoServerClientStub;

    @BeforeEach
    public void before() {
        final var channel = ManagedChannelBuilder.forAddress(
                "localhost",
                9090
        ).usePlaintext().build();
        cryptoServerClientStub = CryptoGrpcServiceImplGrpc.newBlockingStub(channel);

    }

    @Test
    void createRegistration_succeed() {

        CreateRegistrationRequest request = CreateRegistrationRequest
                .newBuilder()
                .setClientPublicKey(ByteString.copyFrom(CryptoTestUtils.generateECDHPublicKey()))
                .setFromEpochId(currentEpochId)
                .setNumberOfDaysForEpochBundles(NUMBER_OF_DAYS_FOR_BUNDLES)
                .setServerCountryCode(ByteString.copyFrom(BigInteger.valueOf(SERVER_COUNTRY_CODE).toByteArray()))
                .build();

        final var createRegistrationResponse = cryptoServerClientStub.createRegistration(request);

        assertThat(createRegistrationResponse.hasError()).isFalse();

        final var idAKey = clientStorageService.findKeyById(createRegistrationResponse.getIdA().toByteArray()).get()
                .getKeyForTuples();
        final var expectedSize = (NUMBER_OF_DAYS_FOR_BUNDLES - 1) * 96
                + TimeUtils.remainingEpochsForToday(currentEpochId);

        assertThat(
                createRegistrationResponse.getTuples().toByteArray(), isEncrypted(
                        isJson(
                                jsonArray(hasSize(expectedSize))
                        ),
                        idAKey
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
                .setFromEpochId(currentEpochId)
                .setNumberOfDaysForEpochBundles(NUMBER_OF_DAYS_FOR_BUNDLES)
                .setServerCountryCode(ByteString.copyFrom(BigInteger.valueOf(SERVER_COUNTRY_CODE).toByteArray()))
                .build();

        final var createRegistrationResponse = cryptoServerClientStub.createRegistration(request);

        assertThat(createRegistrationResponse.hasError()).isTrue();
        assertEquals(400, createRegistrationResponse.getError().getCode());
    }

    @Test
    void createRegistration_failed_with_clientPublicKey_not_ECDH() {

        CreateRegistrationRequest request = CreateRegistrationRequest
                .newBuilder()
                .setClientPublicKey(ByteString.copyFrom(CryptoTestUtils.generateDHPublicKey()))
                .setFromEpochId(currentEpochId)
                .setNumberOfDaysForEpochBundles(NUMBER_OF_DAYS_FOR_BUNDLES)
                .setServerCountryCode(ByteString.copyFrom(BigInteger.valueOf(SERVER_COUNTRY_CODE).toByteArray()))
                .build();

        final var createRegistrationResponse = cryptoServerClientStub.createRegistration(request);

        assertThat(createRegistrationResponse.hasError()).isTrue();
        assertEquals(400, createRegistrationResponse.getError().getCode());

    }

    @Test
    void createRegistration_failed_with_incorrect_EC_clientPublicKey() {
        // Client public key generated with EC curve "secp256k1" instead of server's
        // choice of "secp256*r*1"
        CreateRegistrationRequest request = CreateRegistrationRequest
                .newBuilder()
                .setClientPublicKey(ByteString.copyFrom(CryptoTestUtils.generateECDHPublicKey("secp256k1")))
                .setFromEpochId(currentEpochId)
                .setNumberOfDaysForEpochBundles(NUMBER_OF_DAYS_FOR_BUNDLES)
                .setServerCountryCode(ByteString.copyFrom(BigInteger.valueOf(SERVER_COUNTRY_CODE).toByteArray()))
                .build();

        final var createRegistrationResponse = cryptoServerClientStub.createRegistration(request);

        assertThat(createRegistrationResponse.hasError()).isTrue();
        assertEquals(400, createRegistrationResponse.getError().getCode());

    }

}
