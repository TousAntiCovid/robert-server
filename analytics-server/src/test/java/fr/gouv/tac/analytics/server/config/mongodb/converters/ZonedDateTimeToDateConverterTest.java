package fr.gouv.tac.analytics.server.config.mongodb.converters;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.ZonedDateTime;
import java.util.Date;

@ExtendWith(SpringExtension.class)
public class ZonedDateTimeToDateConverterTest {

    private final ZonedDateTimeToDateConverter zonedDateTimeToDateConverter = new ZonedDateTimeToDateConverter();

    @Test
    public void shouldMapZoneDateTimeToDate() {

        final ZonedDateTime zdt = ZonedDateTime.parse("2019-04-01T16:24:11.252+02:00");

        final Date result = zonedDateTimeToDateConverter.convert(zdt);

        Assertions.assertThat(result).isEqualTo("2019-04-01T16:24:11.252+0200");

    }
}