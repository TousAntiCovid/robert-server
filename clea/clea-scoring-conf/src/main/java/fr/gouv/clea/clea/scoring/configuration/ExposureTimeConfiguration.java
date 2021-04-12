package fr.gouv.clea.clea.scoring.configuration;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@ToString(callSuper = true)
@Getter
public class ExposureTimeConfiguration extends ScoringConfiguration {
    private int exposureTime;
    
    @Builder
    public ExposureTimeConfiguration(int venueType, int venueCategory1, int venueCategory2, int exposureTime){
      super(venueType, venueCategory1, venueCategory2);
      this.exposureTime = exposureTime; 
    }

}
