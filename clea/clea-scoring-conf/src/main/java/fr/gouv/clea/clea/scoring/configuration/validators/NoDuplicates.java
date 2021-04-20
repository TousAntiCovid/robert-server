package fr.gouv.clea.clea.scoring.configuration.validators;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;

@Constraint(validatedBy = {NoDuplicatesValidator.class})
@Target({TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface NoDuplicates {

    String message() default
            "Found at least one duplicate in scoring configuration";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}

