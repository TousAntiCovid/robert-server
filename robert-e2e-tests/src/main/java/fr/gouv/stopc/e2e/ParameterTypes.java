package fr.gouv.stopc.e2e;

import ch.qos.logback.classic.Level;
import io.cucumber.java.ParameterType;

import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class ParameterTypes {

    @ParameterType(".*")
    public Instant naturalTime(final String timeExpression) {
        if ("maintenant".equals(timeExpression)) {
            return Instant.now();
        }
        final var matcher = Pattern.compile("^(\\d+) jours$")
                .matcher(timeExpression);
        if (!matcher.find()) {
            throw new IllegalArgumentException("time expression can't be parsed: " + timeExpression);
        }
        final var days = Integer.parseInt(matcher.group(1));
        return ZonedDateTime.now()
                .minusDays(days)
                .toInstant();
    }

    @ParameterType("(DEBUG|INFO|WARN|ERROR)")
    public Level logLevel(final String logLevel) {
        return Level.valueOf(logLevel);
    }

    @ParameterType("(\\d+) (jours?|heures?|minutes?)")
    public Duration duration(final String amountExpression, final String unitExpression) {
        final var amount = Integer.parseInt(amountExpression);
        final var unit = ChronoUnit.valueOf(unitExpression.toUpperCase());
        return Duration.of(amount, unit);
    }

    @ParameterType(".*")
    public List<String> wordList(final String words) {
        return Arrays.asList(words.split("\\s*,\\s*|\\s*et\\s*"));
    }
}
