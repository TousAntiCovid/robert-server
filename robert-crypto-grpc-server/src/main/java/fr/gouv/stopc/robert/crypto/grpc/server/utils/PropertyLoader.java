package fr.gouv.stopc.robert.crypto.grpc.server.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import fr.gouv.stopc.robert.crypto.grpc.server.storage.utils.KeystoreTypeEnum;
import lombok.Getter;


@Getter
@Component
public class PropertyLoader {

    @Value("${robert.crypto.server.port:9090}")
    private String cryptoServerPort;

    @Value("${robert.crypto.server.keystore.password:1234}")
    private String keyStorePassword;

    @Value("${robert.crypto.server.keystore.config.file:/config/SoftHSMv2/softhsm2.cfg}")
    private String keyStoreConfigFile;

    @Value("${robert.crypto.server.keystore.type:PKCS11}")
    private KeystoreTypeEnum keystoreType;

    @Value("${robert.crypto.server.keystore.file:}")
    private Resource keystoreFile;

    @Value("${robert.server.time-start:20200601}")
    private String timeStart;

    @Value("${robert.protocol.hello-message-timestamp-tolerance:180}")
    private Integer helloMessageTimeStampTolerance;

}
