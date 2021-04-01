package fr.gouv.tac.analytics.server.config.mongodb;

import fr.gouv.tac.analytics.server.config.mongodb.converters.DateToZonedDateTimeConverter;
import fr.gouv.tac.analytics.server.config.mongodb.converters.ZonedDateTimeToDateConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.core.mapping.event.ValidatingMongoEventListener;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.Arrays;


@Configuration
public class MongoConfiguration {

    @Bean
    public MongoCustomConversions customConversions() {
        return new MongoCustomConversions(
                Arrays.asList(
                        new DateToZonedDateTimeConverter(),
                        new ZonedDateTimeToDateConverter())
        );
    }

    @Bean
    public ValidatingMongoEventListener validatingMongoEventListener(final LocalValidatorFactoryBean localValidatorFactoryBean) {
        return new ValidatingMongoEventListener(localValidatorFactoryBean);
    }


}
