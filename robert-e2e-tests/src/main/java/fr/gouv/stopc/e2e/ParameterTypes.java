package fr.gouv.stopc.e2e;

import ch.qos.logback.classic.Level;
import io.cucumber.java.ParameterType;
import org.ocpsoft.prettytime.nlp.PrettyTimeParser;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;

import static java.lang.String.format;

public class ParameterTypes {

    @ParameterType(".*")
    public Instant naturalTime(final String timeExpression) {
        final var dates = new PrettyTimeParser()
                .parse(timeExpression);
        if (dates.size() != 1) {
            throw new IllegalArgumentException(
                    format(
                            "Expecting date to find exactly 1 date expression but found %d in '%s'",
                            dates.size(), timeExpression
                    )
            );
        }
        return dates.stream()
                .findFirst()
                .orElseThrow()
                .toInstant();
    }

    @ParameterType("(DEBUG|INFO|WARN|ERROR)")
    public Level logLevel(final String logLevel) {
        return Level.valueOf(logLevel);
    }

    @ParameterType("(\\d+) (days|hours|minutes)")
    public Duration duration(final String amountExpression, final String unitExpression) {
        final var amount = Integer.parseInt(amountExpression);
        final var unit = ChronoUnit.valueOf(unitExpression.toUpperCase());
        return Duration.of(amount, unit);
    }

    @ParameterType(".*")
    public List<String> wordList(final String words) {
        return Arrays.asList(words.split("\\s*,\\s*|\\s*and\\s*"));
    }
}
