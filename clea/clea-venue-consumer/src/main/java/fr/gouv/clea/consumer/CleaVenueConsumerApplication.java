package fr.gouv.clea.consumer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "fr.gouv.clea.consumer")
public class CleaVenueConsumerApplication {

    public static void main(String[] args) {
        SpringApplication.run(CleaVenueConsumerApplication.class, args);
    }
}
