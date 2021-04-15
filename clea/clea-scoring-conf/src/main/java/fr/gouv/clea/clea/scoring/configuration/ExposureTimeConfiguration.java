package fr.gouv.clea.clea.scoring.configuration;

import java.util.List;

import javax.validation.constraints.NotEmpty;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import lombok.Setter;

@Validated
@Configuration
@ConfigurationProperties(prefix="clea.conf")
public class ExposureTimeConfiguration extends ScoringConfiguration {
    @Setter
    @NotEmpty
    protected List<ExposureTimeConfigurationItem> scorings;

    public List<? extends ScoringConfigurationItem> getScorings() {
        return scorings;
    }

    public ExposureTimeConfigurationItem getConfigurationFor(int venueType, int venueCategory1, int venueCategory2) {
        return (ExposureTimeConfigurationItem) super.getConfigurationFor(venueType, venueCategory1, venueCategory2);
    }
}
