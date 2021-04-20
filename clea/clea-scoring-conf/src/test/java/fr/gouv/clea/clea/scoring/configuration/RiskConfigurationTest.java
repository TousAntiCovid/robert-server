package fr.gouv.clea.clea.scoring.configuration;

import fr.gouv.clea.clea.scoring.configuration.risk.RiskConfiguration;
import fr.gouv.clea.clea.scoring.configuration.risk.RiskConfigurationConverter;
import fr.gouv.clea.clea.scoring.configuration.risk.RiskRule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest()
@ExtendWith(SpringExtension.class)
@EnableConfigurationProperties(value = RiskConfiguration.class)
@ContextConfiguration(classes = {RiskConfigurationConverter.class})
@ActiveProfiles("test")
public class RiskConfigurationTest {

    @Autowired
    private RiskConfiguration configuration;

    @Test
    void testRiskConfigurationHasExpectedSize() {
        assertThat(configuration.getScorings()).hasSize(6);
    }

    @Test
    void testExposureTimeConfigurationHasExpectedData() {
        RiskRule scoring = (RiskRule) configuration.getScorings().get(5);

        assertThat(scoring.getVenueType()).isEqualTo(3);
        assertThat(scoring.getVenueCategory1()).isEqualTo(ScoringConfigurationItem.wildcardValue);
        assertThat(scoring.getVenueCategory2()).isEqualTo(ScoringConfigurationItem.wildcardValue);
        assertThat(scoring.getClusterThresholdBackward()).isEqualTo(3);
        assertThat(scoring.getClusterThresholdForward()).isEqualTo(1);
        assertThat(scoring.getRiskLevelBackward()).isEqualTo(3.0f);
        assertThat(scoring.getRiskLevelForward()).isEqualTo(2.0f);
    }

    @Test
    void testGetMostSpecificConfiguration() {
        RiskRule scoring = configuration.getConfigurationFor(1, 1, 1);

        assertThat(scoring.getClusterThresholdBackward()).isEqualTo(3);
        assertThat(scoring.getClusterThresholdForward()).isEqualTo(1);
        assertThat(scoring.getRiskLevelBackward()).isEqualTo(3.0f);
        assertThat(scoring.getRiskLevelForward()).isEqualTo(2.0f);
    }

    @Test
    void testValidation() {
        // test empty configuration
    }
}
