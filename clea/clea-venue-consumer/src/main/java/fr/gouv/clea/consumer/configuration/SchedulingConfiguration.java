package fr.gouv.clea.consumer.configuration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@ConditionalOnProperty(value = "clea.conf.scheduling.purge.enabled", havingValue = "true")
@Configuration
@EnableScheduling
public class SchedulingConfiguration {
}
