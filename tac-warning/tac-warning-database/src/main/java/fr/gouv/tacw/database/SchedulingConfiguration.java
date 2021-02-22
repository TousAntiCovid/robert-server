package fr.gouv.tacw.database;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@ConditionalOnProperty(value = "tacw.database.scheduling_enabled", havingValue = "true")
@Configuration
@EnableScheduling
public class SchedulingConfiguration {
}
