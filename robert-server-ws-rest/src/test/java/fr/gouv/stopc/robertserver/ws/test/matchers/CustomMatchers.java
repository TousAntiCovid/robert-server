package fr.gouv.stopc.robertserver.ws.test.matchers;

import java.time.temporal.ChronoUnit;

public class CustomMatchers {

    public static IsBetweenNowAndTimeAgoMatcher isBetweenNowAndTimeAgo(final int quantity, final ChronoUnit timeUnit) {
        return new IsBetweenNowAndTimeAgoMatcher(quantity, timeUnit);
    }
}
