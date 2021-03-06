package fr.gouv.stopc.robertserver.ws;

import fr.gouv.stopc.robertserver.ws.config.RobertWsProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@EnableConfigurationProperties(RobertWsProperties.class)
@ComponentScan(basePackages = "fr.gouv.stopc")
@EnableMongoRepositories(basePackages = "fr.gouv.stopc")
@EnableAutoConfiguration(exclude = { DataSourceAutoConfiguration.class })
@SpringBootApplication
public class RobertServerWsRestApplication {

    public static void main(String[] args) {
        SpringApplication.run(RobertServerWsRestApplication.class, args);
    }

}
