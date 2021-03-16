package fr.gouv.stopc.submission.code.server.ws.validators;

import fr.gouv.stopc.submission.code.server.ws.annotations.PresentOrFutureTruncateDay;
import lombok.extern.slf4j.Slf4j;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

@Slf4j
public class PresentOrFutureTruncateDayValidator implements ConstraintValidator<PresentOrFutureTruncateDay, OffsetDateTime> {

    /**
     * @param date date to test
     * @return return true if date is sup or equals by day of server date.
     */
    @Override
    public boolean isValid(OffsetDateTime date, ConstraintValidatorContext constraintValidatorContext) {
        final OffsetDateTime now = OffsetDateTime.now();
        final ZoneOffset offset = now.getOffset();
        final OffsetDateTime dateToTest = date.toInstant().atOffset(offset);
        return ChronoUnit.SECONDS.between(dateToTest, now) <= 60;
    }
}