package fr.gouv.clea.clea.scoring.configuration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

import fr.gouv.clea.scoring.configuration.risk.RiskConfiguration;
import fr.gouv.clea.scoring.configuration.risk.RiskConfigurationConverter;
import fr.gouv.clea.scoring.configuration.risk.RiskRule;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ScoringRuleSelectionTest {

    private RiskConfiguration riskConfiguration;
    
    final static List<String> rawRules = List.of( 
            "*,*,*,3,1,3.0,2.0", // 0
            "1,1,1,3,1,3.0,2.0", // 1
            "1,2,3,3,1,3.0,2.0", // 2
            "3,*,*,3,1,3.0,2.0", // 3
            "3,1,*,3,1,3.0,2.0", // 4
            "3,*,2,3,1,3.0,2.0", // 5
            "3,*,3,3,1,3.0,2.0", // 6
            "3,1,2,3,1,3.0,2.0"  // 7
        );
    final static RiskConfigurationConverter converter = new RiskConfigurationConverter();
    final static List<RiskRule> rules = rawRules.stream().map(converter::convert).collect(Collectors.toList());
    
    @BeforeEach
    void setUp() {
        riskConfiguration = new RiskConfiguration();
        riskConfiguration.setRules(rules);
    }

    @Test
    void should_return_the_full_wildcard_rule() {
        assertThat(riskConfiguration.getConfigurationFor(2, 1, 1))
                .isEqualTo(riskConfiguration.getScorings().get(0));
    }

    @Test
    void should_return_rule_one_one_one() {
        assertThat(riskConfiguration.getConfigurationFor(1, 1, 1))
                .isEqualTo(riskConfiguration.getScorings().get(1));
    }

    @Test
    void should_return_rule_one_two_three() {
        assertThat(riskConfiguration.getConfigurationFor(1, 2, 3))
                .isEqualTo(riskConfiguration.getScorings().get(2));
    }

    @Test
    void should_return_the_rule_three_wildcard_wildcard() {
        assertThat(riskConfiguration.getConfigurationFor(3, 2, 1))
                .isEqualTo(riskConfiguration.getScorings().get(3));
    }

    @Test
    void should_return_the_rule_three_one_wildcard() {
        assertThat(riskConfiguration.getConfigurationFor(3, 1, 5))
                .isEqualTo(riskConfiguration.getScorings().get(4));
    }

    @Test
    void should_return_the_rule_three_wildcard_two() {
        assertThat(riskConfiguration.getConfigurationFor(3, 2, 2))
                .isEqualTo(riskConfiguration.getScorings().get(5));
    }

    @Test
    void should_return_the_rule_three_one_two() {
        assertThat(riskConfiguration.getConfigurationFor(3, 1, 2))
                .isEqualTo(riskConfiguration.getScorings().get(7));
    }

    @Test
    void should_return_the_rule_with_specific_cat1_when_rule_with_cat1_wildcard_and_rule_with_cat2_wildcard_matching() {
        assertThat(riskConfiguration.getConfigurationFor(3, 1, 3))
                .isEqualTo(riskConfiguration.getScorings().get(4));
    }

}
