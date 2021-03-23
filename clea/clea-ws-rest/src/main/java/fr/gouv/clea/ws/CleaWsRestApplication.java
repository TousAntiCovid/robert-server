package fr.gouv.clea.ws;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "fr.gouv.clea.ws")
public class CleaWsRestApplication {

    public static void main(String[] args) {
        SpringApplication.run(CleaWsRestApplication.class, args);
    }
}
