package fr.gouv.stopc.robertserver.common

import fr.gouv.stopc.robertserver.common.RobertClock.RobertInstant
import java.nio.ByteBuffer
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset.UTC
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit.SECONDS
import java.time.temporal.Temporal
import java.time.temporal.TemporalAmount
import java.time.temporal.TemporalField
import java.time.temporal.TemporalUnit

/**
 * The *Robert epoch* [TemporalUnit]. Can be used to add/subtract epochs from [Instant]s: `Instant.now().plus(2, ROBERT_EPOCH)`.
 */
val ROBERT_EPOCH: TemporalUnit = RobertEpoch()

/**
 * Obtains an instance of `RobertInstant` from a text string such as `2007-12-03T10:15:30.00Z=3598E`.
 *
 * The string must represent a valid instant in UTC concatenated with the epoch and terminating with the character 'E'.
 *
 * @param text the text to parse, not null
 * @return the parsed robert instant, not null
 * @throws DateTimeParseException if the text cannot be parsed
 */
fun parseRobertInstant(text: CharSequence): RobertInstant {
    val match = Regex("([^Z]+Z)=(\\d+)E").find(text)
        ?: throw DateTimeParseException("$text doesn't match RobertInstant pattern", text, 0)
    val (isoDateTime, epochId) = match.destructured
    val instant = Instant.parse(isoDateTime)
    val clockStart = instant.minus(epochId.toLong(), ROBERT_EPOCH)
        .atZone(UTC)
        .toLocalDate()
    return RobertClock(clockStart)
        .at(instant)
}

/**
 * This component aims to help manipulate time and switching from one unit to another in a manner like the java.time API.
 *
 * Robert protocol split time in 15 minutes units called *epochs*, use NTP timestamp at low level (epoch 1900-01-01), sometimes unix timestamps (epoch 1970-01-01).
 *
 * This is a clock component aware of the service start time to help to convert time to Robert *epochs*.
 */
class RobertClock private constructor(private val start: Instant) {

    companion object {

        private val SECONDS_BETWEEN_NTP_EPOCH_AND_UNIX_EPOCH = Duration.between(
            Instant.parse("1900-01-01T00:00:00Z"),
            Instant.parse("1970-01-01T00:00:00Z")
        ).seconds
    }

    constructor(startDate: String) : this(LocalDate.parse(startDate))
    constructor(startDate: LocalDate) : this(startDate.atStartOfDay(UTC).toInstant())

    fun at(time: Instant) = RobertInstant(start, time)

    fun atNtpTimestamp(ntpTimestampSeconds: Long): RobertInstant {
        val unixTimestamp = ntpTimestampSeconds - SECONDS_BETWEEN_NTP_EPOCH_AND_UNIX_EPOCH
        return atUnixTimestamp(unixTimestamp)
    }

    fun atUnixTimestamp(unixTimestampSeconds: Long): RobertInstant {
        val instant = Instant.ofEpochSecond(unixTimestampSeconds)
        return at(instant)
    }

    fun atEpoch(epochId: Int): RobertInstant = RobertInstant(start, start.plus(epochId.toLong(), ROBERT_EPOCH))

    fun atTime32(ntpTimestamp32bitByteArray: ByteArray?): RobertInstant {
        val ntpTimestamp = ByteBuffer.allocate(java.lang.Long.BYTES)
            .position(4)
            .put(ntpTimestamp32bitByteArray)
            .rewind()
            .long
        return atNtpTimestamp(ntpTimestamp)
    }

    fun now() = at(Instant.now())

    /**
     * A wrapper for [Instant] introducing Robert protocol *epoch*.
     * Provides functions to convert time to différents representations:
     *
     *  * [.asInstant]: a regular [Instant]
     *  * [.asNtpTimestamp]: an amount of seconds relative to 1900-01-01
     *
     *  * [.asEpochId]: a number of *robert epoch* units of 15
     * minutes relative to the service start time
     *  * [.asTime32]: a 4 byte array containing the most significant bits
     * of the NTP timestamp
     *
     */
    inner class RobertInstant(
        private val clockStart: Instant,
        private val time: Instant
    ) : Temporal {

        fun asNtpTimestamp() = time.epochSecond + SECONDS_BETWEEN_NTP_EPOCH_AND_UNIX_EPOCH

        fun asUnixTimestamp() = time.epochSecond

        fun asInstant() = time

        fun asEpochId() = Duration.between(clockStart, time)
            .dividedBy(ROBERT_EPOCH.duration).toInt()

        /**
         * Quote from Robert specification :
         *
         * > "16-bit timestamp (to encode the ne-grain emission time). It contains the 16 less significant bits of the
         * > current NTP "Seconds" timestamp of AppA (which represents, for era 0, the number of seconds since 0h
         * > January 1st, 1900 UTC). Since it is truncated to 16 bits, it covers a bit more than 18 hours, what is much
         * > larger than the epoch duration."
         */
        fun as16LessSignificantBits(): Int {
            val less16SignificantBits = asNtpTimestamp() and 0x0000FFFFL
            return less16SignificantBits.toInt()
        }

        fun asTime32(): ByteArray {
            val time32 = ByteArray(4)
            ByteBuffer.allocate(java.lang.Long.BYTES)
                .putLong(asNtpTimestamp())
                .position(4)[time32]
            return time32
        }

        override fun minus(amountToSubtract: Long, unit: TemporalUnit) = at(time.minus(amountToSubtract, unit))

        override fun until(endExclusive: Temporal, unit: TemporalUnit) = unit.between(this, endExclusive)

        override fun plus(amountToAdd: Long, unit: TemporalUnit) = at(time.plus(amountToAdd, unit))

        fun truncatedTo(unit: TemporalUnit?) = at(time.truncatedTo(unit))

        override fun minus(amountToSubtract: TemporalAmount) = at(time.minus(amountToSubtract))

        override fun isSupported(unit: TemporalUnit) = time.isSupported(unit)

        override fun with(field: TemporalField, newValue: Long) = at(time.with(field, newValue))

        override fun plus(amountToAdd: TemporalAmount) = at(time.plus(amountToAdd))

        override fun isSupported(field: TemporalField) = time.isSupported(field)

        override fun getLong(field: TemporalField) = time.getLong(field)

        fun until(otherRobertInstant: RobertInstant): Duration = Duration.between(time, otherRobertInstant.time)

        fun isBefore(otherRobertInstant: RobertInstant) = time.isBefore(otherRobertInstant.time)

        fun isAfter(otherRobertInstant: RobertInstant) = time.isAfter(otherRobertInstant.time)

        fun epochsUntil(endExclusive: RobertInstant): Sequence<RobertInstant> {
            val start = this.truncatedTo(ROBERT_EPOCH)
            val epochsSequence = generateSequence(start) { it.plus(1, ROBERT_EPOCH) }
            return epochsSequence.takeWhile { it.isBefore(endExclusive) }
        }

        override fun toString() = "$time=${asEpochId()}E"
    }
}

/**
 * A [TemporalUnit] representing a Robert *epoch* of 15 minutes.
 */
class RobertEpoch : TemporalUnit {
    override fun getDuration(): Duration = Duration.ofMinutes(15)

    override fun isDurationEstimated() = false

    override fun isDateBased() = true

    override fun isTimeBased() = true

    override fun <R : Temporal> addTo(temporal: R, amount: Long): R =
        temporal.plus(amount * duration.seconds, SECONDS) as R

    override fun between(temporal1Inclusive: Temporal, temporal2Exclusive: Temporal) =
        Duration.between(temporal1Inclusive, temporal2Exclusive)
            .dividedBy(duration)
}
