package fr.gouv.stopc.robert.crypto.grpc.server.test;

import fr.gouv.stopc.robert.crypto.grpc.server.service.impl.CryptoServerConfigurationServiceImpl;
import fr.gouv.stopc.robert.crypto.grpc.server.storage.cryptographic.service.ICryptographicStorageService;
import fr.gouv.stopc.robert.crypto.grpc.server.storage.database.repository.ClientIdentifierRepository;
import fr.gouv.stopc.robert.crypto.grpc.server.storage.service.IClientKeyStorageService;
import fr.gouv.stopc.robert.crypto.grpc.server.storage.service.impl.ClientKeyStorageServiceImpl;
import fr.gouv.stopc.robert.server.common.service.RobertClock;
import fr.gouv.stopc.robert.server.crypto.service.CryptoService;
import fr.gouv.stopc.robert.server.crypto.service.impl.CryptoServiceImpl;
import fr.gouv.stopc.robert.server.crypto.structure.impl.CryptoAESECB;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;

public class DataManager implements TestExecutionListener {

    public static final int SERVER_COUNTRY_CODE = 33;

    public static final int NUMBER_OF_DAYS_FOR_BUNDLES = 4;

    @Autowired
    private ICryptographicStorageService cryptographicStorageService;

    public static IClientKeyStorageService clientStorageService;

    public static CryptoService cryptoService;

    @Autowired
    private ClientIdentifierRepository clientIdentifierRepository;

    @Autowired
    private CryptoServerConfigurationServiceImpl serverConfigurationService;

    @Autowired
    private RobertClock clock;

    public static byte[][] serverKeys;

    public static int currentEpochId;

    public static CryptoAESECB federationKey;

    @Override
    public void beforeTestMethod(final TestContext testContext) {
        testContext.getApplicationContext()
                .getAutowireCapableBeanFactory()
                .autowireBean(this);

        cryptoService = new CryptoServiceImpl();

        clientStorageService = new ClientKeyStorageServiceImpl(cryptographicStorageService, clientIdentifierRepository);

        currentEpochId = clock.now().asEpochId();

        serverKeys = cryptographicStorageService.getServerKeys(
                currentEpochId, serverConfigurationService.getServiceTimeStart(), NUMBER_OF_DAYS_FOR_BUNDLES
        );

        federationKey = new CryptoAESECB(cryptographicStorageService.getFederationKey());
    }
}
