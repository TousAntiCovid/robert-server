package fr.gouv.tac.systemtest;

import org.junit.Test;
import org.ocpsoft.prettytime.nlp.PrettyTimeParser;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;

public class WhoWhereWhenHowTest {

    @Test
    public void setWhen() {
        PrettyTimeParser ptp = new PrettyTimeParser();
        List<Date> dates = ptp.parse("yesterday at 12:15");
        String test = dates.get(0).toString();
        Date date = dates.get(0);
        LocalDate localDate = LocalDate.parse("1900-01-01");
        LocalDateTime localDateTime = localDate.atStartOfDay();
        Instant instant = localDateTime.toInstant(ZoneOffset.UTC);
        Date dateOriging =  Date.from(instant);
        Long timestamp = Math.abs(date.getTime()-dateOriging.getTime())/1000;
        System.out.println(test);
        System.out.println(Long.toString(timestamp));
    }
}