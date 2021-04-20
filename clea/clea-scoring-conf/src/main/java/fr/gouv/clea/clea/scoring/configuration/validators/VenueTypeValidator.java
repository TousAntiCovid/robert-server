package fr.gouv.clea.clea.scoring.configuration.validators;

import fr.gouv.clea.clea.scoring.configuration.ScoringRule;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class VenueTypeValidator implements ConstraintValidator<ValidateWildcards, Object> {

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {

        ScoringRule scoringRule = (ScoringRule) value;
        if (allFieldsAreWildcards(scoringRule)) {
            return true;
        } else {
            return venueTypeIsNotWildcarded(scoringRule);
        }
    }

    private boolean venueTypeIsNotWildcarded(ScoringRule scoringRule) {
        return scoringRule.getVenueType() != ScoringRule.wildcardValue ||
                (scoringRule.getVenueCategory1() != ScoringRule.wildcardValue &&
                        scoringRule.getVenueCategory2() != ScoringRule.wildcardValue);
    }

    private boolean allFieldsAreWildcards(ScoringRule scoringRule) {
        return scoringRule.getVenueType() == ScoringRule.wildcardValue &&
                scoringRule.getVenueCategory1() == ScoringRule.wildcardValue &&
                scoringRule.getVenueCategory2() == ScoringRule.wildcardValue;
    }


}
