package fr.gouv.stopc.e2e.mobileApplication;

import fr.gouv.stopc.e2e.external.common.utils.TimeUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.Duration;
import java.time.Instant;

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

    @RequiredArgsConstructor
    public static class RobertInstant {

        private final long startNtpTimestamp;

        @Getter
        private final Instant time;

        public long asNtpTimestamp() {
            return time.getEpochSecond() + SECONDS_FROM_01_01_1900_TO_01_01_1970;
        }

        public int getEpochId() {
            final var numberEpochs = (asNtpTimestamp() - startNtpTimestamp) / TimeUtils.EPOCH_DURATION_SECS;
            return (int) numberEpochs;
        }
    }
}
