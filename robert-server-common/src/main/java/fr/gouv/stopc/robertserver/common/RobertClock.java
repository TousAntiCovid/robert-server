package fr.gouv.stopc.robertserver.common;

import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAmount;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalUnit;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.time.ZoneOffset.UTC;
import static java.time.temporal.ChronoUnit.SECONDS;

/**
 * This component aims to help manipulate time and switching from one unit to
 * another in a manner like the java.time API.
 * <p>
 * Robert protocol split time in 15 minutes units called <em>epochs</em>, use
 * NTP timestamp at low level (epoch 1900-01-01), sometimes unix timestamps
 * (epoch 1970-01-01).
 * <p>
 * This is a clock component aware of the service start time to help to convert
 * time to Robert <em>epochs</em>.
 */
public class RobertClock {

    private final static long SECONDS_BETWEEN_NTP_EPOCH_AND_UNIX_EPOCH;

    static {
        final var unixEpoch = Instant.parse("1970-01-01T00:00:00Z");
        final var ntpEpoch = Instant.parse("1900-01-01T00:00:00Z");
        SECONDS_BETWEEN_NTP_EPOCH_AND_UNIX_EPOCH = Duration.between(ntpEpoch, unixEpoch).getSeconds();
    }

    private final Instant start;

    public RobertClock(final String startDate) {
        this(LocalDate.parse(startDate));
    }

    public RobertClock(final LocalDate startDate) {
        this(startDate.atStartOfDay(UTC).toInstant());
    }

    private RobertClock(final Instant startInstant) {
        this.start = startInstant;
    }

    public RobertInstant at(final Instant time) {
        return new RobertInstant(start, time);
    }

    public RobertInstant atNtpTimestamp(final long ntpTimestampSeconds) {
        final var unixTimestamp = ntpTimestampSeconds - SECONDS_BETWEEN_NTP_EPOCH_AND_UNIX_EPOCH;
        return atUnixTimestamp(unixTimestamp);
    }

    public RobertInstant atUnixTimestamp(final long unixTimestampSeconds) {
        final var instant = Instant.ofEpochSecond(unixTimestampSeconds);
        return at(instant);
    }

    public RobertInstant atEpoch(final int epochId) {
        return new RobertInstant(start, start.plus(epochId, ROBERT_EPOCH));
    }

    public RobertInstant atTime32(final byte[] ntpTimestamp32bitByteArray) {
        final var ntpTimestamp = ByteBuffer.allocate(Long.BYTES)
                .position(4)
                .put(ntpTimestamp32bitByteArray)
                .rewind()
                .getLong();
        return atNtpTimestamp(ntpTimestamp);
    }

    public RobertInstant now() {
        return at(Instant.now());
    }

    /**
     * Obtains an instance of {@code RobertInstant} from a text string such as
     * {@code 2007-12-03T10:15:30.00Z=3598E}.
     * <p>
     * The string must represent a valid instant in UTC concatenated with the epoch
     * and terminating with the character 'E'.
     *
     * @param text the text to parse, not null
     * @return the parsed robert instant, not null
     * @throws DateTimeParseException if the text cannot be parsed
     */
    public static RobertInstant parse(final CharSequence text) {
        final var matcher = Pattern.compile("(?<instant>[^Z]+Z)=(?<epochId>\\d+)E")
                .matcher(text);
        if (!matcher.find()) {
            throw new DateTimeParseException(format("%s doesn't match RobertInstant pattern", text), text, 0);
        }
        final var instant = Instant.parse(matcher.group("instant"));
        final var epochId = Integer.parseInt(matcher.group("epochId"));
        final var clockStart = instant.minus(epochId, ROBERT_EPOCH);
        return new RobertClock(clockStart)
                .at(instant);
    }

    /**
     * A wrapper for {@link Instant} introducing Robert protocol <em>epoch</em>.
     * Provides functions to convert time to différents representations:
     * <ul>
     * <li>{@link #asInstant()}: a regular {@link Instant}</li>
     * <li>{@link #asNtpTimestamp()}: an amount of seconds relative to 1900-01-01
     * </li>
     * <li>{@link #asEpochId()}: a number of <em>robert epoch</em> units of 15
     * minutes relative to the service start time</li>
     * <li>{@link #asTime32()}: a 4 byte array containing the most significant bits
     * of the NTP timestamp</li>
     * </ul>
     */
    @RequiredArgsConstructor
    @EqualsAndHashCode
    public class RobertInstant implements Temporal {

        private final Instant clockStart;

        private final Instant time;

        public long asNtpTimestamp() {
            return time.getEpochSecond() + SECONDS_BETWEEN_NTP_EPOCH_AND_UNIX_EPOCH;
        }

