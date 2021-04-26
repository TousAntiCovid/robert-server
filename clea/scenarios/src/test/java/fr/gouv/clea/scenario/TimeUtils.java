package fr.gouv.clea.scenario;

import org.ocpsoft.prettytime.nlp.PrettyTimeParser;

import java.time.Instant;
import java.util.Date;

public class TimeUtils {
    public static Date naturalLanguageDateStringToTimestamp(String text){
        return new PrettyTimeParser().parse(text).get(0);
    }
    public static Instant naturalLanguageDateStringToInstant(String text){
        return  naturalLanguageDateStringToTimestamp(text).toInstant();
    }
}
