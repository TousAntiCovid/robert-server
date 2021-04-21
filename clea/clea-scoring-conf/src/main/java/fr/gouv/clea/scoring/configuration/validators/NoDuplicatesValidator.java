package fr.gouv.clea.scoring.configuration.validators;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import fr.gouv.clea.scoring.configuration.ScoringRule;

import java.util.ArrayList;
import java.util.List;

public class NoDuplicatesValidator implements ConstraintValidator<NoDuplicates, Object> {

    private List<ScoringRule> list;

    @Override
    public void initialize(NoDuplicates noDuplicates) {
        this.list = new ArrayList<>();
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {

        ScoringRule scoringRule = (ScoringRule) value;
        if (list.contains(scoringRule)) {
            return false;
        } else {
            list.add(scoringRule);
            return true;
        }
    }

}
