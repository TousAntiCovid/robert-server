package fr.gouv.clea.scoring.configuration.risk;

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
@ConfigurationProperties(prefix = "clea.conf.risk")
@ConditionalOnProperty(value = "clea.conf.risk.enabled", havingValue = "true")
@ToString
@Slf4j
public class RiskConfiguration extends ScoringConfiguration {

    @Setter
    @NotEmpty
    @CheckDefaultRulePresence
    protected List<RiskRule> rules;

    @PostConstruct
    private void logConfiguration() {
        log.info(this.toString());
    }

    public List<? extends ScoringRule> getScorings() {
        return rules;
    }

    public RiskRule getConfigurationFor(int venueType, int venueCategory1, int venueCategory2) {
        return (RiskRule) super.getConfigurationFor(venueType, venueCategory1, venueCategory2);
    }
}
