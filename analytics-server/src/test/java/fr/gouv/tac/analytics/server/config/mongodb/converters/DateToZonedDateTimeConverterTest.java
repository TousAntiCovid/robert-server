package fr.gouv.tac.analytics.server.config.mongodb.converters;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.GregorianCalendar;

import org.springframework.test.context.junit.jupiter.SpringExtension;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(SpringExtension.class)
public class DateToZonedDateTimeConverterTest {

    private final DateToZonedDateTimeConverter dateToZonedDateTimeConverter = new DateToZonedDateTimeConverter();

    @Test
    public void shouldConvertDateToZonedDateTimeWithUTCTimeZone() {

        final ZonedDateTime zdt = ZonedDateTime.parse("2019-04-01T16:24:11.252+02:00");
        final GregorianCalendar calendar = GregorianCalendar.from(zdt);

        final Date date = calendar.getTime();

        final ZonedDateTime result = dateToZonedDateTimeConverter.convert(date);
        Assertions.assertThat(result.getZone()).isEqualTo(ZoneId.of("UTC").normalized());
        Assertions.assertThat(result).isEqualToIgnoringNanos(zdt);

    }

}