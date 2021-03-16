package fr.gouv.stopc.submission.code.server.ws.annotations;

import fr.gouv.stopc.submission.code.server.ws.validators.CodePerDayValidator;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;


@Target({FIELD})
@Retention(RUNTIME)
@Documented
@Constraint(validatedBy = {CodePerDayValidator.class})
/**
 * @see CodePerDayValidator description
 */
public @interface CodePerDay {
    String message() default "{value} error";
    Class<?>[] groups() default { };
    Class<? extends Payload>[] payload() default { };
}