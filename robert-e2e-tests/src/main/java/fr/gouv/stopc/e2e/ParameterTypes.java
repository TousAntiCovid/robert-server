package fr.gouv.stopc.e2e;

import io.cucumber.java.ParameterType;
import org.ocpsoft.prettytime.nlp.PrettyTimeParser;

import java.time.Instant;

import static java.lang.String.format;

public class ParameterTypes {

    @ParameterType(".*")
    public Instant naturalTime(final String timeExpression) {
        final var dates = new PrettyTimeParser()
                .parse(timeExpression);
        if (dates.size() != 1) {
            final var message = format(
                    "Expecting to find exactly 1 date expression but found %d in '%s'",
                    dates.size(), timeExpression
            );
            throw new IllegalArgumentException(message);
        }
        return dates.stream()
                .findAny()
                .orElseThrow()
                .toInstant();
    }
}
