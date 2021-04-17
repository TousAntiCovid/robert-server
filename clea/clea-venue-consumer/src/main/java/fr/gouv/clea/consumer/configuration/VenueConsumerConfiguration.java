package fr.gouv.clea.consumer.configuration;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Positive;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Validated
@Configuration
@ConfigurationProperties(prefix = "clea.conf")
public class VenueConsumerConfiguration {
    @Min(value = 600)
    private long durationUnitInSeconds;
    @Positive
    private int driftBetweenDeviceAndOfficialTimeInSecs;
    @Positive
    private int cleaClockDriftInSecs;
    @Min(value = 10)
    @Max(value = 30)
    private int retentionDurationInDays;
}
