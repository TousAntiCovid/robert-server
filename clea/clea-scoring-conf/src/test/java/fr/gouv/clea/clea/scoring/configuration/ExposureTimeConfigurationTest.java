package fr.gouv.clea.clea.scoring.configuration;

import fr.gouv.clea.scoring.configuration.ScoringRule;
import fr.gouv.clea.scoring.configuration.exposure.ExposureTimeConfiguration;
import fr.gouv.clea.scoring.configuration.exposure.ExposureTimeConfigurationConverter;
import fr.gouv.clea.scoring.configuration.exposure.ExposureTimeRule;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@EnableConfigurationProperties(value = ExposureTimeConfiguration.class)
@ContextConfiguration(classes = {ExposureTimeConfigurationConverter.class})
@ActiveProfiles("test")
class ExposureTimeConfigurationTest {

    @Autowired
    private ExposureTimeConfiguration configuration;

    @Test
    void testExposureTimeConfigurationHasExpectedSize() {
        assertThat(configuration.getScorings()).hasSize(4);
    }

    @Test
    void testExposureTimeConfigurationHasExpectedData() {
        ExposureTimeRule scoring = (ExposureTimeRule) configuration.getScorings().get(2);

        assertThat(scoring.getVenueType()).isEqualTo(3);
        assertThat(scoring.getVenueCategory1()).isEqualTo(ScoringRule.WILDCARD_VALUE);
        assertThat(scoring.getVenueCategory2()).isEqualTo(ScoringRule.WILDCARD_VALUE);
        assertThat(scoring.getExposureTimeBackward()).isEqualTo(1);
        assertThat(scoring.getExposureTimeForward()).isEqualTo(11);
        assertThat(scoring.getExposureTimeStaffBackward()).isEqualTo(21);
        assertThat(scoring.getExposureTimeStaffForward()).isEqualTo(31);
    }

    @Test
    void testGetMostSpecificConfiguration() {
        ExposureTimeRule scoring = configuration.getConfigurationFor(1, 1, 1);

        assertThat(scoring.getExposureTimeBackward()).isEqualTo(3);
        assertThat(scoring.getExposureTimeForward()).isEqualTo(13);
        assertThat(scoring.getExposureTimeStaffBackward()).isEqualTo(23);
        assertThat(scoring.getExposureTimeStaffForward()).isEqualTo(33);
    }

    @Test
    void testValidation() {
        // test empty configuration
    }

}
