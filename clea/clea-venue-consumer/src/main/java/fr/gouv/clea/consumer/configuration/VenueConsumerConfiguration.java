package fr.gouv.clea.consumer.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Data
@Configuration
@ConfigurationProperties(prefix = "clea.conf")
public class VenueConsumerConfiguration {
    private long durationUnitInSeconds;
    private long statSlotDurationInSeconds;
}
