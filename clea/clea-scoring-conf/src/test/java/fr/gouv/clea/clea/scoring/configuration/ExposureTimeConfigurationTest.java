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
@EnableConfigurationProperties(value = TestConfiguration.class)
@ContextConfiguration(classes = { ExposureTimeConfigurationConverter.class })
@TestPropertySource("classpath:application.properties")
public class ExposureTimeConfigurationTest {
    @Autowired
    private TestConfiguration configuration;
    
    @Test
    void testExposureTimeConfigurationHasExpectedSize() {
        assertThat(configuration.getScorings()).hasSize(4);
    }
    
    @Test
    void testExposureTimeConfigurationHasExpectedData() {
        ExposureTimeConfiguration scoring = configuration.getScorings().get(2);
        
        assertThat(scoring.getVenueType()).isEqualTo(3);
        assertThat(scoring.getVenueCategory1()).isEqualTo(ScoringConfiguration.wildcardValue);
        assertThat(scoring.getVenueCategory2()).isEqualTo(ScoringConfiguration.wildcardValue);
        assertThat(scoring.getExposureTime()).isEqualTo(1);
    }
    
    @Test
    void testGetMostSpecificConfiguration() {
        ExposureTimeConfiguration scoring = configuration.getConfigurationFor(1, 1, 1);
        
        assertThat(scoring.getExposureTime()).isEqualTo(3);
    }

}
