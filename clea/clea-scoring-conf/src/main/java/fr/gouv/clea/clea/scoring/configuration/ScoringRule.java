package fr.gouv.clea.clea.scoring.configuration;

import fr.gouv.clea.clea.scoring.configuration.validators.NoDuplicates;
import fr.gouv.clea.clea.scoring.configuration.validators.ValidateWildcards;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import java.util.Objects;

// @Valid
@SuperBuilder
@AllArgsConstructor
@ToString
@Getter
@Valid
@ValidateWildcards
@NoDuplicates
public class ScoringRule {

    public static int wildcardValue = -1;

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
        return this.getVenueType() == venueType || this.getVenueType() == ScoringRule.wildcardValue;
    }

    public boolean hasVenueCategory1CompatibleWith(int venueCategory1) {
        return this.getVenueCategory1() == venueCategory1 || this.getVenueCategory1() == ScoringRule.wildcardValue;
    }

    public boolean hasVenueCategory2CompatibleWith(int venueCategory2) {
        return this.getVenueCategory2() == venueCategory2 || this.getVenueCategory2() == ScoringRule.wildcardValue;
    }

    public int getWildCardCount() {
        int count = 0;
        if (this.venueType == ScoringRule.wildcardValue) {
            count++;
        }
        if (this.venueCategory1 == ScoringRule.wildcardValue) {
            count++;
        }
        if (this.venueCategory2 == ScoringRule.wildcardValue) {
            count++;
        }
        return count;
    }

    public boolean isFullMatch() {
        return this.getWildCardCount() == 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ScoringRule that = (ScoringRule) o;
        return getVenueType() == that.getVenueType() && getVenueCategory1() == that.getVenueCategory1() && getVenueCategory2() == that.getVenueCategory2();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getVenueType(), getVenueCategory1(), getVenueCategory2());
    }
}
