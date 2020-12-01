package fr.gouv.tac.systemtest;

import org.ocpsoft.prettytime.nlp.PrettyTimeParser;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;

public class TimeUtil {
    static final int TIME_ROUNDING = 900;
    private static final int HOUR = 3600;
    private static final int DAY = 86400;
    private static final int HOURS_PER_DAY = 24;
    private static Long today = 0L;


    public static Long dateToTimestamp(Date date){
        LocalDateTime localDateOrigin = LocalDateTime.parse("1900-01-01T00:00:00");
        Instant instant =  localDateOrigin.toInstant(ZoneOffset.UTC);
        Date dateOrigin =  Date.from(instant);
        return Math.abs(date.getTime()-dateOrigin.getTime())/1000;
    }

    public static Long roundTimestamp(Long timestamp) {
        return timestamp - (timestamp % TIME_ROUNDING);
    }

    public static Long naturalLanguageDateStringToTimestamp(String text){
        PrettyTimeParser ptp = new PrettyTimeParser();
        return 0L;
    }

}
