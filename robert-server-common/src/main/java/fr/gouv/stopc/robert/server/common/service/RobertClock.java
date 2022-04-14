package fr.gouv.stopc.robert.server.common.service;

import fr.gouv.stopc.robert.server.common.utils.ByteUtils;
import lombok.EqualsAndHashCode;
import lombok.Getter;
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

@Component
@RequiredArgsConstructor
public class RobertClock {

    @Getter
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

    public RobertInstant now() {
        return at(Instant.now());
    }

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
            final var unixTimestampByteArray = ByteUtils.longToBytes(asNtpTimestamp());
            return Arrays.copyOfRange(unixTimestampByteArray, 4, 8);
        }

        public RobertInstant plusEpochs(int numberOfEpochs) {
            return RobertClock.this.at(time.plusSeconds((long) numberOfEpochs * EPOCH_DURATION_SECS));
        }

        public RobertInstant minusEpochs(int numberOfEpochs) {
            return plusEpochs(-numberOfEpochs);
        }

        public RobertInstant minus(final long amountToSubtract, final TemporalUnit unit) {
            return RobertClock.this.at(time.minus(amountToSubtract, unit));
        }

        public RobertInstant plus(final long amountToSubtract, final TemporalUnit unit) {
            return RobertClock.this.at(time.plus(amountToSubtract, unit));
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

    public final static TemporalUnit EPOCH = new Epoch();

    private static class Epoch implements TemporalUnit {

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
            final var result = (R) temporal.plus(amount, this);
            return result;
        }

        @Override
        public long between(Temporal temporal1Inclusive, Temporal temporal2Exclusive) {
            return temporal1Inclusive.until(temporal2Exclusive, this);
        }
    }
}
