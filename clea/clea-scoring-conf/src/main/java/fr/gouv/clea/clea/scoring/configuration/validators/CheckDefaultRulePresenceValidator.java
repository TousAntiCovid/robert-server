package fr.gouv.clea.clea.scoring.configuration.validators;

import fr.gouv.clea.clea.scoring.configuration.ScoringRule;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.List;

public class CheckDefaultRulePresenceValidator implements ConstraintValidator<CheckDefaultRulePresence, List<? extends ScoringRule>> {

    @Override
    public boolean isValid(List<? extends ScoringRule> ruleList, ConstraintValidatorContext context) {

        return ruleList.stream().anyMatch(ScoringRule::isDefaultRule);

    }

}
