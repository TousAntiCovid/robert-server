package fr.gouv.stopc.submission.code.server.ws.validators;

import fr.gouv.stopc.submission.code.server.ws.annotations.CodePerDay;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

@Slf4j
public class CodePerDayValidator implements ConstraintValidator<CodePerDay, Long> {

    @Value("${generation.code.per.day.max}")
    Long maxCodePerDay;

    @Value("${generation.code.per.day.min}")
    Long minCodePerDay;

    /**
     * Validate codePerDay value. It should be contained between code.per.day.max and code.per.day.min
     * @param codePerDay code type to test
     * @return return true if code type to test corresponding to a CodeTypeEnum.
     */
    @Override
    public boolean isValid(Long codePerDay, ConstraintValidatorContext constraintValidatorContext) {
       return codePerDay != null && minCodePerDay <= codePerDay && codePerDay <= maxCodePerDay;
    }
}
