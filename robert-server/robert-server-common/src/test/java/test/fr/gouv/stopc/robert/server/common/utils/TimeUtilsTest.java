package test.fr.gouv.stopc.robert.server.common.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import fr.gouv.stopc.robert.server.common.utils.TimeUtils;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.data.Offset;
import org.assertj.core.data.TemporalUnitLessThanOffset;
import org.junit.jupiter.api.Test;

@Slf4j
public class TimeUtilsTest {
    @Test
    public void testCanGetNtpSecondsWhenEpochIsZero() {
        long startTime = System.currentTimeMillis() / 1000 + TimeUtils.SECONDS_FROM_01_01_1900_TO_01_01_1970;
        assertEquals(startTime, TimeUtils.getNtpSeconds(0, startTime));
    }
    
    @Test
    public void testCanGetNtpSeconds() {
        long startTime = System.currentTimeMillis() / 1000 + TimeUtils.SECONDS_FROM_01_01_1900_TO_01_01_1970;
        assertEquals(startTime + 60*60*24*7, TimeUtils.getNtpSeconds(TimeUtils.EPOCHS_PER_DAY * 7, startTime));
    }

    @Test
    void testGetDateFromEpochNowSucceeds() {
        long startTime = System.currentTimeMillis() / 1000 + TimeUtils.SECONDS_FROM_01_01_1900_TO_01_01_1970;
        assertEquals(LocalDate.now(), TimeUtils.getDateFromEpoch(0, startTime));
    }

    @Test
    void testGetDateFromEpochSetSucceeds() {
        assertEquals(LocalDate.of(2020, 5, 26), TimeUtils.getDateFromEpoch(4080, 3795804000L));
    }

    @Test
    void testGetDateFromEpochSetFails() {
        assertNotEquals(LocalDate.of(2020, 5, 26), TimeUtils.getDateFromEpoch(3984, 3795804000L));
    }

    public long getServiceTimeStart() {
        final LocalDateTime ldt = LocalDateTime.of(2020, 6, 1, 00, 00);
        final ZonedDateTime zdt = ldt.atZone(ZoneId.of("UTC"));
        return TimeUtils.convertUnixMillistoNtpSeconds(zdt.toInstant().toEpochMilli());
    }

    @Test
    void testGetDateFromEpochTimezone() {
        for (int i = 0; i < 96 * 2; i++) {
            log.info("{}: {}", i, TimeUtils.getDateFromEpoch(i, getServiceTimeStart()));
            log.info("{}", 1 + (96 + 87 - (i % 96)) % 96);
        }
    }

    @Test
    void testGetDateFromEpochBeforeChange() {
        for (int i = 940; i < 1200; i++) {
            log.info("{}; i={}",  TimeUtils.getDateFromEpoch(i, getServiceTimeStart()), i);
        }
    }

    @Test
    public void testTimestampIsDayTruncated() {
        Instant instant = Instant.parse("1980-04-09T10:15:30.00Z");
        long ntpInstant = instant.getEpochSecond() + TimeUtils.SECONDS_FROM_01_01_1900_TO_01_01_1970;
        
        Instant roundedInstant = this.instantFromTimestamp(TimeUtils.dayTruncatedTimestamp(ntpInstant));
        
        assertThat(roundedInstant).isEqualTo(Instant.parse("1980-04-09T00:00:00.00Z"));
    }
    
    @Test
    public void testTimestampJustBeforeMidnightIsTruncatedToCurrentDay() {
        Instant instant = Instant.parse("2021-01-17T23:59:10.00Z");
        long ntpInstant = instant.getEpochSecond() + TimeUtils.SECONDS_FROM_01_01_1900_TO_01_01_1970;
        
        Instant roundedInstant = this.instantFromTimestamp(TimeUtils.dayTruncatedTimestamp(ntpInstant));
        
        assertThat(roundedInstant).isEqualTo(Instant.parse("2021-01-17T00:00:00.00Z"));
    }
    
