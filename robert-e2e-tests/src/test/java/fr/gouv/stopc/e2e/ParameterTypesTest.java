package fr.gouv.stopc.e2e;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static java.time.temporal.ChronoUnit.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class ParameterTypesTest {

    final ParameterTypes parameters = new ParameterTypes();

    private static List<Arguments> can_parse_relative_time() {
        return List.of(
                Arguments.of("il y a 1 jour", Instant.now().minus(1, DAYS)),
                Arguments.of("il y a 22 jours", Instant.now().minus(22, DAYS)),
                Arguments.of("il y a 1 heure", Instant.now().minus(1, HOURS)),
                Arguments.of("il y a 19 heures", Instant.now().minus(19, HOURS)),
                Arguments.of("il y a 1 minute", Instant.now().minus(1, MINUTES)),
                Arguments.of("il y a 33 minutes", Instant.now().minus(33, MINUTES)),
                Arguments.of("il y a 2 jours 3 heures et 5 minutes", Instant.now().minus(Duration.parse("PT51H5M"))),
                Arguments.of("maintenant", Instant.now()),
                Arguments.of("dans 1 jour", Instant.now().plus(1, DAYS)),
                Arguments.of("dans 22 jours", Instant.now().plus(22, DAYS)),
                Arguments.of("dans 1 heure", Instant.now().plus(1, HOURS)),
                Arguments.of("dans 19 heures", Instant.now().plus(19, HOURS)),
                Arguments.of("dans 1 minute", Instant.now().plus(1, MINUTES)),
                Arguments.of("dans 33 minutes", Instant.now().plus(33, MINUTES)),
                Arguments.of("dans 1 jour 2 heures et 33 minutes", Instant.now().plus(Duration.parse("PT26H33M")))
        );
    }

    @ParameterizedTest
    @MethodSource
    void can_parse_relative_time(final String timeExpression, final Instant expectedInstant) {
        assertThat(parameters.relativeTime(timeExpression))
                .isCloseTo(expectedInstant, within(5, SECONDS));
    }

    private static List<Arguments> can_parse_expression() {
        return List.of(
                Arguments.of("1 jour", Duration.ofDays(1)),
                Arguments.of("22 jours", Duration.ofDays(22)),
                Arguments.of("1 heure", Duration.ofHours(1)),
                Arguments.of("19 heures", Duration.ofHours(19)),
                Arguments.of("1 minute", Duration.ofMinutes(1)),
                Arguments.of("33 minutes", Duration.ofMinutes(33)),
                Arguments.of("1 jour 2 heures et 33 minutes", Duration.parse("PT26H33M"))
        );
    }

    @ParameterizedTest
    @MethodSource
    void can_parse_expression(final String durationExpression, final Duration expectedDuration) {
        assertThat(parameters.duration(durationExpression))
                .isEqualTo(expectedDuration);
    }
}