        public long asUnixTimestamp() {
            return time.getEpochSecond();
        }

        public Instant asInstant() {
            return time;
        }

        public int asEpochId() {
            return (int) Duration.between(clockStart, time)
                    .dividedBy(ROBERT_EPOCH.getDuration());
        }

        /**
         * Quote from Robert specification :
         * 
         * <pre>
         * "16-bit timestamp (to encode the ne-grain emission time). It contains the 16 less significant bits of the
         * current NTP "Seconds" timestamp of AppA (which represents, for era 0, the number of seconds since 0h
         * January 1st, 1900 UTC). Since it is truncated to 16 bits, it covers a bit more than 18 hours, what is much
         * larger than the epoch duration."
         * </pre>
         */
        public int as16LessSignificantBits() {
            final var less16SignificantBits = (asNtpTimestamp() & 0x0000FFFF);
            return (int) less16SignificantBits;
        }

        public byte[] asTime32() {
            final var time32 = new byte[4];
            ByteBuffer.allocate(Long.BYTES)
                    .putLong(asNtpTimestamp())
                    .position(4)
                    .get(time32);
            return time32;
        }

        @Override
        public RobertInstant minus(final long amountToSubtract, final TemporalUnit unit) {
            return RobertClock.this.at(time.minus(amountToSubtract, unit));
        }

        @Override
        public long until(final Temporal endExclusive, final TemporalUnit unit) {
            return unit.between(this, endExclusive);
        }

        @Override
        public RobertInstant plus(final long amountToAdd, final TemporalUnit unit) {
            return RobertClock.this.at(time.plus(amountToAdd, unit));
        }

        public RobertInstant truncatedTo(final TemporalUnit unit) {
            return RobertClock.this.at(time.truncatedTo(unit));
        }

        @Override
        public RobertInstant minus(final TemporalAmount amountToSubtract) {
            return RobertClock.this.at(time.minus(amountToSubtract));
        }

        @Override
        public boolean isSupported(final TemporalUnit unit) {
            return time.isSupported(unit);
        }

        @Override
        public Temporal with(final TemporalField field, final long newValue) {
            return RobertClock.this.at(time.with(field, newValue));
        }

        @Override
        public RobertInstant plus(final TemporalAmount amountToAdd) {
            return RobertClock.this.at(time.plus(amountToAdd));
        }

        @Override
        public boolean isSupported(final TemporalField field) {
            return time.isSupported(field);
        }

        @Override
        public long getLong(final TemporalField field) {
            return time.getLong(field);
        }

        public Duration until(final RobertInstant otherRobertInstant) {
            return Duration.between(time, otherRobertInstant.time);
        }

        public boolean isBefore(final RobertInstant otherRobertInstant) {
            return time.isBefore(otherRobertInstant.time);
        }

        public boolean isAfter(final RobertInstant otherRobertInstant) {
            return time.isAfter(otherRobertInstant.time);
        }

        public Stream<RobertInstant> epochsUntil(final RobertInstant endExclusive) {
            return Stream.iterate(
                    this.truncatedTo(ROBERT_EPOCH),
                    instant -> instant.isBefore(endExclusive),
                    instant -> instant.plus(1, ROBERT_EPOCH)
            );
        }

        @Override
        public String toString() {
            return format("%s=%sE", time.toString(), asEpochId());
        }
    }

    /**
     * The <em>Robert epoch</em> {@link TemporalUnit}. Can be used to add/subtract
     * epochs from {@link Instant}s:
     * <code>Instant.now().plus(2, ROBERT_EPOCH)</code>.
     */
    public final static TemporalUnit ROBERT_EPOCH = new RobertEpoch();

    /**
     * A {@link TemporalUnit} representing a Robert <em>epoch</em> of 15 minutes.
     */
    private static class RobertEpoch implements TemporalUnit {

        @Override
        public Duration getDuration() {
            return Duration.ofMinutes(15);
        }

        @Override
        public boolean isDurationEstimated() {
            return false;
        }

        @Override
        public boolean isDateBased() {
            return true;
        }

        @Override
        public boolean isTimeBased() {
            return true;
        }

        @Override
        public <R extends Temporal> R addTo(final R temporal, final long amount) {
            final var result = (R) temporal.plus(
                    amount * getDuration().getSeconds(),
                    SECONDS
            );
            return result;
        }

        @Override
        public long between(final Temporal temporal1Inclusive, final Temporal temporal2Exclusive) {
            return Duration.between(temporal1Inclusive, temporal2Exclusive)
                    .dividedBy(getDuration());
        }
    }
}
