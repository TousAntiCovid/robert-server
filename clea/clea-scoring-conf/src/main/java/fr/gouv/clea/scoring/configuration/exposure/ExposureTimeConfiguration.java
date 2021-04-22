package fr.gouv.clea.scoring.configuration.exposure;

import fr.gouv.clea.scoring.configuration.ScoringConfiguration;
import fr.gouv.clea.scoring.configuration.ScoringRule;
import fr.gouv.clea.scoring.configuration.validators.CheckDefaultRulePresence;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import javax.annotation.PostConstruct;
import javax.validation.constraints.NotEmpty;
import java.util.List;

@Validated
@Configuration
@ConfigurationProperties(prefix = "clea.conf.exposure")
@ConditionalOnProperty(value = "clea.conf.exposure.enabled", havingValue = "true")
@Slf4j
@ToString
public class ExposureTimeConfiguration extends ScoringConfiguration {

    @Setter
    @NotEmpty
    @CheckDefaultRulePresence
    protected List<ExposureTimeRule> rules;

    @PostConstruct
    private void logConfiguration() {
        log.info(this.toString());
    }

    public List<? extends ScoringRule> getScorings() {
        return rules;
    }

    public ExposureTimeRule getConfigurationFor(int venueType, int venueCategory1, int venueCategory2) {
        return (ExposureTimeRule) super.getConfigurationFor(venueType, venueCategory1, venueCategory2);
    }
}
