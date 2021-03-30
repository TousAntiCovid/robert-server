package fr.gouv.tac.systemtest;

import static org.junit.Assert.assertEquals;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.TimeZone;

import org.junit.After;
import org.junit.Test;

public class TimeUtilTest {

    private TimeZone origTimeZone = TimeZone.getDefault();

    @After
    public void resetTimeZone() {
        TimeZone.setDefault(origTimeZone);
    }

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
        //use UTC timezone because on Europe/Paris timezone, there are time changes twice a year leading to the failure of this test.
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        Long today = TimeUtil.naturalLanguageDateStringToNTPTimestamp("Today at noon") ;
        Long yesterday = TimeUtil.naturalLanguageDateStringToNTPTimestamp("Yesterday at noon");
        assertEquals(new Long(24*3600), new Long(today - yesterday) );
    }

    @Test
    public void naturalLanguageDateStringToNTPTimestampTest2() {
        //use UTC timezone because on Europe/Paris timezone, there are time changes twice a year leading to the failure of this test.
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        Long yesterday = TimeUtil.naturalLanguageDateStringToNTPTimestamp("12:30, 2 day ago") ;
        Long today = TimeUtil.naturalLanguageDateStringToNTPTimestamp("Yesterday at 12:30");
        assertEquals(new Long(24*3600), new Long(today - yesterday) );
    }

    @Test
    public void ntpTimestampToStringTest() {
        assertEquals(TimeUtil.ntpTimestampToString(3815897460L), "Wed Dec 02 12:31:00 CET 2020");
    }
}