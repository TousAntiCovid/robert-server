package fr.gouv.clea.clea.scoring.configuration.validators;

import fr.gouv.clea.clea.scoring.configuration.ScoringConfigurationItem;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.ArrayList;
import java.util.List;

public class NoDuplicatesValidator implements ConstraintValidator<NoDuplicates, Object> {

    private List<ScoringConfigurationItem> list;

    @Override
    public void initialize(NoDuplicates noDuplicates) {
        this.list = new ArrayList<>();
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {

        ScoringConfigurationItem scoringConfigurationItem = (ScoringConfigurationItem) value;
        if (list.contains(scoringConfigurationItem)) {
            return false;
        } else {
            list.add(scoringConfigurationItem);
            return true;
        }
    }

}
