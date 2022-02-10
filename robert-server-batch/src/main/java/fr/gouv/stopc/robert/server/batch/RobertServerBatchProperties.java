package fr.gouv.stopc.robert.server.batch;

import lombok.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

import java.time.Duration;

@Value
@ConstructorBinding
@ConfigurationProperties("robert-batch")
public class RobertServerBatchProperties {

    RiskThreshold riskThreshold;

    @Value
    public static class RiskThreshold {

        Duration lastContactDelay;
    }
}
