package fr.gouv.tac.systemtest;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class TimeUtil {
    private static final int TIME_ROUNDING = Integer.parseInt(System.getenv("TIME_ROUNDING"));
    private static final int HOUR = 3600;
    private static final int DAY = 86400;
    private static final int HOURS_PER_DAY = 24;
    private static Long today = 0L;

    public static Long today(){
        LocalDateTime dateTime=LocalDateTime.of(1900,1,1,0,0,0);
        LocalDateTime dateTime2=LocalDateTime.now().truncatedTo(ChronoUnit.DAYS).minusDays(1);
        Long timestamp=java.time.Duration.between(dateTime,dateTime2).getSeconds();
        return timestamp-(timestamp % TIME_ROUNDING);
    }
    public static Long yesterdayAt(int time){
        return today() + (time - HOURS_PER_DAY) * HOUR;
    }
    public static List<Long> everyDayAt(int time){
      List<Long> list = new ArrayList<Long>();
      Long value = TimeUtil.yesterdayAt(time);
      for (int i=0 ; i < 12; i++){
         list.add(value);
         value = value - DAY;
      }
      return list;
    }
}
