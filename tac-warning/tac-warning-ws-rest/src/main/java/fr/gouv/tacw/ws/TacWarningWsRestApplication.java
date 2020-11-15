package fr.gouv.tacw.ws;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = { "fr.gouv.tacw.ws", "fr.gouv.tacw.database" })
public class TacWarningWsRestApplication {

	public static void main(String[] args) {
		SpringApplication.run(TacWarningWsRestApplication.class, args);
	}

}
