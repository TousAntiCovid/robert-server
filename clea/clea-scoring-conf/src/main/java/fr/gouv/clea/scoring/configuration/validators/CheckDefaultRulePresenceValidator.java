package fr.gouv.clea.scoring.configuration.validators;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import fr.gouv.clea.scoring.configuration.ScoringRule;

import java.util.List;

public class CheckDefaultRulePresenceValidator implements ConstraintValidator<CheckDefaultRulePresence, List<? extends ScoringRule>> {

    @Override
    public boolean isValid(List<? extends ScoringRule> ruleList, ConstraintValidatorContext context) {

        return ruleList.stream().anyMatch(ScoringRule::isDefaultRule);

    }

}
