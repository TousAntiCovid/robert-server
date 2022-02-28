package fr.gouv.stopc.e2e;

import ch.qos.logback.classic.Level;
import io.cucumber.java.ParameterType;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.TemporalUnit;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.time.temporal.ChronoUnit.*;

public class ParameterTypes {

    @ParameterType("(maintenant|dans .*|il y a .*)")
    public Instant relativeTime(final String timeExpression) {
        if ("maintenant".equals(timeExpression)) {
            return Instant.now();
        } else if (timeExpression.startsWith("il y a ")) {
            final var delta = duration(timeExpression);
            return Instant.now().minus(delta);
        } else if (timeExpression.startsWith("dans ")) {
            final var delta = duration(timeExpression);
            return Instant.now().plus(delta);
        }
        throw new IllegalArgumentException("can't parse relative time expression: " + timeExpression);
    }

    @ParameterType("(DEBUG|INFO|WARN|ERROR)")
    public Level logLevel(final String logLevel) {
        return Level.valueOf(logLevel);
    }

    @ParameterType(".*")
    public Duration duration(final String durationExpression) {
        final var matcher = Pattern.compile("(\\d+) (jours?|heures?|minutes?)")
                .matcher(durationExpression);
        final var expressions = Stream.<Duration>builder();
        while (matcher.find()) {
            final var amount = Integer.parseInt(matcher.group(1));
            final TemporalUnit unit;
            switch (matcher.group(2)) {
                case "jour":
                case "jours":
                    unit = DAYS;
                    break;
                case "heure":
                case "heures":
                    unit = HOURS;
                    break;
                case "minute":
                case "minutes":
                    unit = MINUTES;
                    break;
                default:
                    throw new IllegalArgumentException("Invalid unit in time expression: " + durationExpression);
            }
            expressions.add(Duration.of(amount, unit));
        }
        return expressions.build()
                .reduce(Duration.ZERO, Duration::plus);
    }

    @ParameterType(".*")
    public List<String> wordList(final String words) {
        return Arrays.asList(words.split("\\s*,\\s*|\\s*et\\s*"));
    }
}
