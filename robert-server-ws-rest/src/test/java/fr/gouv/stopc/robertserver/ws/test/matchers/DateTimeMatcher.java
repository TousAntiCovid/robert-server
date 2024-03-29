package fr.gouv.stopc.robertserver.ws.test.matchers;

import lombok.RequiredArgsConstructor;
import org.hamcrest.Description;
import org.hamcrest.DiagnosingMatcher;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;

import java.time.Duration;
import java.time.Instant;

public class DateTimeMatcher {

    public static Matcher<String> isoDateTimeNear(final Instant instant, final Duration acceptableDifference) {
        return new IsoDateTimeMatcher(instant, acceptableDifference);
    }

    public static Matcher<Object> isNtpTimestamp(final Instant instant) {
        return new TimestampNear(TimestampNear.NTP_EPOCH, instant, Duration.ZERO);
    }

    public static Matcher<Object> isUnixTimestamp(final Instant instant) {
        return new TimestampNear(TimestampNear.UNIX_EPOCH, instant, Duration.ZERO);
    }

    public static Matcher<Object> isUnixTimestampNear(final Instant instant, final Duration acceptableDifference) {
        return new TimestampNear(TimestampNear.UNIX_EPOCH, instant, acceptableDifference);
    }

    @RequiredArgsConstructor
    private static class IsoDateTimeMatcher extends TypeSafeDiagnosingMatcher<String> {

        private final Instant expectedInstant;

        private final Duration acceptableDifference;

        @Override
        public void describeTo(final Description description) {
            description
                    .appendText("a date-time near ").appendValue(expectedInstant)
                    .appendText(" +/- ").appendText(acceptableDifference.toString());
        }

        @Override
        protected boolean matchesSafely(final String dateTime, final Description mismatchDescription) {
            final var actualInstant = Instant.parse(dateTime);
            final Instant lowerBound = expectedInstant.minus(acceptableDifference);
            final Instant upperBound = expectedInstant.plus(acceptableDifference);
            mismatchDescription
                    .appendText("was ")
                    .appendValue(actualInstant);
            return (actualInstant.equals(lowerBound) || actualInstant.isAfter(lowerBound))
                    && (actualInstant.equals(upperBound) || actualInstant.isBefore(upperBound));
        }
    }

    @RequiredArgsConstructor
    private static class TimestampNear extends DiagnosingMatcher<Object> {

        private static final Instant UNIX_EPOCH = Instant.ofEpochSecond(0);

        private static final Instant NTP_EPOCH = Instant.parse("1900-01-01T00:00:00Z");

        private final Instant epoch;

        private final Instant expectedInstant;

        private final Duration acceptableDifference;

        @Override
        public void describeTo(final Description description) {
            description
                    .appendText("a timestamp near ").appendValue(expectedInstant)
                    .appendText(" +/- ").appendText(acceptableDifference.toString());
        }

        @Override
        protected boolean matches(final Object item, final Description mismatchDescription) {
            final long timestamp;
            if (item instanceof Integer) {
                timestamp = (int) item;
            } else if (item instanceof Long) {
                timestamp = (long) item;
            } else if (item instanceof String) {
                timestamp = Long.parseLong((String) item);
            } else {
                return false;
            }
            final var actualInstant = epoch.plusSeconds(timestamp);
            final var dateTimeMatcher = new IsoDateTimeMatcher(expectedInstant, acceptableDifference);
            mismatchDescription
                    .appendText("was ")
                    .appendValue(timestamp)
                    .appendText(" seconds since epoch ")
                    .appendText(epoch.toString())
                    .appendText(" (");
            dateTimeMatcher.describeMismatch(actualInstant, mismatchDescription);
            mismatchDescription.appendText(")");
            return dateTimeMatcher.matches(actualInstant.toString());
        }
    }
}
