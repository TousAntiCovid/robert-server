package fr.gouv.stopc.robertserver.ws.config;

import fr.gouv.stopc.robert.crypto.grpc.server.client.service.ICryptoServerGrpcClient;
import fr.gouv.stopc.robertserver.ws.utils.PropertyLoader;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RobertServerWsConfiguration {

    public RobertServerWsConfiguration(final PropertyLoader propertyLoader,
            final ICryptoServerGrpcClient cryptoServerClient) {

        cryptoServerClient
                .init(propertyLoader.getCryptoServerHost(), Integer.parseInt(propertyLoader.getCryptoServerPort()));
    }

}
