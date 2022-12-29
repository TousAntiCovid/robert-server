package fr.gouv.stopc.robertserver.common

import fr.gouv.stopc.robertserver.common.RobertClock.RobertInstant
import lombok.SneakyThrows
import org.apache.commons.codec.binary.Hex
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.MethodSource
import java.time.Instant
import java.time.temporal.ChronoUnit.DAYS
import java.time.temporal.ChronoUnit.HOURS
import java.time.temporal.ChronoUnit.MILLIS
import java.time.temporal.ChronoUnit.MINUTES
import java.time.temporal.ChronoUnit.SECONDS

class RobertClockTest {

    companion object {
        @JvmStatic
        fun values() = listOf(
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
        )
    }

    private val robertClock = RobertClock("2022-01-01")

    @ParameterizedTest
    @MethodSource("values")
    fun can_convert_instant_to_ntp_timestamp(
        instant: Instant,
        ntpTimestamp: Long,
        epochId: Int,
        time32AsHexString: String
    ) {
        val robertInstant = robertClock.at(instant)
        assertThat(robertInstant.asNtpTimestamp())
            .isEqualTo(ntpTimestamp)
    }

    @ParameterizedTest
    @MethodSource("values")
    fun can_convert_instant_to_robert_epoch_id(
        instant: Instant,
        ntpTimestamp: Long,
        epochId: Int,
        time32AsHexString: String
    ) {
        val robertInstant = robertClock.at(instant)
        assertThat(robertInstant.asEpochId())
            .isEqualTo(epochId)
    }

    @ParameterizedTest
    @MethodSource("values")
    fun can_convert_instant_to_time32(instant: Instant, ntpTimestamp: Long, epochId: Int, time32AsHexString: String) {
        val robertInstant = robertClock.at(instant)
        assertThat(robertInstant.asTime32())
            .asHexString()
            .isEqualTo(time32AsHexString)
    }

    @ParameterizedTest
    @CsvSource("2022-04-23T08:35:12.004Z, 14528", "1980-01-01T00:00:00.000Z, 9344", "2010-05-04T12:00:30.000Z, 35550")
    fun can_convert_instant_to_16_less_significant_bits(instant: Instant, expected16LsbValue: Int) {
        val robertInstant = robertClock.at(instant)
        assertThat(robertInstant.as16LessSignificantBits())
            .isEqualTo(expected16LsbValue)
    }

    @ParameterizedTest
    @MethodSource("values")
    fun can_convert_ntp_timestamp_to_instant(
        instant: Instant,
        ntpTimestamp: Long,
        epochId: Int,
        time32AsHexString: String
    ) {
        assertThat(robertClock.atNtpTimestamp(ntpTimestamp).asInstant())
            .isEqualTo(instant.truncatedTo(SECONDS))
    }

    @ParameterizedTest
    @CsvSource(
        "2022-01-01T00:00:00Z, E57A1800",
        "2022-01-01T00:14:59Z, E57A1B83",
        "2022-01-01T00:14:59Z, E57A1B83",
        "2022-01-01T00:15:00Z, E57A1B84",
        "2022-01-01T00:29:59Z, E57A1F07",
        "2022-01-01T00:29:59Z, E57A1F07",
        "2022-01-01T00:30:00Z, E57A1F08",
        "2022-02-15T22:43:13Z, E5B6AB01",
        "2035-02-15T22:43:13Z, FE2A3D01"
    )
    @SneakyThrows
    fun can_convert_time32_to_instant(instant: Instant, time32AsHexString: String) {
        val time32 = Hex.decodeHex(time32AsHexString)
        assertThat(robertClock.atTime32(time32).asInstant())
            .isEqualTo(instant.truncatedTo(SECONDS))
    }

    @ParameterizedTest
    @CsvSource(
        "3052-11-14T22:34:59Z,        787C0D13, 3052-11-14T22:34:59Z",
        "4037-11-23T09:22:44.999900Z, B5416EE4, 3052-11-14T22:34:59Z"
    )
    @SneakyThrows
    fun after_2036_time32_to_instant_conversion_doesnt_work(
        inputinstant: Instant,
        time32AsHexString: String,
        outputInstant: Instant
    ) {
        val robertInstant = robertClock.at(inputinstant)
        val instantAsTime32 = robertInstant.asTime32()
        assertThat(instantAsTime32)
            .asHexString()
            .isEqualTo(time32AsHexString)
        val robertInstantFromTime32 = robertClock.atTime32(instantAsTime32)
        assertThat(robertInstantFromTime32.asInstant())
            .isNotEqualTo(outputInstant)
    }

    @Test
    fun can_generate_useful_toString() {
        val instant = Instant.parse("2022-04-23T08:30:00Z")
        assertThat(robertClock.at(instant).toString())
            .isEqualTo("2022-04-23T08:30:00Z=10786E")
    }

    @Test
    fun can_add_time() {
        val instant = Instant.parse("2022-04-23T08:30:00Z")
        val robertInstant = robertClock.at(instant)

        // add 30 minutes
        assertThat(robertInstant.plus(1800000, MILLIS)).hasToString("2022-04-23T09:00:00Z=10788E")
        assertThat(robertInstant.plus(1800, SECONDS)).hasToString("2022-04-23T09:00:00Z=10788E")
        assertThat(robertInstant.plus(30, MINUTES)).hasToString("2022-04-23T09:00:00Z=10788E")
        assertThat(robertInstant.plus(2, ROBERT_EPOCH)).hasToString("2022-04-23T09:00:00Z=10788E")

        // add 2 days
        assertThat(robertInstant.plus(48, HOURS)).hasToString("2022-04-25T08:30:00Z=10978E")
        assertThat(robertInstant.plus(2, DAYS)).hasToString("2022-04-25T08:30:00Z=10978E")
        assertThat(robertInstant.plus(192, ROBERT_EPOCH)).hasToString("2022-04-25T08:30:00Z=10978E")
    }

