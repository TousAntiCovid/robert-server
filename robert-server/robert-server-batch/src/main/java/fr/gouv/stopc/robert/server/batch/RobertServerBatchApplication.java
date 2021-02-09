package fr.gouv.stopc.robert.server.batch;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

/**
 * This Robert Server batch application is composed of different jobs.
 * These jobs are defined in ***JobConfiguration classes and
 * are run conditionally according to the robert.scoring.batch-mode
 * environment property value.
 * It means only one of the Robert Server JobConfiguration will be
 * run during the batch execution.
 */
@ComponentScan(basePackages  = "fr.gouv.stopc")
@EnableMongoRepositories(basePackages = "fr.gouv.stopc")
@EnableAutoConfiguration(exclude={DataSourceAutoConfiguration.class})
@EnableBatchProcessing
@SpringBootApplication
public class RobertServerBatchApplication {

	public static void main(String[] args) {
		SpringApplication.run(RobertServerBatchApplication.class, args);
	}

}
