package fr.gouv.stopc.robert.server.common.service;

import fr.gouv.stopc.robert.server.common.utils.TimeUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

public class RobertClockTest {

    final RobertClock robertClock = new RobertClock("2022-01-01");

    static List<Arguments> values() {
        return List.of(
                arguments(Instant.parse("2022-01-01T00:00:00Z"), 3849984000L, 0),
                arguments(Instant.parse("2022-01-01T00:14:59Z"), 3849984899L, 0),
                arguments(Instant.parse("2022-01-01T00:14:59.999Z"), 3849984899L, 0),
                arguments(Instant.parse("2022-01-01T00:15:00Z"), 3849984900L, 1),
                arguments(Instant.parse("2022-01-01T00:29:59Z"), 3849985799L, 1),
                arguments(Instant.parse("2022-01-01T00:29:59.999999Z"), 3849985799L, 1),
                arguments(Instant.parse("2022-01-01T00:30:00Z"), 3849985800L, 2),
                arguments(Instant.parse("2022-02-15T22:43:13Z"), 3853953793L, 4410)
        );
    }

    @ParameterizedTest
    @MethodSource("values")
    void timeutils_returns_same_values(final Instant instant, final long ntpTimestamp, final int epochId) {
        final var timeUtilsNtp = TimeUtils.convertUnixMillistoNtpSeconds(instant.toEpochMilli());
        assertThat(timeUtilsNtp)
                .isEqualTo(ntpTimestamp);

        final var ntpTimeStampClockStart = TimeUtils.convertUnixMillistoNtpSeconds(
                Instant.parse("2022-01-01T00:00:00Z").toEpochMilli()
        );
        assertThat(TimeUtils.getNumberOfEpochsBetween(ntpTimeStampClockStart, timeUtilsNtp))
                .isEqualTo(epochId);
    }

    @ParameterizedTest
    @MethodSource("values")
    void can_convert_values_from_instant(final Instant instant, final long ntpTimestamp, final int epochId) {
        final var robertInstant = robertClock.at(instant);
        assertThat(robertInstant.asNtpTimestamp())
                .isEqualTo(ntpTimestamp);
        assertThat(robertInstant.asEpochId())
                .isEqualTo(epochId);
    }

    @Test
    void can_generate_useful_toString() {
        final var instant = Instant.parse("2022-04-23T08:30:00Z");
        assertThat(robertClock.at(instant).toString())
                .isEqualTo("2022-04-23T08:30:00Z=10786E");
    }
}