    @Test
    fun can_subtract_time() {
        val instant = Instant.parse("2022-04-23T08:30:00Z")
        val robertInstant = robertClock.at(instant)

        // subtract 30 minutes
        assertThat(robertInstant.minus(1800000, MILLIS)).hasToString("2022-04-23T08:00:00Z=10784E")
        assertThat(robertInstant.minus(1800, SECONDS)).hasToString("2022-04-23T08:00:00Z=10784E")
        assertThat(robertInstant.minus(30, MINUTES)).hasToString("2022-04-23T08:00:00Z=10784E")
        assertThat(robertInstant.minus(2, ROBERT_EPOCH)).hasToString("2022-04-23T08:00:00Z=10784E")

        // subtract 2 days
        assertThat(robertInstant.minus(48, HOURS)).hasToString("2022-04-21T08:30:00Z=10594E")
        assertThat(robertInstant.minus(2, DAYS)).hasToString("2022-04-21T08:30:00Z=10594E")
        assertThat(robertInstant.minus(192, ROBERT_EPOCH)).hasToString("2022-04-21T08:30:00Z=10594E")
    }

    @Test
    fun can_truncate_to_robert_epoch() {
        val instant = Instant.parse("2022-04-23T08:35:12.004Z")
        val robertInstant = robertClock.at(instant)
        assertThat(robertInstant.truncatedTo(ROBERT_EPOCH)).hasToString("2022-04-23T08:30:00Z=10786E")
    }

    @ParameterizedTest
    @CsvSource(
        "2022-04-23T08:00:00Z=10784E   ,2022-04-23T08:00:00Z       ,10784",
        "2022-01-01T00:00:00Z=0E       ,2022-01-01T00:00:00Z       ,0",
        "2022-01-01T00:14:59Z=0E       ,2022-01-01T00:14:59Z       ,0",
        "2022-01-01T00:14:59.999Z=0E   ,2022-01-01T00:14:59.999Z   ,0",
        "2022-01-01T00:15:00Z=1E       ,2022-01-01T00:15:00Z       ,1",
        "2022-01-01T00:29:59Z=1E       ,2022-01-01T00:29:59Z       ,1",
        "2022-01-01T00:29:59.999999Z=1E,2022-01-01T00:29:59.999999Z,1",
        "2022-01-01T00:30:00Z=2E       ,2022-01-01T00:30:00Z       ,2",
        "2022-02-15T22:43:13Z=4410E    ,2022-02-15T22:43:13Z       ,4410"
    )
    fun can_parse_robert_instant_string(
        robertInstantString: String,
        expectedInstant: String,
        expectedEpochId: Int
    ) {
        val parsedRobertInstant = parseRobertInstant(robertInstantString!!)
        assertThat(parsedRobertInstant.asInstant())
            .hasToString(expectedInstant)
        assertThat(parsedRobertInstant.asEpochId())
            .isEqualTo(expectedEpochId)
    }

    @ParameterizedTest
    @CsvSource(
        "2022-01-01T00:00:00Z,2022-01-01T00:00:00Z,0",
        "2022-01-01T00:00:00Z,2022-01-01T00:14:59.999Z,0",
        "2022-01-01T00:00:00Z,2022-01-01T00:15:00Z,1",
        "2022-02-15T18:43:13Z,2022-02-15T22:43:13Z,16",
        "2022-02-15T18:43:13Z,2022-02-15T22:43:13Z,16",
        "2022-02-15T18:43:13Z,2022-02-15T22:43:13Z,16",
        "2022-02-15T20:43:13Z,2022-02-15T18:13:00Z,-10",
        "1950-01-01T00:00:00Z,1950-01-01T01:00:00Z,4"
    )
    fun can_count_epochs_between_two_instants(begin: Instant, end: Instant, epochsCount: Int) {
        assertThat(begin.until(end, ROBERT_EPOCH))
            .isEqualTo(epochsCount.toLong())
    }

    @Test
    fun can_list_epochs_until() {
        val begin = robertClock.at(Instant.parse("2022-12-18T06:23:43Z"))
        // 1h and 5m later, or 4 epochs later
        val endExclusive = robertClock.at(Instant.parse("2022-12-18T07:28:43Z"))
        assertThat(
            begin.epochsUntil(endExclusive)
                .map(RobertInstant::toString)
                .toList()
        ).containsExactly(
            "2022-12-18T06:15:00Z=33721E",
            "2022-12-18T06:30:00Z=33722E",
            "2022-12-18T06:45:00Z=33723E",
            "2022-12-18T07:00:00Z=33724E",
            "2022-12-18T07:15:00Z=33725E"
        )
    }

    @Test
    fun `list_epochs_until_operation_is "end exclusive"`() {
        val begin = robertClock.at(Instant.parse("2022-12-18T06:23:43Z"))
        val endExclusive = robertClock.at(Instant.parse("2022-12-18T07:30:00Z"))
        assertThat(
            begin.epochsUntil(endExclusive)
                .map(RobertInstant::toString)
                .toList()
        ).containsExactly(
            "2022-12-18T06:15:00Z=33721E",
            "2022-12-18T06:30:00Z=33722E",
            "2022-12-18T06:45:00Z=33723E",
            "2022-12-18T07:00:00Z=33724E",
            "2022-12-18T07:15:00Z=33725E"
        )
    }
}
