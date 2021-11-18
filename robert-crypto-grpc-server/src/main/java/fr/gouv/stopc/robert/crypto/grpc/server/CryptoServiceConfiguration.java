package fr.gouv.stopc.robert.crypto.grpc.server;

import fr.gouv.stopc.robert.crypto.grpc.server.storage.cryptographic.service.ICryptographicStorageService;
import fr.gouv.stopc.robert.crypto.grpc.server.utils.PropertyLoader;
import org.springframework.context.annotation.Configuration;

import javax.inject.Inject;

import java.io.IOException;

@Configuration
public class CryptoServiceConfiguration {

    @Inject
    public CryptoServiceConfiguration(CryptoServiceGrpcServer server,
            PropertyLoader propertyLoader,
            ICryptographicStorageService cryptoStorageService) throws IOException {

        // Init the cryptographic Storage
        cryptoStorageService.init(
                propertyLoader.getKeyStorePassword(),
                propertyLoader.getKeyStoreConfigFile(),
                propertyLoader.getKeystoreType(),
                propertyLoader.getKeystoreFile()
        );

        server.initPort(Integer.parseInt(propertyLoader.getCryptoServerPort()));
        server.start();

    }

}
