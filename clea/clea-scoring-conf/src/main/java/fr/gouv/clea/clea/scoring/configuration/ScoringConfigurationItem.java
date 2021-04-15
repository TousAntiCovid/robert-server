package fr.gouv.clea.clea.scoring.configuration;

import javax.validation.constraints.Min;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

// @Valid
@SuperBuilder
@AllArgsConstructor
@ToString
@Getter
public class ScoringConfigurationItem {
    public static int wildcardValue = -1;
    @Min(value = -1)
    private int venueType;
    @Min(value = -1)
    private int venueCategory1;
    @Min(value = -1)
    private int venueCategory2;
    
    /**
     * @return the scoring configuration priority. The greater it is, the most priority it has.
     *  It allows to select a scoring configuration between many that are compatible for a 
     *  tuple(venueType, venueCategory1, venueCategory2).
     */
    public int getPriority() {
        int priority = 100;
        if (venueType == wildcardValue) {
            // venueType is the most important part for configuration selection
            priority -= 51;
        }
        if (venueCategory1 == wildcardValue) {
            // venueCategory1 has priority over venueCategory2
            priority -= 25;
        }
        if (venueCategory2 == wildcardValue) {
            priority -= 20;
        }
        return priority;
    }

    public boolean isCompatibleWith(int venueType, int venueCategory1, int venueCategory2) {
        return this.hasVenueTypeCompatibleWith(venueType)
                && this.hasVenueCategory1CompatibleWith(venueCategory1)
                && this.hasVenueCategory2CompatibleWith(venueCategory2);
    }

    public boolean hasVenueTypeCompatibleWith(int venueType) {
        return this.getVenueType() == venueType || this.getVenueType() == ScoringConfigurationItem.wildcardValue;
    } 

    public boolean hasVenueCategory1CompatibleWith(int venueCategory1) {
        return this.getVenueCategory1() == venueType || this.getVenueCategory1() == ScoringConfigurationItem.wildcardValue;
    } 

    public boolean hasVenueCategory2CompatibleWith(int venueCategory2) {
        return this.getVenueCategory2() == venueType || this.getVenueCategory2() == ScoringConfigurationItem.wildcardValue;
    } 
    
}
