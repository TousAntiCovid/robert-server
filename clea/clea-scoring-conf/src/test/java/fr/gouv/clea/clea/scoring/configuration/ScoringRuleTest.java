package fr.gouv.clea.clea.scoring.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import fr.gouv.clea.clea.scoring.configuration.exposure.ExposureTimeConfiguration;
import fr.gouv.clea.clea.scoring.configuration.exposure.ExposureTimeConfigurationConverter;
import fr.gouv.clea.clea.scoring.configuration.risk.RiskConfiguration;
import fr.gouv.clea.clea.scoring.configuration.risk.RiskConfigurationConverter;

@SpringBootTest()
@ExtendWith(SpringExtension.class)
@EnableConfigurationProperties(value = {RiskConfiguration.class, ExposureTimeConfiguration.class})
@ContextConfiguration(classes = {RiskConfigurationConverter.class, ExposureTimeConfigurationConverter.class})
class ScoringRuleTest {

    @Autowired
    private RiskConfiguration riskConfiguration;

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
        assertThat(riskConfiguration.getConfigurationFor(3, 1, 3))
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
                .isEqualTo(riskConfiguration.getScorings().get(6));
    }

}
