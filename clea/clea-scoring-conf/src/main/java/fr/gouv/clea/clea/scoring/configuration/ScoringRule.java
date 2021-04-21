package fr.gouv.clea.clea.scoring.configuration;

import javax.validation.Valid;
import javax.validation.constraints.Min;

import fr.gouv.clea.clea.scoring.configuration.validators.NoDuplicates;
import fr.gouv.clea.clea.scoring.configuration.validators.ValidateWildcards;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@Getter
@Valid
@ValidateWildcards
@NoDuplicates
public class ScoringRule {

    public final static int WILDCARD_VALUE = -1;

    @Min(value = -1)
    private int venueType;

    @Min(value = -1)
    private int venueCategory1;

    @Min(value = -1)
    private int venueCategory2;

    public boolean isCompatibleWith(int venueType, int venueCategory1, int venueCategory2) {
        return this.hasVenueTypeCompatibleWith(venueType)
                && this.hasVenueCategory1CompatibleWith(venueCategory1)
                && this.hasVenueCategory2CompatibleWith(venueCategory2);
    }

    public boolean hasVenueTypeCompatibleWith(int venueType) {
        return this.getVenueType() == venueType || this.getVenueType() == ScoringRule.WILDCARD_VALUE;
    }

    public boolean hasVenueCategory1CompatibleWith(int venueCategory1) {
        return this.getVenueCategory1() == venueCategory1 || this.getVenueCategory1() == ScoringRule.WILDCARD_VALUE;
    }

    public boolean hasVenueCategory2CompatibleWith(int venueCategory2) {
        return this.getVenueCategory2() == venueCategory2 || this.getVenueCategory2() == ScoringRule.WILDCARD_VALUE;
    }

    public int getWildCardCount() {
        int count = 0;
        if (this.venueType == ScoringRule.WILDCARD_VALUE) {
            count++;
        }
        if (this.venueCategory1 == ScoringRule.WILDCARD_VALUE) {
            count++;
        }
        if (this.venueCategory2 == ScoringRule.WILDCARD_VALUE) {
            count++;
        }
        return count;
    }

    public boolean isFullMatch() {
        return this.getWildCardCount() == 0;
    }
    
    public boolean isDefaultRule() {
        return this.getWildCardCount() == 3;
    }
    
    public boolean isCategory1Wildcarded() {
        return this.getVenueCategory1() == ScoringRule.WILDCARD_VALUE;
    }

    public boolean isCategory2Wildcarded() {
        return this.getVenueCategory2() == ScoringRule.WILDCARD_VALUE;
    }

}
