package fr.gouv.stopc.robert.server.batch.configuration;

import fr.gouv.stopc.robert.crypto.grpc.server.client.service.ICryptoServerGrpcClient;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RobertServerBatchConfiguration {

    public RobertServerBatchConfiguration(final PropertyLoader propertyLoader,
            final ICryptoServerGrpcClient cryptoServerClient) {

        cryptoServerClient
                .init(propertyLoader.getCryptoServerHost(), Integer.parseInt(propertyLoader.getCryptoServerPort()));
    }

}
