package fr.gouv.stopc.e2e.mobileapplication;

import fr.gouv.stopc.e2e.external.common.utils.TimeUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.TemporalUnit;

@RequiredArgsConstructor
public class EpochClock {

    private final static long SECONDS_FROM_01_01_1900_TO_01_01_1970 = Duration.between(
            Instant.parse("1900-01-01T00:00:00Z"),
            Instant.parse("1970-01-01T00:00:00Z")
    ).getSeconds();

    private final long startNtpTimestamp;

    public RobertInstant at(Instant time) {
        return new RobertInstant(startNtpTimestamp, time);
    }

    public RobertInstant now() {
        return at(Instant.now());
    }

    public RobertInstant atNtpTimestamp(long ntpTimestamp) {
        final var unixTimestampMillis = TimeUtils.convertNTPSecondsToUnixMillis(ntpTimestamp);
        final var instant = Instant.ofEpochMilli(unixTimestampMillis);
        return at(instant);
    }

    public RobertInstant atEpoch(int epochId) {
        final var ntpTimestamp = startNtpTimestamp + epochId + TimeUtils.EPOCH_DURATION_SECS;
        return atNtpTimestamp(ntpTimestamp);
    }

    @RequiredArgsConstructor
    public class RobertInstant {

        private final long startNtpTimestamp;

        @Getter
        private final Instant time;

        public long asNtpTimestamp() {
            return time.getEpochSecond() + SECONDS_FROM_01_01_1900_TO_01_01_1970;
        }

        public int asEpochId() {
            final var numberEpochs = (asNtpTimestamp() - startNtpTimestamp) / TimeUtils.EPOCH_DURATION_SECS;
            return (int) numberEpochs;
        }

        public RobertInstant plusEpochs(int numberOfEpochs) {
            return EpochClock.this.at(time.plusSeconds((long) numberOfEpochs * TimeUtils.EPOCH_DURATION_SECS));
        }

        public RobertInstant minus(final long amountToSubtract, final TemporalUnit unit) {
            return EpochClock.this.at(time.minus(amountToSubtract, unit));
        }
    }
}
