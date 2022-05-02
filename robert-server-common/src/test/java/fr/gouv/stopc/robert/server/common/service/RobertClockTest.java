package fr.gouv.stopc.robert.server.common.service;

import fr.gouv.stopc.robert.server.common.utils.TimeUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Instant;
import java.util.List;

import static fr.gouv.stopc.robert.server.common.service.RobertClock.ROBERT_EPOCH;
import static java.time.temporal.ChronoUnit.*;
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

    @Test
    void can_add_time() {
        final var instant = Instant.parse("2022-04-23T08:30:00Z");
        final var robertInstant = robertClock.at(instant);

        // add 30 minutes
        assertThat(robertInstant.plus(1_800_000, MILLIS)).hasToString("2022-04-23T09:00:00Z=10788E");
        assertThat(robertInstant.plus(1_800, SECONDS)).hasToString("2022-04-23T09:00:00Z=10788E");
        assertThat(robertInstant.plus(30, MINUTES)).hasToString("2022-04-23T09:00:00Z=10788E");
        assertThat(robertInstant.plus(2, ROBERT_EPOCH)).hasToString("2022-04-23T09:00:00Z=10788E");

        // add 2 days
        assertThat(robertInstant.plus(48, HOURS)).hasToString("2022-04-25T08:30:00Z=10978E");
        assertThat(robertInstant.plus(2, DAYS)).hasToString("2022-04-25T08:30:00Z=10978E");
        assertThat(robertInstant.plus(192, ROBERT_EPOCH)).hasToString("2022-04-25T08:30:00Z=10978E");
    }

    @Test
    void can_subtract_time() {
        final var instant = Instant.parse("2022-04-23T08:30:00Z");
        final var robertInstant = robertClock.at(instant);

        // subtract 30 minutes
        assertThat(robertInstant.minus(1_800_000, MILLIS)).hasToString("2022-04-23T08:00:00Z=10784E");
        assertThat(robertInstant.minus(1_800, SECONDS)).hasToString("2022-04-23T08:00:00Z=10784E");
        assertThat(robertInstant.minus(30, MINUTES)).hasToString("2022-04-23T08:00:00Z=10784E");
        assertThat(robertInstant.minus(2, ROBERT_EPOCH)).hasToString("2022-04-23T08:00:00Z=10784E");

        // subtract 2 days
        assertThat(robertInstant.minus(48, HOURS)).hasToString("2022-04-21T08:30:00Z=10594E");
        assertThat(robertInstant.minus(2, DAYS)).hasToString("2022-04-21T08:30:00Z=10594E");
        assertThat(robertInstant.minus(192, ROBERT_EPOCH)).hasToString("2022-04-21T08:30:00Z=10594E");
    }

    @Test
    void can_truncate_to_robert_epoch() {
        final var instant = Instant.parse("2022-04-23T08:35:12.004Z");
        final var robertInstant = robertClock.at(instant);
        assertThat(robertInstant.truncatedTo(ROBERT_EPOCH)).hasToString("2022-04-23T08:30:00Z=10786E");
    }
}
