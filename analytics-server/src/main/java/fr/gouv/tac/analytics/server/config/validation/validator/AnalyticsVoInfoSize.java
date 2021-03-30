package fr.gouv.tac.analytics.server.config.validation.validator;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = AnalyticsVoInfoSizeValidator.class)
public @interface AnalyticsVoInfoSize {

    String message() default "Too many info";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {

    };
}
