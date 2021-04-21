package fr.gouv.clea.consumer.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import fr.gouv.clea.scoring.configuration.exposure.ExposureTimeConfiguration;
import fr.gouv.clea.scoring.configuration.exposure.ExposureTimeConfigurationConverter;

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
