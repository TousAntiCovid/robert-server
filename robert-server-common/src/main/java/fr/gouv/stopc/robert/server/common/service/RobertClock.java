package fr.gouv.stopc.robert.server.common.service;

import fr.gouv.stopc.robert.server.common.utils.ByteUtils;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAmount;
import java.time.temporal.TemporalUnit;
import java.util.Arrays;

import static fr.gouv.stopc.robert.server.common.utils.TimeUtils.EPOCH_DURATION_SECS;
import static fr.gouv.stopc.robert.server.common.utils.TimeUtils.SECONDS_FROM_01_01_1900_TO_01_01_1970;
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
@Component
@RequiredArgsConstructor
public class RobertClock {

    private final long startNtpTimestamp;

    @Autowired
    public RobertClock(final IServerConfigurationService config) {
        this(config.getServiceTimeStart());
    }

    public RobertClock(final String startDate) {
        this(Instant.parse(startDate + "T00:00:00Z").getEpochSecond() + SECONDS_FROM_01_01_1900_TO_01_01_1970);
    }

    public RobertInstant at(Instant time) {
        return new RobertInstant(startNtpTimestamp, time);
    }

    public RobertInstant atNtpTimestamp(long ntpTimestamp) {
        final var unixTimestampSeconds = ntpTimestamp - SECONDS_FROM_01_01_1900_TO_01_01_1970;
        final var instant = Instant.ofEpochSecond(unixTimestampSeconds);
        return at(instant);
    }

    public RobertInstant atEpoch(int epochId) {
        final var ntpTimestamp = startNtpTimestamp + (long) epochId * EPOCH_DURATION_SECS;
        return atNtpTimestamp(ntpTimestamp);
    }

    public RobertInstant atTime32(byte[] ntpTimestamp32bitByteArray) {
        final var ntpTimestamp64bitByteArray = ByteUtils.addAll(new byte[] { 0, 0, 0, 0 }, ntpTimestamp32bitByteArray);
        final var ntpTimestamp = ByteUtils.bytesToLong(ntpTimestamp64bitByteArray);
        return atNtpTimestamp(ntpTimestamp);
    }

    public RobertInstant now() {
        return at(Instant.now());
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
    public class RobertInstant {

        private final long startNtpTimestamp;

        private final Instant time;

        public long asNtpTimestamp() {
            return time.getEpochSecond() + SECONDS_FROM_01_01_1900_TO_01_01_1970;
        }

        public Instant asInstant() {
            return time;
        }

        public int asEpochId() {
            final var numberEpochs = (asNtpTimestamp() - startNtpTimestamp) / EPOCH_DURATION_SECS;
            return (int) numberEpochs;
        }

        public byte[] asTime32() {
            final var ntpTimestamp32bitByteArray = ByteUtils.longToBytes(asNtpTimestamp());
            return Arrays.copyOfRange(ntpTimestamp32bitByteArray, 4, 8);
        }

        public RobertInstant minus(final long amountToSubtract, final TemporalUnit unit) {
            return RobertClock.this.at(time.minus(amountToSubtract, unit));
        }

        public RobertInstant plus(final long amountToAdd, final TemporalUnit unit) {
            return RobertClock.this.at(time.plus(amountToAdd, unit));
        }

        public RobertInstant truncatedTo(final TemporalUnit unit) {
            return RobertClock.this.at(time.truncatedTo(unit));
        }

        public RobertInstant minus(final TemporalAmount amountToSubtract) {
            return RobertClock.this.at(time.minus(amountToSubtract));
        }

        public RobertInstant plus(final TemporalAmount amountToAdd) {
            return RobertClock.this.at(time.plus(amountToAdd));
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

        @Override
        public String toString() {
            return String.format("%s=%sE", time.toString(), asEpochId());
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
        public <R extends Temporal> R addTo(R temporal, long amount) {
            @SuppressWarnings("unchecked")
            final var result = (R) temporal.plus(amount * getDuration().getSeconds(), SECONDS);
            return result;
        }

        @Override
        public long between(Temporal temporal1Inclusive, Temporal temporal2Exclusive) {
            return temporal1Inclusive.until(temporal2Exclusive, this);
        }
    }
}
