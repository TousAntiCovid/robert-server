package fr.gouv.clea.ws.configuration;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import javax.annotation.PostConstruct;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

@Data
@NoArgsConstructor
@Validated
@RefreshScope
@Configuration
@ConfigurationProperties(prefix = "clea.conf")
@Slf4j
public class CleaWsProperties {

    @Min(value = 1800)
    private long exposureTimeUnitInSeconds;

    @Positive
    private long duplicateScanThresholdInSeconds;

    @Min(value = 10)
    @Max(value = 30)
    private int retentionDurationInDays;

    @NotNull
    private boolean authorizationCheckActive;

    @NotNull
    @NotEmpty
    @NotBlank
    @ToString.Exclude
    private String robertJwtPublicKey;

    @PostConstruct
    public void logConfigurationLoaded() {
        log.info("Loaded Clea Ws configuration: {}", this);
    }
}
