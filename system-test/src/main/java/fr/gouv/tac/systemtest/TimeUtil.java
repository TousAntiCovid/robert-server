package fr.gouv.tac.systemtest;

import org.ocpsoft.prettytime.nlp.PrettyTimeParser;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;

public class TimeUtil {
    private static final int HOUR = 3600;
    private static final int DAY = 86400;
    private static final int HOURS_PER_DAY = 24;
    private static Long today = 0L;

    public TimeUtil() {
    }

    public static Long dateToTimestamp(Date date){
        LocalDateTime ntpTimeOrigin = LocalDateTime.parse("1900-01-01T00:00:00");
        Instant ntpInstant =  ntpTimeOrigin.toInstant(ZoneOffset.UTC);
        Date ntpDateOrigin =  Date.from(ntpInstant);

        return new Long(Math.abs(date.getTime()-ntpDateOrigin.getTime())/1000);
    }

    public static String ntpTimestampToString(Long ntpTimestamp){
        LocalDateTime ntpTimeOrigin = LocalDateTime.parse("1900-01-01T00:00:00");
        Instant ntpInstant =  ntpTimeOrigin.toInstant(ZoneOffset.UTC);
        Date ntpDateOrigin =  Date.from(ntpInstant);

        Long timestamp = ntpTimestamp*1000 + ntpDateOrigin.getTime();
        Timestamp ts = new Timestamp(timestamp);
        return new Date(ts.getTime()).toString();
    }

    public static Long roundTimestamp(Long timestamp) {
        return timestamp - (timestamp % Long.parseLong(Config.getProperty("TIME_ROUNDING")));
    }

    public static Date naturalLanguageDateStringToTimestamp(String text){
        return new PrettyTimeParser().parse(text).get(0);
    }
    public static Long naturalLanguageDateStringToNTPTimestamp(String text){
        return dateToTimestamp(naturalLanguageDateStringToTimestamp(text));
    }
}
