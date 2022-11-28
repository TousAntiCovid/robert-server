package fr.gouv.stopc.robertserver.test.matchers

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.time.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

class DateTimeMatcherTest() {

    private val UNIX_TIMESTAMP = 1650464700
    private val NTP_TIMESTAMP = 3859453500L
    private val EXPECTED_INSTANT = Instant.ofEpochSecond(UNIX_TIMESTAMP.toLong())

    @Test
    fun can_match_iso_datetime() {
        assertThat("2022-04-20T14:25:00Z", isoDateTimeNear(EXPECTED_INSTANT, Duration.ZERO))
        assertThat("2022-04-20T14:24:00Z", isoDateTimeNear(EXPECTED_INSTANT, 1.minutes))
        assertThat("2022-04-20T14:26:00Z", isoDateTimeNear(EXPECTED_INSTANT, 1.minutes))
    }

    @Test
    fun can_match_unix_timestamp() {
        assertThat("an Integer unix timestamp", UNIX_TIMESTAMP, isUnixTimestamp(EXPECTED_INSTANT))
        assertThat("a Long unix timestamp", UNIX_TIMESTAMP.toLong(), isUnixTimestamp(EXPECTED_INSTANT))
        assertThat("a String unix timestamp", UNIX_TIMESTAMP.toString(), isUnixTimestamp(EXPECTED_INSTANT))
    }

    @Test
    fun can_match_ntp_timestamp() {
        assertThat("a Long ntp timestamp", NTP_TIMESTAMP, isNtpTimestamp(EXPECTED_INSTANT))
        assertThat(
            "a String ntp timestamp",
            NTP_TIMESTAMP.toString(),
            isNtpTimestamp(
                EXPECTED_INSTANT
            )
        )
    }

    @ParameterizedTest
    @ValueSource(strings = ["2022-04-20T14:23:59Z", "2022-04-20T14:26:01Z", "2022-04-20T14:26:00.001Z", "2022-04-22T23:30:00Z"])
    fun can_detect_iso_datetime_mismatch(isoDateTime: String) {
        val error = assertThrows(AssertionError::class.java) {
            assertThat(isoDateTime, isoDateTimeNear(EXPECTED_INSTANT, offset = 1.minutes))
        }
        assertThat(
            error.message?.replace("\r", ""),
            equalTo(
                """
                    
                    Expected: a date-time near <2022-04-20T14:25:00Z> +/- 1m
                         but: was <$isoDateTime>
                """.trimIndent()
            )
        )
    }

    @Test
    fun can_detect_unix_timestamp_mismatch() {
        val error = assertThrows(AssertionError::class.java) {
            assertThat(UNIX_TIMESTAMP, isUnixTimestamp(EXPECTED_INSTANT.plusSeconds(1)))
        }
        assertThat(
            error.message?.replace("\r", ""),
            equalTo(
                """
                    
                    Expected: a timestamp near <2022-04-20T14:25:01Z> +/- 0s
                         but: was <1650464700L> seconds since epoch 1970-01-01T00:00:00Z (was Instant <2022-04-20T14:25:00Z>)
                """.trimIndent()
            )
        )
    }

    @Test
    fun can_detect_ntp_timestamp_mismatch() {
        val error = assertThrows(AssertionError::class.java) {
            assertThat(NTP_TIMESTAMP, isNtpTimestamp(EXPECTED_INSTANT.plusSeconds(1)))
        }
        assertThat(
            error.message?.replace("\r", ""),
            equalTo(
                """
                    
                    Expected: a timestamp near <2022-04-20T14:25:01Z> +/- 0s
                         but: was <3859453500L> seconds since epoch 1900-01-01T00:00:00Z (was Instant <2022-04-20T14:25:00Z>)
                """.trimIndent()
            )
        )
    }
}
