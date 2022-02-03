package fr.gouv.stopc.robertserver.dataset.injector.utils;

import fr.gouv.stopc.robert.crypto.grpc.server.storage.utils.KeystoreTypeEnum;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Getter
@Component
public class PropertyLoader {

    @Value("${robert.crypto.server.keystore.password}")
    private String keyStorePassword;

    @Value("${robert.crypto.server.keystore.config.file}")
    private String keyStoreConfigFile;

    @Value("${robert.crypto.server.host}")
    private String cryptoServerHost;

    @Value("${robert.crypto.server.port}")
    private String cryptoServerPort;

    @Value("${robert.crypto.server.keystore.type:PKCS11}")
    private KeystoreTypeEnum keystoreType;

    @Value("${robert.crypto.server.keystore.file:}")
    private Resource keystoreFile;

}
