package fr.gouv.tacw;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "fr.gouv.tacw") // fixme remove scan
public class CleaVenueConsumerApplication {

    public static void main(String[] args) {
        SpringApplication.run(CleaVenueConsumerApplication.class, args);
    }
}
