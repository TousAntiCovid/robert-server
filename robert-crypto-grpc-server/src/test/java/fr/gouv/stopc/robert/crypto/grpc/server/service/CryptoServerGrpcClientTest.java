package fr.gouv.stopc.robert.crypto.grpc.server.service;

import com.google.protobuf.ByteString;
import fr.gouv.stopc.robert.crypto.grpc.server.client.service.ICryptoServerGrpcClient;
import fr.gouv.stopc.robert.crypto.grpc.server.client.service.impl.CryptoServerGrpcClient;
import fr.gouv.stopc.robert.crypto.grpc.server.messaging.CreateRegistrationRequest;
import fr.gouv.stopc.robert.crypto.grpc.server.messaging.CreateRegistrationResponse;
import fr.gouv.stopc.robert.crypto.grpc.server.service.impl.CryptoServerConfigurationServiceImpl;
import fr.gouv.stopc.robert.crypto.grpc.server.storage.cryptographic.service.ICryptographicStorageService;
import fr.gouv.stopc.robert.crypto.grpc.server.storage.database.repository.ClientIdentifierRepository;
import fr.gouv.stopc.robert.crypto.grpc.server.storage.service.IClientKeyStorageService;
import fr.gouv.stopc.robert.crypto.grpc.server.storage.service.impl.ClientKeyStorageServiceImpl;
import fr.gouv.stopc.robert.crypto.grpc.server.test.IntegrationTest;
import fr.gouv.stopc.robert.crypto.grpc.server.test.TuplesMatchers;
import fr.gouv.stopc.robert.crypto.grpc.server.utils.CryptoTestUtils;
import fr.gouv.stopc.robert.server.common.utils.ByteUtils;
import fr.gouv.stopc.robert.server.common.utils.TimeUtils;
import fr.gouv.stopc.robert.server.crypto.service.CryptoService;
import fr.gouv.stopc.robert.server.crypto.service.impl.CryptoServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.security.SecureRandom;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@IntegrationTest
public class CryptoServerGrpcClientTest {

    private final byte[] SERVER_COUNTRY_CODE = new byte[] { (byte) 0x21 };

    private final int NUMBER_OF_DAYS_FOR_BUNDLES = 4;

    private int currentEpochId;

    private ICryptoServerGrpcClient cryptoServerClient;

    @Autowired
    private ICryptographicStorageService cryptographicStorageService;

    private IClientKeyStorageService clientStorageService;

    private CryptoService cryptoService;

    @Autowired
    private ClientIdentifierRepository clientIdentifierRepository;

    @Autowired
    private CryptoServerConfigurationServiceImpl serverConfigurationService;

    private byte[][] serverKeys;

    @BeforeEach
    public void before() {

        cryptoServerClient = new CryptoServerGrpcClient("localhost", 9090);

        cryptoService = new CryptoServiceImpl();

        clientStorageService = new ClientKeyStorageServiceImpl(cryptographicStorageService, clientIdentifierRepository);

        currentEpochId = TimeUtils.getCurrentEpochFrom(this.serverConfigurationService.getServiceTimeStart());

        serverKeys = cryptographicStorageService.getServerKeys(
                currentEpochId, this.serverConfigurationService.getServiceTimeStart(), NUMBER_OF_DAYS_FOR_BUNDLES
        );

    }

    @Test
    void createRegistration_succeed() {

        CreateRegistrationRequest request = CreateRegistrationRequest
                .newBuilder()
                .setClientPublicKey(ByteString.copyFrom(CryptoTestUtils.generateECDHPublicKey()))
                .setFromEpochId(currentEpochId)
                .setNumberOfDaysForEpochBundles(NUMBER_OF_DAYS_FOR_BUNDLES)
                .setServerCountryCode(ByteString.copyFrom(SERVER_COUNTRY_CODE))
                .build();

        Optional<CreateRegistrationResponse> optionalCreateRegistrationResponse = cryptoServerClient
                .createRegistration(request);

        assertThat(optionalCreateRegistrationResponse).isPresent();
        assertThat(optionalCreateRegistrationResponse.get().hasError()).isFalse();

        CreateRegistrationResponse createRegistrationResponse = optionalCreateRegistrationResponse.get();

        assertTrue(ByteUtils.isNotEmpty(createRegistrationResponse.getIdA().toByteArray()));
        byte[] tuples = createRegistrationResponse.getTuples().toByteArray();
        assertTrue(ByteUtils.isNotEmpty(tuples));

        assertTrue(
                TuplesMatchers.checkTuples(
                        createRegistrationResponse.getIdA().toByteArray(), tuples, this.currentEpochId, serverKeys,
                        clientStorageService, cryptoService
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
                .setServerCountryCode(ByteString.copyFrom(SERVER_COUNTRY_CODE))
                .build();

        Optional<CreateRegistrationResponse> optionalCreateRegistrationResponse = cryptoServerClient
                .createRegistration(request);

        assertThat(optionalCreateRegistrationResponse).isPresent();
        assertThat(optionalCreateRegistrationResponse.get().hasError()).isTrue();
        assertEquals(400, optionalCreateRegistrationResponse.get().getError().getCode());
    }

    @Test
    void createRegistration_failed_with_clientPublicKey_not_ECDH() {

        CreateRegistrationRequest request = CreateRegistrationRequest
                .newBuilder()
                .setClientPublicKey(ByteString.copyFrom(CryptoTestUtils.generateDHPublicKey()))
                .setFromEpochId(currentEpochId)
                .setNumberOfDaysForEpochBundles(NUMBER_OF_DAYS_FOR_BUNDLES)
                .setServerCountryCode(ByteString.copyFrom(SERVER_COUNTRY_CODE))
                .build();

        Optional<CreateRegistrationResponse> optionalCreateRegistrationResponse = cryptoServerClient
                .createRegistration(request);

        assertThat(optionalCreateRegistrationResponse).isPresent();
        assertThat(optionalCreateRegistrationResponse.get().hasError()).isTrue();
        assertEquals(400, optionalCreateRegistrationResponse.get().getError().getCode());

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
                .setServerCountryCode(ByteString.copyFrom(SERVER_COUNTRY_CODE))
                .build();

        Optional<CreateRegistrationResponse> optionalCreateRegistrationResponse = cryptoServerClient
                .createRegistration(request);

        assertThat(optionalCreateRegistrationResponse).isPresent();
        assertThat(optionalCreateRegistrationResponse.get().hasError()).isTrue();
        assertEquals(400, optionalCreateRegistrationResponse.get().getError().getCode());

    }

}
