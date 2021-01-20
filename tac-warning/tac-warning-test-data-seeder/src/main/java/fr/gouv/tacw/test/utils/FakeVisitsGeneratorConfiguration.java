package fr.gouv.tacw.test.utils;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import fr.gouv.tacw.ws.vo.VenueCategoryVo;
import fr.gouv.tacw.ws.vo.VenueTypeVo;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "test.data")
public class FakeVisitsGeneratorConfiguration {
    private int nbVenues = 80;
    private int nbVisitors = 5;
    private int nbVisitsPerVisitorPerDay = 4;
    private int retentionDays = 12;
    private int nbHangouts = 8;
    private VenueTypeVo venueType = VenueTypeVo.N;
    private VenueCategoryVo venueCategory = VenueCategoryVo.CAT5;
    
}
