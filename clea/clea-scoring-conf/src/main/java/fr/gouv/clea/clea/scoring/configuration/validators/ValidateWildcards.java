package fr.gouv.clea.clea.scoring.configuration.validators;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;

@Constraint(validatedBy = {VenueTypeValidator.class})
@Target({TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidateWildcards {

    String message() default
            "VenueType can not be wildcarded unless all fields are too";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
