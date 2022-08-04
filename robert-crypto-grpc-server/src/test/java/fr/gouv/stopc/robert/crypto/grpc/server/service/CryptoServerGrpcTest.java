package fr.gouv.stopc.robert.crypto.grpc.server.service;

import com.google.protobuf.ByteString;
import fr.gouv.stopc.robert.crypto.grpc.server.client.service.ICryptoServerGrpcClient;
import fr.gouv.stopc.robert.crypto.grpc.server.client.service.impl.CryptoServerGrpcClient;
import fr.gouv.stopc.robert.crypto.grpc.server.messaging.CreateRegistrationRequest;
import fr.gouv.stopc.robert.crypto.grpc.server.messaging.CreateRegistrationResponse;
import fr.gouv.stopc.robert.crypto.grpc.server.test.IntegrationTest;
import fr.gouv.stopc.robert.crypto.grpc.server.utils.CryptoTestUtils;
import fr.gouv.stopc.robert.server.common.service.RobertClock;
import fr.gouv.stopc.robert.server.common.utils.TimeUtils;
import fr.gouv.stopc.robert.server.crypto.service.CryptoService;
import fr.gouv.stopc.robert.server.crypto.service.impl.CryptoServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigInteger;
import java.util.Optional;

import static fr.gouv.stopc.robert.crypto.grpc.server.test.DataManager.NUMBER_OF_DAYS_FOR_BUNDLES;
import static fr.gouv.stopc.robert.crypto.grpc.server.test.DataManager.SERVER_COUNTRY_CODE;
import static fr.gouv.stopc.robert.crypto.grpc.server.test.DataManager.clientStorageService;
import static fr.gouv.stopc.robert.crypto.grpc.server.test.DataManager.currentEpochId;
import static fr.gouv.stopc.robert.crypto.grpc.server.test.EncryptedMatcher.isEncrypted;
import static fr.gouv.stopc.robert.crypto.grpc.server.test.JsonNodeMatcher.isJson;
import static fr.gouv.stopc.robert.crypto.grpc.server.test.matchers.EphemeralTupleMatcher.isValidTuple;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;

@IntegrationTest
@Slf4j
class CryptoServerGrpcTest {

    private ICryptoServerGrpcClient cryptoServerClient;

    private CryptoService cryptoService;

    @Autowired
    private RobertClock clock;

    @BeforeEach
    public void setup() {

        cryptoServerClient = new CryptoServerGrpcClient("localhost", 9090);

        cryptoService = new CryptoServiceImpl();

    }

    @Test
    void createRegistration_succeed() {

        final var request = CreateRegistrationRequest
                .newBuilder()
                .setClientPublicKey(ByteString.copyFrom(CryptoTestUtils.generateECDHPublicKey()))
                .setFromEpochId(currentEpochId)
                .setNumberOfDaysForEpochBundles(NUMBER_OF_DAYS_FOR_BUNDLES)
                .setServerCountryCode(ByteString.copyFrom(BigInteger.valueOf(SERVER_COUNTRY_CODE).toByteArray()))
                .build();

        Optional<CreateRegistrationResponse> optionalCreateRegistrationResponse = cryptoServerClient
                .createRegistration(request);

        assertThat(optionalCreateRegistrationResponse.get().hasError()).isFalse();

        CreateRegistrationResponse createRegistrationResponse = optionalCreateRegistrationResponse.get();

        final var idAKey = clientStorageService.findKeyById(createRegistrationResponse.getIdA().toByteArray()).get()
                .getKeyForTuples();
        final var expectedSize = (NUMBER_OF_DAYS_FOR_BUNDLES - 1) * 96
                + TimeUtils.remainingEpochsForToday(currentEpochId);

        assertThat(
                createRegistrationResponse.getTuples().toByteArray(), isEncrypted(
                        isJson(
                                hasSize(expectedSize)
                        ),
                        idAKey
                )
        );

        // TODO : faire les vérifs par jour
        // prendre des lots de 96 tuples correspondant à une journée ainsi que la clé
        // associé

        assertThat(
                createRegistrationResponse.getTuples().toByteArray(), isEncrypted(
                        isJson(
                                everyItem(isValidTuple(cryptoService, clock))
                        ),
                        idAKey
                )
        );
    }

    // @Test
    // void createRegistration_failed_with_fake_clientPublicKey() {
    // byte[] fakeKey = new byte[32];
    // new SecureRandom().nextBytes(fakeKey);
    //
    // CreateRegistrationRequest request = CreateRegistrationRequest
    // .newBuilder()
    // .setClientPublicKey(ByteString.copyFrom(fakeKey))
    // .setFromEpochId(currentEpochId)
    // .setNumberOfDaysForEpochBundles(NUMBER_OF_DAYS_FOR_BUNDLES)
    // .setServerCountryCode(ByteString.copyFrom(BigInteger.valueOf(SERVER_COUNTRY_CODE).toByteArray()))
    // .build();
    //
    // final var createRegistrationResponse =
    // cryptoServerClient.createRegistration(request);
    //
    // assertThat(createRegistrationResponse.hasError()).isTrue();
    // assertEquals(400, createRegistrationResponse.getError().getCode());
    // }
    //
    // @Test
    // void createRegistration_failed_with_clientPublicKey_not_ECDH() {
    //
    // CreateRegistrationRequest request = CreateRegistrationRequest
    // .newBuilder()
    // .setClientPublicKey(ByteString.copyFrom(CryptoTestUtils.generateDHPublicKey()))
    // .setFromEpochId(currentEpochId)
    // .setNumberOfDaysForEpochBundles(NUMBER_OF_DAYS_FOR_BUNDLES)
    // .setServerCountryCode(ByteString.copyFrom(BigInteger.valueOf(SERVER_COUNTRY_CODE).toByteArray()))
    // .build();
    //
    // final var createRegistrationResponse =
    // cryptoServerClient.createRegistration(request);
    //
    // assertThat(createRegistrationResponse.hasError()).isTrue();
    // assertEquals(400, createRegistrationResponse.getError().getCode());
    //
    // }
    //
    // @Test
    // void createRegistration_failed_with_incorrect_EC_clientPublicKey() {
    // // Client public key generated with EC curve "secp256k1" instead of server's
    // // choice of "secp256*r*1"
    // CreateRegistrationRequest request = CreateRegistrationRequest
    // .newBuilder()
    // .setClientPublicKey(ByteString.copyFrom(CryptoTestUtils.generateECDHPublicKey("secp256k1")))
    // .setFromEpochId(currentEpochId)
    // .setNumberOfDaysForEpochBundles(NUMBER_OF_DAYS_FOR_BUNDLES)
    // .setServerCountryCode(ByteString.copyFrom(BigInteger.valueOf(SERVER_COUNTRY_CODE).toByteArray()))
    // .build();
    //
    // final var createRegistrationResponse =
    // cryptoServerClient.createRegistration(request);
    //
    // assertThat(createRegistrationResponse.hasError()).isTrue();
    // assertEquals(400, createRegistrationResponse.getError().getCode());
    //
    // }

}