    @Test
    public void testTimestampJustAfterMidnightIsTruncatedToCurrentDay() {
        Instant instant = Instant.parse("2021-01-17T00:01:50.00Z");
        long ntpInstant = instant.getEpochSecond() + TimeUtils.SECONDS_FROM_01_01_1900_TO_01_01_1970;
        
        Instant roundedInstant = this.instantFromTimestamp(TimeUtils.dayTruncatedTimestamp(ntpInstant));
        
        assertThat(roundedInstant).isEqualTo(Instant.parse("2021-01-17T00:00:00.00Z"));
    }
    
    @Test
    public void testTimestampAtMidnightIsTruncatedToEndingDay() {
        Instant instant = Instant.parse("2021-01-17T00:00:00.00Z");
        long ntpInstant = instant.getEpochSecond() + TimeUtils.SECONDS_FROM_01_01_1900_TO_01_01_1970;
        
        Instant roundedInstant = this.instantFromTimestamp(TimeUtils.dayTruncatedTimestamp(ntpInstant));
        
        assertThat(roundedInstant).isEqualTo(Instant.parse("2021-01-17T00:00:00.00Z"));
    }
    
    @Test
    public void testRandomizedDateIsBetweenJMinus1AndJPlus1() {
        Instant instant = Instant.parse("2021-02-11T13:00:00.00Z");
        long ntpInstant = instant.getEpochSecond() + TimeUtils.SECONDS_FROM_01_01_1900_TO_01_01_1970;
        
        Set<Instant> randomizedInstants = IntStream.rangeClosed(1, 5000)
            .mapToObj(i -> this.instantFromTimestamp(TimeUtils.randomizedDate(ntpInstant)))
            .collect(Collectors.toSet());
        
        assertThat(randomizedInstants).
            containsExactly(instant.minus(1, ChronoUnit.DAYS), instant, instant.plus(1, ChronoUnit.DAYS));
    }

    @Test
    public void shouldGetRandomizedDateNotInFutureReturnTheCurrentNtpTimestampInCaseProvidedNtpInstantIsInFuture() {

        Instant instantInFuture = Instant.now().plus(5, ChronoUnit.DAYS);
        long ntpInstant = instantInFuture.getEpochSecond() + TimeUtils.SECONDS_FROM_01_01_1900_TO_01_01_1970;

        Set<Instant> randomizedInstants = IntStream.rangeClosed(1, 5000)
                .mapToObj(i -> this.instantFromTimestamp(TimeUtils.getRandomizedDateNotInFuture(ntpInstant)))
                .collect(Collectors.toSet());

        assertThat(randomizedInstants.size()).isEqualTo(1);
        Iterator<Instant> iterator = randomizedInstants.iterator();
        assertThat(iterator.next()).isCloseTo(Instant.now(), new TemporalUnitLessThanOffset(10, ChronoUnit.SECONDS));
    }

    @Test
    public void shouldGetRandomizedDateNotInFutureReturnNtpInstantBetweenJMinus1AndJPlus1FromProvidedNtpInstantIsInPast() {

        Instant instantInPast = Instant.now().minus(10, ChronoUnit.DAYS).with(ChronoField.MILLI_OF_SECOND, 0);
        long ntpInstant = instantInPast.getEpochSecond() + TimeUtils.SECONDS_FROM_01_01_1900_TO_01_01_1970;

        Set<Instant> randomizedInstants = IntStream.rangeClosed(1, 5000)
                .mapToObj(i -> this.instantFromTimestamp(TimeUtils.getRandomizedDateNotInFuture(ntpInstant)))
                .collect(Collectors.toSet());

        assertThat(randomizedInstants).
                containsExactly(instantInPast.minus(1, ChronoUnit.DAYS), instantInPast, instantInPast.plus(1, ChronoUnit.DAYS));
    }


    private Instant instantFromTimestamp(long ntpTimestamp) {
        return Instant.ofEpochSecond(ntpTimestamp - TimeUtils.SECONDS_FROM_01_01_1900_TO_01_01_1970);
    }

