package fr.gouv.clea.consumer.configuration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import javax.annotation.PostConstruct;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Positive;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Validated
@RefreshScope
@Configuration
@ConfigurationProperties(prefix = "clea.conf")
@Slf4j
public class VenueConsumerConfiguration {
    @Min(value = 600)
    private long durationUnitInSeconds;
    @Min(value = 1800)
    private long statSlotDurationInSeconds;
    @Positive
    private int driftBetweenDeviceAndOfficialTimeInSecs;
    @Positive
    private int cleaClockDriftInSecs;
    @Min(value = 10)
    @Max(value = 30)
    private int retentionDurationInDays;

    @PostConstruct
    private void logConfiguration() {
        log.info(this.toString());
    }
}
