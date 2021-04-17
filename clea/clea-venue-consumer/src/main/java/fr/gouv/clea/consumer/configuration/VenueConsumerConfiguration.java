package fr.gouv.clea.consumer.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Configuration
@ConfigurationProperties(prefix = "clea.conf")
public class VenueConsumerConfiguration {
    private long durationUnitInSeconds;
    private int driftBetweenDeviceAndOfficialTimeInSecs;
    private int cleaClockDriftInSecs;
    private int retentionDurationInDays;
}
