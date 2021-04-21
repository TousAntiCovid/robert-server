package fr.gouv.clea;

import fr.gouv.clea.config.BatchProperties;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EntityScan
@EnableBatchProcessing
@EnableTransactionManagement
@EnableConfigurationProperties(BatchProperties.class)
public class CleaBatchApplication {

    public static void main(String[] args) {
        SpringApplication.run(CleaBatchApplication.class, args);
    }
}
