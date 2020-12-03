package fr.gouv.tac.systemtest;

import org.junit.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;

import static org.junit.Assert.assertEquals;

public class TimeUtilTest {

    @Test
    public void dateToTimestampTestAtOriginOneMinute() {
        LocalDateTime localDateTime= LocalDateTime.parse("1900-01-01T00:01:00");
        Instant instant = localDateTime.toInstant(ZoneOffset.UTC);
        Date dateOrigin =  Date.from(instant);
        Long test = TimeUtil.dateToTimestamp(dateOrigin);
        assertEquals(60L, (long) test);
    }

    @Test
    public void dateToTimestampTestAtOriginOneHour() {
        LocalDateTime localDateTime= LocalDateTime.parse("1900-01-01T01:00:00");
        Instant instant = localDateTime.toInstant(ZoneOffset.UTC);
        Date dateOrigin =  Date.from(instant);
        Long test = TimeUtil.dateToTimestamp(dateOrigin);
        assertEquals(3600L, (long) test);
    }

    @Test
    public void testRoundTimestampDown() {
        assertEquals(Long.parseLong(Config.getProperty("TIME_ROUNDING")) , (long) TimeUtil.roundTimestamp(Long.parseLong(Config.getProperty("TIME_ROUNDING"))+ 2L ));
    }

    @Test
    public void testRoundTimestampUp() {
        assertEquals(0L , (long) TimeUtil.roundTimestamp(Long.parseLong(Config.getProperty("TIME_ROUNDING"))- 2L ));
    }

    @Test
    public void dateToTimestamp() {
    }

    @Test
    public void roundTimestamp() {
    }

    @Test
    public void naturalLanguageDateStringToNTPTimestampTest() {
        Long today = TimeUtil.naturalLanguageDateStringToNTPTimestamp("Today at noon") ;
                Long yesterday = TimeUtil.naturalLanguageDateStringToNTPTimestamp("Yesterday at noon");
        assertEquals(new Long(24*3600), new Long(today - yesterday) );

    }
}