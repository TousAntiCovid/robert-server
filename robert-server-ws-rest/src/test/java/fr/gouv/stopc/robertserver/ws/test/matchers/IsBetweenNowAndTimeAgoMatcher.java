package fr.gouv.stopc.robertserver.ws.test.matchers;

import lombok.AllArgsConstructor;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import static java.time.ZonedDateTime.now;
import static org.exparity.hamcrest.date.ZonedDateTimeMatchers.sameOrAfter;
import static org.exparity.hamcrest.date.ZonedDateTimeMatchers.sameOrBefore;
import static org.hamcrest.Matchers.allOf;

@AllArgsConstructor
public class IsBetweenNowAndTimeAgoMatcher extends TypeSafeDiagnosingMatcher<String> {

    private final int quantity;

    private final ChronoUnit timeUnit;

    @Override
    public void describeTo(final Description description) {
        isBetweenNowAndTimeAgo().describeTo(description);
    }

    @Override
    protected boolean matchesSafely(final String s, final Description description) {
        return isBetweenNowAndTimeAgo().matches(ZonedDateTime.parse(s));
    }

    private Matcher<ZonedDateTime> isBetweenNowAndTimeAgo() {
        final var now = now();
        return allOf(
                sameOrAfter(now.minus(quantity, timeUnit)),
                sameOrBefore(now)
        );
    }
}
