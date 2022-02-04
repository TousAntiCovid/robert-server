package fr.gouv.stopc.e2e;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;

import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class ParameterTypesTest {

    final ParameterTypes parameters = new ParameterTypes();

    @Nested
    class NaturalTimeTest {

        @Test
        void can_parse_now() {
            assertThat(parameters.naturalTime("maintenant"))
                    .isCloseTo(Instant.now(), within(5, SECONDS));
        }

        @ParameterizedTest
        @ValueSource(ints = { 1, 2, 3, 4, 5 })
        void can_parse_x_days_ago(final int x) {
            assertThat(parameters.naturalTime(x + " jours"))
                    .isCloseTo(Instant.now().minus(x, DAYS), within(5, SECONDS));
        }
    }
}
