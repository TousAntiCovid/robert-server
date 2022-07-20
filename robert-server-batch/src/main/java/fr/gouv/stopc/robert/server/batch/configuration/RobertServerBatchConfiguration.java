package fr.gouv.stopc.robert.server.batch.configuration;

import fr.gouv.stopc.robert.crypto.grpc.server.messaging.CryptoGrpcServiceImplGrpc;
import io.grpc.ManagedChannelBuilder;
import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RobertServerBatchConfiguration {

    @Bean
    public TimedAspect timedAspect(final MeterRegistry registry) {
        return new TimedAspect(registry);
    }

    @Bean
    public CryptoGrpcServiceImplGrpc.CryptoGrpcServiceImplBlockingStub cryptoGrpcClient(
            final PropertyLoader propertyLoader) {
        final var channel = ManagedChannelBuilder
                .forAddress(
                        propertyLoader.getCryptoServerHost(), Integer.parseInt(propertyLoader.getCryptoServerPort())
                )
                .usePlaintext().build();
        return CryptoGrpcServiceImplGrpc.newBlockingStub(channel);
    }

}
