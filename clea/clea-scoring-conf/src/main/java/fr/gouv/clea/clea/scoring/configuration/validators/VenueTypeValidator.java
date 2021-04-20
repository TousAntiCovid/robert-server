package fr.gouv.clea.clea.scoring.configuration.validators;

import fr.gouv.clea.clea.scoring.configuration.ScoringConfigurationItem;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class VenueTypeValidator implements ConstraintValidator<ValidateWildcards, Object> {

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {

        ScoringConfigurationItem scoringConfigurationItem = (ScoringConfigurationItem) value;
        if (allFieldsAreWildcards(scoringConfigurationItem)) {
            return true;
        } else {
            return venueTypeIsNotWildcarded(scoringConfigurationItem);
        }
    }

    private boolean venueTypeIsNotWildcarded(ScoringConfigurationItem scoringConfigurationItem) {
        return scoringConfigurationItem.getVenueType() != ScoringConfigurationItem.wildcardValue ||
                (scoringConfigurationItem.getVenueCategory1() != ScoringConfigurationItem.wildcardValue &&
                        scoringConfigurationItem.getVenueCategory2() != ScoringConfigurationItem.wildcardValue);
    }

    private boolean allFieldsAreWildcards(ScoringConfigurationItem scoringConfigurationItem) {
        return scoringConfigurationItem.getVenueType() == ScoringConfigurationItem.wildcardValue &&
                scoringConfigurationItem.getVenueCategory1() == ScoringConfigurationItem.wildcardValue &&
                scoringConfigurationItem.getVenueCategory2() == ScoringConfigurationItem.wildcardValue;
    }


}
