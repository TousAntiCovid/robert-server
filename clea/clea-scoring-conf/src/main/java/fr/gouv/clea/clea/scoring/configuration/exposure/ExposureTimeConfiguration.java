package fr.gouv.clea.clea.scoring.configuration.exposure;

import fr.gouv.clea.clea.scoring.configuration.ScoringConfiguration;
import fr.gouv.clea.clea.scoring.configuration.ScoringRule;
import lombok.Setter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotEmpty;
import java.util.List;

@Validated
@Configuration
@ConfigurationProperties(prefix="clea.conf.exposure")
@ConditionalOnProperty(value = "clea.conf.exposure.enabled", havingValue = "true")
public class ExposureTimeConfiguration extends ScoringConfiguration {

    @Setter
    @NotEmpty
    protected List<ExposureTimeRule> rules;

    public List<? extends ScoringRule> getScorings() {
        return rules;
    }

    public ExposureTimeRule getConfigurationFor(int venueType, int venueCategory1, int venueCategory2) {
        return (ExposureTimeRule) super.getConfigurationFor(venueType, venueCategory1, venueCategory2);
    }
}
