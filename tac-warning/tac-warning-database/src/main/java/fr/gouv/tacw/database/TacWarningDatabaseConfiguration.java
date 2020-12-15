package fr.gouv.tacw.database;

import javax.validation.constraints.Min;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EnableConfigurationProperties
@ConfigurationProperties(prefix="tacw.database")
@Configuration
public class TacWarningDatabaseConfiguration {
    @Min(value = 1)
    private long visitTokenRetentionPeriodDays;
    
    /**
     * This property is there for coherence but cannot be used
     * inside an @Schedule annotation.
     * @Value has to be used to inject the property.
     */
    private String visitTokenDeletionJobCronExpression;
}
