package fr.gouv.stopc.robertserver.test.matchers

import org.hamcrest.Description
import org.hamcrest.DiagnosingMatcher
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeDiagnosingMatcher
import java.time.Instant
import java.util.Date
import kotlin.time.Duration
import kotlin.time.toJavaDuration

private val UNIX_EPOCH = Instant.ofEpochSecond(0)
private val NTP_EPOCH = Instant.parse("1900-01-01T00:00:00Z")

fun isoDateTimeNear(instant: Instant, offset: Duration = Duration.ZERO): Matcher<String> {
    return IsoDateTimeMatcher(instant, offset)
}

fun isNtpTimestamp(instant: Instant, offset: Duration = Duration.ZERO): Matcher<Any?> {
    return TimestampNear(NTP_EPOCH, instant, offset)
}

fun isUnixTimestamp(instant: Instant, offset: Duration = Duration.ZERO): Matcher<Any?> {
    return TimestampNear(UNIX_EPOCH, instant, offset)
}

class IsoDateTimeMatcher internal constructor(
    private val expectedInstant: Instant,
    private val acceptableDifference: Duration
) : TypeSafeDiagnosingMatcher<String>() {

    override fun describeTo(description: Description) {
        description
            .appendText("a date-time near ").appendValue(expectedInstant)
            .appendText(" +/- ").appendText(acceptableDifference.toString())
    }

    override fun matchesSafely(dateTime: String, mismatchDescription: Description): Boolean {
        val actualInstant = Instant.parse(dateTime)
        val lowerBound = expectedInstant.minus(acceptableDifference.toJavaDuration())
        val upperBound = expectedInstant.plus(acceptableDifference.toJavaDuration())
        mismatchDescription
            .appendText("was ")
            .appendValue(actualInstant)
        return (
            (actualInstant == lowerBound || actualInstant.isAfter(lowerBound)) &&
                (actualInstant == upperBound || actualInstant.isBefore(upperBound))
            )
    }
}

class TimestampNear internal constructor(
    private val epoch: Instant,
    private val expectedInstant: Instant,
    private val acceptableDifference: Duration
) : DiagnosingMatcher<Any?>() {

    override fun describeTo(description: Description) {
        description
            .appendText("a timestamp near ").appendValue(expectedInstant)
            .appendText(" +/- ").appendText(acceptableDifference.toString())
    }

    override fun matches(item: Any?, mismatchDescription: Description): Boolean {
        val timestamp = when (item) {
            is Int -> item.toLong()
            is Long -> item
            is String -> item.toLong()
            is Date -> item.toInstant().epochSecond
            else -> {
                mismatchDescription.appendText("'$item' can't be interpreted as a time information")
                return false
            }
        }
        val actualInstant = epoch.plusSeconds(timestamp)
        val dateTimeMatcher = IsoDateTimeMatcher(expectedInstant, acceptableDifference)
        mismatchDescription
            .appendText("was ")
            .appendValue(timestamp)
            .appendText(" seconds since epoch ")
            .appendText(epoch.toString())
            .appendText(" (")
        dateTimeMatcher.describeMismatch(actualInstant, mismatchDescription)
        mismatchDescription.appendText(")")
        return dateTimeMatcher.matches(actualInstant.toString())
    }
}
