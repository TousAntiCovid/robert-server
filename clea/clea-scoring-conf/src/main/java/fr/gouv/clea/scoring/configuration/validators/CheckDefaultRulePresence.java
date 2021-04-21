package fr.gouv.clea.scoring.configuration.validators;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Constraint(validatedBy = {CheckDefaultRulePresenceValidator.class})
@Retention(RetentionPolicy.RUNTIME)
public @interface CheckDefaultRulePresence {

    String message() default
            "Missing default rule with tuple * * *";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
