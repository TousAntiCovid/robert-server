package fr.gouv.stopc.robertserver.common;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Instant;
import java.util.List;

import static fr.gouv.stopc.robertserver.common.RobertClock.ROBERT_EPOCH;
import static java.time.temporal.ChronoUnit.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class RobertClockTest {

    final RobertClock robertClock = new RobertClock("2022-01-01");

    static List<Arguments> values() {
        return List.of(
                arguments(Instant.parse("2022-01-01T00:00:00Z"), 3849984000L, 0, "E57A1800"),
                arguments(Instant.parse("2022-01-01T00:14:59Z"), 3849984899L, 0, "E57A1B83"),
                arguments(Instant.parse("2022-01-01T00:14:59.999Z"), 3849984899L, 0, "E57A1B83"),
                arguments(Instant.parse("2022-01-01T00:15:00Z"), 3849984900L, 1, "E57A1B84"),
                arguments(Instant.parse("2022-01-01T00:29:59Z"), 3849985799L, 1, "E57A1F07"),
                arguments(Instant.parse("2022-01-01T00:29:59.999999Z"), 3849985799L, 1, "E57A1F07"),
                arguments(Instant.parse("2022-01-01T00:30:00Z"), 3849985800L, 2, "E57A1F08"),
                arguments(Instant.parse("2022-02-15T22:43:13Z"), 3853953793L, 4410, "E5B6AB01"),
                arguments(Instant.parse("2035-02-15T22:43:13Z"), 4264180993L, 460218, "FE2A3D01"),
                arguments(Instant.parse("3052-11-14T22:34:59Z"), 36381134099L, 36145722, "787C0D13"),
                arguments(Instant.parse("4037-11-23T09:22:44.999900Z"), 67465473764L, 70683877, "B5416EE4")
        );
    }

    @ParameterizedTest
    @MethodSource("values")
    void can_convert_instant_to_ntp_timestamp(final Instant instant, final long ntpTimestamp, final int epochId,
            final String time32AsHexString) {
        final var robertInstant = robertClock.at(instant);
        assertThat(robertInstant.asNtpTimestamp())
                .isEqualTo(ntpTimestamp);
    }

    @ParameterizedTest
    @MethodSource("values")
    void can_convert_instant_to_robert_epoch_id(final Instant instant, final long ntpTimestamp, final int epochId,
            final String time32AsHexString) {
        final var robertInstant = robertClock.at(instant);
        assertThat(robertInstant.asEpochId())
                .isEqualTo(epochId);
    }

    @ParameterizedTest
    @MethodSource("values")
    void can_convert_instant_to_time32(final Instant instant, final long ntpTimestamp, final int epochId,
            final String time32AsHexString) {
        final var robertInstant = robertClock.at(instant);
        assertThat(robertInstant.asTime32())
                .asHexString()
                .isEqualTo(time32AsHexString);
    }

    @ParameterizedTest
    @CsvSource({
            "2022-04-23T08:35:12.004Z, 14528",
            "1980-01-01T00:00:00.000Z, 9344",
            "2010-05-04T12:00:30.000Z, 35550"
    })
    void can_convert_instant_to_16_less_significant_bits(final Instant instant, final int expected16LsbValue) {
        final var robertInstant = robertClock.at(instant);
        assertThat(robertInstant.as16LessSignificantBits())
                .isEqualTo(expected16LsbValue);
    }

    @ParameterizedTest
    @MethodSource("values")
    void can_convert_ntp_timestamp_to_instant(final Instant instant, final long ntpTimestamp, final int epochId,
            final String time32AsHexString) {
        assertThat(robertClock.atNtpTimestamp(ntpTimestamp))
                .extracting(RobertClock.RobertInstant::asInstant)
                .isEqualTo(instant.truncatedTo(SECONDS));
    }

    @ParameterizedTest
    @CsvSource({
            "2022-01-01T00:00:00Z, E57A1800",
            "2022-01-01T00:14:59Z, E57A1B83",
            "2022-01-01T00:14:59Z, E57A1B83",
            "2022-01-01T00:15:00Z, E57A1B84",
            "2022-01-01T00:29:59Z, E57A1F07",
            "2022-01-01T00:29:59Z, E57A1F07",
            "2022-01-01T00:30:00Z, E57A1F08",
            "2022-02-15T22:43:13Z, E5B6AB01",
            "2035-02-15T22:43:13Z, FE2A3D01"
    })
    @SneakyThrows
    void can_convert_time32_to_instant(final Instant instant, final String time32AsHexString) {
        final var time32 = hexStringToByteArray(time32AsHexString);
        assertThat(robertClock.atTime32(time32))
                .extracting(RobertClock.RobertInstant::asInstant)
                .isEqualTo(instant.truncatedTo(SECONDS));
    }

    @ParameterizedTest
    @CsvSource({
            "3052-11-14T22:34:59Z,        787C0D13, 3052-11-14T22:34:59Z",
            "4037-11-23T09:22:44.999900Z, B5416EE4, 3052-11-14T22:34:59Z"
    })
    @SneakyThrows
    void after_2036_time32_to_instant_conversion_doesnt_work(final Instant inputInstant, final String time32AsHexString,
            final Instant outputInstant) {
        final var robertInstant = robertClock.at(inputInstant);

        final var instantAsTime32 = robertInstant.asTime32();
        assertThat(instantAsTime32)
                .asHexString()
                .isEqualTo(time32AsHexString);

        final var robertInstantFromTime32 = robertClock.atTime32(instantAsTime32);
        assertThat(robertInstantFromTime32)
                .extracting(RobertClock.RobertInstant::asInstant)
                .isNotEqualTo(outputInstant);
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

    @ParameterizedTest
    @CsvSource({
            "2022-04-23T08:00:00Z=10784E   ,2022-04-23T08:00:00Z       ,10784",
            "2022-01-01T00:00:00Z=0E       ,2022-01-01T00:00:00Z       ,0",
            "2022-01-01T00:14:59Z=0E       ,2022-01-01T00:14:59Z       ,0",
            "2022-01-01T00:14:59.999Z=0E   ,2022-01-01T00:14:59.999Z   ,0",
            "2022-01-01T00:15:00Z=1E       ,2022-01-01T00:15:00Z       ,1",
            "2022-01-01T00:29:59Z=1E       ,2022-01-01T00:29:59Z       ,1",
            "2022-01-01T00:29:59.999999Z=1E,2022-01-01T00:29:59.999999Z,1",
            "2022-01-01T00:30:00Z=2E       ,2022-01-01T00:30:00Z       ,2",
            "2022-02-15T22:43:13Z=4410E    ,2022-02-15T22:43:13Z       ,4410"
    })
    void can_parse_robert_instant_string(final String robertInstantString, final String expectedInstant,
            final int expectedEpochId) {
        final var parsedRobertInstant = RobertClock.parse(robertInstantString);
        assertThat(parsedRobertInstant.asInstant())
                .hasToString(expectedInstant);
        assertThat(parsedRobertInstant.asEpochId())
                .isEqualTo(expectedEpochId);
    }

    @ParameterizedTest
    @CsvSource({
            "2022-01-01T00:00:00Z,2022-01-01T00:00:00Z,0",
            "2022-01-01T00:00:00Z,2022-01-01T00:14:59.999Z,0",
            "2022-01-01T00:00:00Z,2022-01-01T00:15:00Z,1",
            "2022-02-15T18:43:13Z,2022-02-15T22:43:13Z,16",
            "2022-02-15T18:43:13Z,2022-02-15T22:43:13Z,16",
            "2022-02-15T18:43:13Z,2022-02-15T22:43:13Z,16",
            "2022-02-15T20:43:13Z,2022-02-15T18:13:00Z,-10",
            "1950-01-01T00:00:00Z,1950-01-01T01:00:00Z,4",
    })
    void can_count_epochs_between_two_instants(final Instant begin, final Instant end, final int epochsCount) {
        assertThat(begin.until(end, ROBERT_EPOCH))
                .isEqualTo(epochsCount);
    }

    @Test
    void can_list_epochs_until() {
        final var begin = robertClock.at(Instant.parse("2022-12-18T06:23:43Z"));
        // 1h and 5m later, or 4 epochs later
        final var end = robertClock.at(Instant.parse("2022-12-18T07:28:43Z"));
        assertThat(begin.epochsUntil(end))
                .extracting(RobertClock.RobertInstant::toString)
                .contains(
                        "2022-12-18T06:15:00Z=33721E",
                        "2022-12-18T06:30:00Z=33722E",
                        "2022-12-18T06:45:00Z=33723E",
                        "2022-12-18T07:00:00Z=33724E"
                );
    }

    private static byte[] hexStringToByteArray(final String s) {
        final int len = s.length();
        final byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }
}
