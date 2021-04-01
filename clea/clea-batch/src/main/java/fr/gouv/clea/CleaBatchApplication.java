package fr.gouv.clea;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EntityScan
@EnableBatchProcessing 
@EnableTransactionManagement
public class CleaBatchApplication {

	public static void main(String[] args) {
		SpringApplication.run(CleaBatchApplication.class, args);
	}


}
