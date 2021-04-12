package fr.gouv.clea.clea.scoring.configuration;

import java.util.Comparator;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix="clea.conf")
public class TestConfiguration {
    private List<ExposureTimeConfiguration> scorings;
    
    public ExposureTimeConfiguration getConfigurationFor(int venueType, int venueCategory1, int venueCategory2) {
        return scorings.stream()
            .filter(scoring -> scoring.isCompatibleWith(venueType, venueCategory1, venueCategory2))
            .max(Comparator.comparing(ScoringConfiguration::getPriority))
            .get();
    }
}
