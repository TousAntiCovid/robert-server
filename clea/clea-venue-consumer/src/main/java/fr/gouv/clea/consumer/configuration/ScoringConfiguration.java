package fr.gouv.clea.consumer.configuration;

import fr.gouv.clea.clea.scoring.configuration.exposure.ExposureTimeConfiguration;
import fr.gouv.clea.clea.scoring.configuration.exposure.ExposureTimeConfigurationConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ScoringConfiguration {

    @Bean
    ExposureTimeConfiguration getExposureTimeConfiguration() {
        return new ExposureTimeConfiguration();
    }

    @Bean
    ExposureTimeConfigurationConverter getExposureTimeConfigurationConverter() {
        return new ExposureTimeConfigurationConverter();
    }
}
