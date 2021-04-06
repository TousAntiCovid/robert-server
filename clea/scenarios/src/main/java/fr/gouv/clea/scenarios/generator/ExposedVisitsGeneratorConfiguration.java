package fr.gouv.clea.scenarios.generator;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ExposedVisitsGeneratorConfiguration {

    private int nbVenues = 80;
    private int nbVisitors = 5;
    private int nbVisitsPerVisitorPerDay = 4;
    private int retentionDays = 12;
    private int nbHangouts = 8;
    // private VenueTypeVo venueType = VenueTypeVo.N;
    // private VenueCategoryVo venueCategory = VenueCategoryVo.CAT5;
    private String manualContactTracingAuthorityPublicKey;
    private String serverAuthorityPublicKey;
}
