package fr.gouv.clea.clea.scoring.configuration.domain.risk;

import fr.gouv.clea.clea.scoring.configuration.ScoringConfiguration;
import fr.gouv.clea.clea.scoring.configuration.ScoringConfigurationItem;
import lombok.Setter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotEmpty;
import java.util.List;

@Validated
@Configuration
@ConfigurationProperties(prefix="clea.conf.risk")
@ConditionalOnProperty(value = "clea.conf.risk.enabled", havingValue = "true")
public class RiskConfiguration extends ScoringConfiguration {

    @Setter
    @NotEmpty
    protected List<RiskRule> rules;

    public List<? extends ScoringConfigurationItem> getScorings() {
        return rules;
    }

    public RiskRule getConfigurationFor(int venueType, int venueCategory1, int venueCategory2) {
        return (RiskRule) super.getConfigurationFor(venueType, venueCategory1, venueCategory2);
    }
}
