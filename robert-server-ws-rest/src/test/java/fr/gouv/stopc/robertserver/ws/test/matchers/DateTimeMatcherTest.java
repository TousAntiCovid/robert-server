package fr.gouv.stopc.robertserver.ws.test.matchers;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static fr.gouv.stopc.robertserver.ws.test.matchers.DateTimeMatcher.isNtpTimestamp;
import static fr.gouv.stopc.robertserver.ws.test.matchers.DateTimeMatcher.isUnixTimestamp;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DateTimeMatcherTest {

    static final int UNIX_TIMESTAMP = 1650464700;

    static final long NTP_TIMESTAMP = 3859453500L;

    static final Instant EXPECTED_INSTANT = Instant.ofEpochSecond(UNIX_TIMESTAMP);

    @Test
    void can_match_unix_timestamp() {
        assertThat("an Integer unix timestamp", UNIX_TIMESTAMP, isUnixTimestamp(EXPECTED_INSTANT));
        assertThat("a Long unix timestamp", (long) UNIX_TIMESTAMP, isUnixTimestamp(EXPECTED_INSTANT));
        assertThat("a String unix timestamp", String.valueOf(UNIX_TIMESTAMP), isUnixTimestamp(EXPECTED_INSTANT));
    }

    @Test
    void can_match_ntp_timestamp() {
        assertThat("a Long ntp timestamp", NTP_TIMESTAMP, isNtpTimestamp(EXPECTED_INSTANT));
        assertThat("a String ntp timestamp", String.valueOf(NTP_TIMESTAMP), isNtpTimestamp(EXPECTED_INSTANT));
    }

    @Test
    void can_detect_unix_timestamp_mismatch() {
        final var error = assertThrows(
                AssertionError.class, () -> assertThat(UNIX_TIMESTAMP, isUnixTimestamp(EXPECTED_INSTANT.plusSeconds(1)))
        );
        assertThat(
                error.getMessage().replace("\r", ""), equalTo(
                        "\n" +
                                "Expected: a timestamp near <2022-04-20T14:25:01Z> +/- PT0S\n" +
                                "     but: was <1650464700L> seconds since epoch 1970-01-01T00:00:00Z (or 2022-04-20T14:25:00Z)"
                )
        );
    }

    @Test
    void can_detect_ntp_timestamp_mismatch() {
        final var error = assertThrows(
                AssertionError.class, () -> assertThat(NTP_TIMESTAMP, isNtpTimestamp(EXPECTED_INSTANT.plusSeconds(1)))
        );
        assertThat(
                error.getMessage().replace("\r", ""), equalTo(
                        "\n" +
                                "Expected: a timestamp near <2022-04-20T14:25:01Z> +/- PT0S\n" +
                                "     but: was <3859453500L> seconds since epoch 1900-01-01T00:00:00Z (or 2022-04-20T14:25:00Z)"
                )
        );
    }
}
