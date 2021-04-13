package fr.gouv.clea.clea.scoring.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@EnableConfigurationProperties(value = ExposureTimeConfiguration.class)
@ContextConfiguration(classes = { ExposureTimeConfigurationConverter.class })
@TestPropertySource("classpath:application.properties")
public class ExposureTimeConfigurationTest {
    @Autowired
    private ExposureTimeConfiguration configuration;
    
    @Test
    void testExposureTimeConfigurationHasExpectedSize() {
        assertThat(configuration.getScorings()).hasSize(4);
    }
    
    @Test
    void testExposureTimeConfigurationHasExpectedData() {
        ExposureTimeConfigurationItem scoring = (ExposureTimeConfigurationItem) configuration.getScorings().get(2);
        
        assertThat(scoring.getVenueType()).isEqualTo(3);
        assertThat(scoring.getVenueCategory1()).isEqualTo(ScoringConfigurationItem.wildcardValue);
        assertThat(scoring.getVenueCategory2()).isEqualTo(ScoringConfigurationItem.wildcardValue);
        assertThat(scoring.getExposureTimeBackward()).isEqualTo(1);
        assertThat(scoring.getExposureTimeForward()).isEqualTo(11);
    }
    
    @Test
    void testGetMostSpecificConfiguration() {
        ExposureTimeConfigurationItem scoring = configuration.getConfigurationFor(1, 1, 1);
        
        assertThat(scoring.getExposureTimeBackward()).isEqualTo(3);
        assertThat(scoring.getExposureTimeForward()).isEqualTo(13);
    }

}
