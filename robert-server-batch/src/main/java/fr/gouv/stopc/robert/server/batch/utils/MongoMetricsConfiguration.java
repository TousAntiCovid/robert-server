package fr.gouv.stopc.robert.server.batch.utils;

import com.mongodb.MongoClientOptions;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.mongodb.MongoMetricsCommandListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MongoMetricsConfiguration {

    @Bean
    public MongoClientOptions mongoClientOptions(final MeterRegistry meterRegistry) {
        return MongoClientOptions.builder().addCommandListener(new MongoMetricsCommandListener(meterRegistry)).build();
    }

}
