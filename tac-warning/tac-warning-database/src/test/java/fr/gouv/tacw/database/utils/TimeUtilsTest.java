package fr.gouv.tacw.database.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.Test;

public class TimeUtilsTest {
    @Test
    public void testTimestampIsRoundedDownToTheNextLongWhenModuloLowerThanHalfTimeRoundingValue() {
        assertThat(TimeUtils.roundedTimestamp(1606132850)).isEqualTo(1606132800);
    }

    @Test
    public void testTimestampIsRoundedUpToTheNextLongWhenModuloGreaterThanHalfTimeRoundingValue() {
        assertThat(TimeUtils.roundedTimestamp(1606136000)).isEqualTo(1606136400);
    }

    @Test
    public void testTimestampIsNotRoundedWhenMultipleOfTimeRoundingValue() {
        assertThat(TimeUtils.roundedTimestamp(1606140000)).isEqualTo(1606140000);
    }

    @Test
    public void testTimestampIsRoundedUpToTheNextLongWhenModuloIsEqualToHalfTimeRoundingValue() {
        assertThat(TimeUtils.roundedTimestamp(1606140450)).isEqualTo(1606140900);
    }
    
    @Test
    public void testTimestampIsDayTruncated() {
        Instant instant = Instant.parse("1980-04-09T10:15:30.00Z");
        long ntpInstant = instant.getEpochSecond() + TimeUtils.SECONDS_FROM_01_01_1900;
        
        Instant roundedInstant = this.instantFromTimestamp(TimeUtils.dayTruncatedTimestamp(ntpInstant));
        
        assertThat(roundedInstant).isEqualTo(Instant.parse("1980-04-09T00:00:00.00Z"));
    }
    
    @Test
    public void testTimestampJustBeforeMidnightIsTruncatedToCurrentDay() {
        Instant instant = Instant.parse("2021-01-17T23:59:10.00Z");
        long ntpInstant = instant.getEpochSecond() + TimeUtils.SECONDS_FROM_01_01_1900;
        
        Instant roundedInstant = this.instantFromTimestamp(TimeUtils.dayTruncatedTimestamp(ntpInstant));
        
        assertThat(roundedInstant).isEqualTo(Instant.parse("2021-01-17T00:00:00.00Z"));
    }
    
    @Test
    public void testTimestampJustAfterMidnightIsTruncatedToCurrentDay() {
        Instant instant = Instant.parse("2021-01-17T00:01:50.00Z");
        long ntpInstant = instant.getEpochSecond() + TimeUtils.SECONDS_FROM_01_01_1900;
        
        Instant roundedInstant = this.instantFromTimestamp(TimeUtils.dayTruncatedTimestamp(ntpInstant));
        
        assertThat(roundedInstant).isEqualTo(Instant.parse("2021-01-17T00:00:00.00Z"));
    }
    
    @Test
    public void testTimestampAtMidnightIsTruncatedToEndingDay() {
        Instant instant = Instant.parse("2021-01-17T00:00:00.00Z");
        long ntpInstant = instant.getEpochSecond() + TimeUtils.SECONDS_FROM_01_01_1900;
        
        Instant roundedInstant = this.instantFromTimestamp(TimeUtils.dayTruncatedTimestamp(ntpInstant));
        
        assertThat(roundedInstant).isEqualTo(Instant.parse("2021-01-17T00:00:00.00Z"));
    }
    
    private Instant instantFromTimestamp(long ntpTimestamp) {
        return Instant.ofEpochSecond(ntpTimestamp - TimeUtils.SECONDS_FROM_01_01_1900);
    }
}
