package fr.gouv.tac.analytics.server.config.mongodb.converters;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;

import static java.time.ZonedDateTime.ofInstant;

@ReadingConverter
public class DateToZonedDateTimeConverter implements Converter<Date, ZonedDateTime> {

    @Override
    public ZonedDateTime convert(final Date source) {
        return ofInstant(source.toInstant(), ZoneOffset.UTC);
    }
}
