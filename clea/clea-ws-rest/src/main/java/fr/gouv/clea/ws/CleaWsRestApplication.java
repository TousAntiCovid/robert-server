package fr.gouv.clea.ws;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(
        scanBasePackages = "fr.gouv.clea.ws",
        exclude = UserDetailsServiceAutoConfiguration.class
)
public class CleaWsRestApplication {

    public static void main(String[] args) {
        SpringApplication.run(CleaWsRestApplication.class, args);
    }
}
