package fr.gouv.stopc.robert.crypto.grpc.server.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import fr.gouv.stopc.robert.crypto.grpc.server.storage.utils.KeystoreTypeEnum;
import lombok.Getter;


@Getter
@Component
public class PropertyLoader {

    @Value("${robert.crypto.server.port}")
    private String cryptoServerPort;

    @Value("${robert.crypto.server.keystore.password}")
    private String keyStorePassword;

    @Value("${robert.crypto.server.keystore.config.file}")
    private String keyStoreConfigFile;

    @Value("${robert.server.time-start}")
    private String timeStart;

    @Value("${robert.protocol.hello-message-timestamp-tolerance}")
    private Integer helloMessageTimeStampTolerance;

    @Value("${robert.crypto.server.keystore.type:PKCS11}")
    private KeystoreTypeEnum keystoreType;

    @Value("${robert.crypto.server.keystore.file:}")
    private Resource keystoreFile;
}
