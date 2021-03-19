package fr.gouv.tacw;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "fr.gouv.tacw")
public class CleaWsRestApplication {

    public static void main(String[] args) {
        SpringApplication.run(CleaWsRestApplication.class, args);
    }
}
