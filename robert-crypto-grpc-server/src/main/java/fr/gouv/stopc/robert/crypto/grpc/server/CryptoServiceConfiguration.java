package fr.gouv.stopc.robert.crypto.grpc.server;

import fr.gouv.stopc.robert.crypto.grpc.server.storage.cryptographic.service.ICryptographicStorageService;
import fr.gouv.stopc.robert.crypto.grpc.server.utils.PropertyLoader;
import fr.gouv.stopc.robertserver.common.RobertClock;
import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class CryptoServiceConfiguration {

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

    @Bean
    public RobertClock clock(@Value("${robert.server.time-start}") String serviceStartTime) {
        return new RobertClock(serviceStartTime);
    }

    @Bean
    public TimedAspect timedAspect(final MeterRegistry registry) {
        return new TimedAspect(registry);
    }
}
