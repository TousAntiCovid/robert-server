package fr.gouv.tacw.ws.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import fr.gouv.tacw.database.utils.TimeUtils;

public class WStatusV2MockTest {
    
    private TACWarningController controller = new TACWarningController(null, null, null, null, null, null, null);

    @Test
    public void testRandomDateFromLastFiveDaysIsBetweenNowAndFiveDaysLater() {
        IntStream.rangeClosed(1, 100).forEach( i -> {
            Instant instant = Instant.ofEpochSecond( Long.parseLong(controller.randomDateFromLastFiveDays())
                - TimeUtils.SECONDS_FROM_01_01_1900);
            assertThat(instant).isBetween(instant.minus(5, ChronoUnit.DAYS), Instant.now());
        });
    }
}