    private static int MAX_TEST = 96 * 13;
    @Test
    void testCompareGetDateAndRemaining() {
        LocalDate[] dates = new LocalDate[MAX_TEST];
        int[] remainingEpochsForDay = new int[MAX_TEST];
        for (int i = 0; i < MAX_TEST; i++) {
            dates[i] = TimeUtils.getDateFromEpoch(i, getServiceTimeStart());
        }

        boolean error = false;
        int i = 0;
        while (i < MAX_TEST) {
            int j = i + 1;
            while (j < MAX_TEST && dates[i].isEqual(dates[j])) {
                j++;
            }
            for (int k = i; k < j; k++) {
                remainingEpochsForDay[k] = j - k;
            }
            i = j;
        }
        for (i = 0; i < MAX_TEST; i++) {
            int i1 = remainingEpochsForDay[i];
            int i2 = TimeUtils.remainingEpochsForToday(i);
            if (i1 != i2) {
                error = true;
            }
        }
        assertTrue(!error);
    }

    @Test
    void testConvertUnixMillistoNtpSeconds() {
        long currentUnixMillis = 1613060938008L;
        assertThat(TimeUtils.convertUnixMillistoNtpSeconds(currentUnixMillis)).isEqualTo(3822049738L);
    }

    @Test
    void testConvertNTPSecondsToUnixMillis() {
        long ntpSeconds = 3822049738L;
        assertThat(TimeUtils.convertNTPSecondsToUnixMillis(ntpSeconds)).isEqualTo(1613060938000L);
    }

    @Test
    public void testBidirectionalUnixNtpConversionLoosePrecision() {
        long currentUnixMillis = System.currentTimeMillis();
        assertThat(TimeUtils.convertNTPSecondsToUnixMillis(TimeUtils.convertUnixMillistoNtpSeconds(currentUnixMillis))).isCloseTo(currentUnixMillis, Offset.offset(1000L));
   }

   @Test
    public void shouldGetNumberOfEpochsBetweenReturnOneEpoch() {
        long fromNTPTimestampInSecondsBeginningOfFirstEpoch = 3822049738L;
        long toNTPTimestampInSecondsBeginningOfSecondEpoch = 3822049738L + 900L;
        long toNTPTimestampInSecondsEndOfSecondEpoch = 3822049738L + 1799L;

        assertThat(TimeUtils.getNumberOfEpochsBetween(fromNTPTimestampInSecondsBeginningOfFirstEpoch,
                toNTPTimestampInSecondsBeginningOfSecondEpoch)).isEqualTo(1);

        assertThat(TimeUtils.getNumberOfEpochsBetween(fromNTPTimestampInSecondsBeginningOfFirstEpoch,
                toNTPTimestampInSecondsEndOfSecondEpoch)).isEqualTo(1);

   }

    @Test
    public void shouldGetNumberOfEpochsBetweenReturnTwoEpochs() {
        long fromNTPTimestampInSecondsBeginningOfFirstEpoch = 3822049738L;
        long toNTPTimestampInSecondsBeginningOfThirdEpoch = 3822049738L + 1800L;

        assertThat(TimeUtils.getNumberOfEpochsBetween(fromNTPTimestampInSecondsBeginningOfFirstEpoch,toNTPTimestampInSecondsBeginningOfThirdEpoch)).isEqualTo(2);

    }

    @Test
    public void shouldGetNumberOfEpochsBetweenReturnSameEpoch() {
        long fromNTPTimestampInSeconds = 3822049738L;
        long toNTPTimestampInSeconds = 3822049738L + 899L;

        assertThat(TimeUtils.getNumberOfEpochsBetween(fromNTPTimestampInSeconds,toNTPTimestampInSeconds)).isEqualTo(0);

    }

    @Test
    public void testGetCurrentEpochFrom() {
        long fromNTPTimestampInSeconds = 3822049738L;
        long currentUnixMillis = System.currentTimeMillis();
        long currentNtpSeconds = TimeUtils.convertUnixMillistoNtpSeconds(currentUnixMillis);

        assertThat(TimeUtils.getCurrentEpochFrom(fromNTPTimestampInSeconds)).isEqualTo(TimeUtils.getNumberOfEpochsBetween(fromNTPTimestampInSeconds, currentNtpSeconds));
    }

}
