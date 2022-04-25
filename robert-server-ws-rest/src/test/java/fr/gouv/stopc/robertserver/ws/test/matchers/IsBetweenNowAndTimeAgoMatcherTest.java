package fr.gouv.stopc.robertserver.ws.test.matchers;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static java.time.Instant.now;
import static java.time.temporal.ChronoUnit.*;
import static org.hamcrest.Matchers.not;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class IsBetweenNowAndTimeAgoMatcherTest {

    @Test
    public void matches_now_modulo_processing_time() {

        MatcherAssert.assertThat(
                "Given processing time (here 100ms), matcher matches on now",
                now().toString(),
                CustomMatchers.isBetweenNowAndTimeAgo(100, MILLIS)
        );
    }

    @Test
    public void matches_2_days_minus_1_second_ago_with_2_days_window() {

        MatcherAssert.assertThat(
                now().minus(2, DAYS).plusSeconds(1).toString(),
                CustomMatchers.isBetweenNowAndTimeAgo(2, DAYS)
        );
    }

    @Test
    public void does_not_match_2_hours_when_window_with_1_hour() {

        MatcherAssert.assertThat(
                now().minus(2, HOURS).toString(),
                not(CustomMatchers.isBetweenNowAndTimeAgo(1, HOURS))
        );
    }
}
