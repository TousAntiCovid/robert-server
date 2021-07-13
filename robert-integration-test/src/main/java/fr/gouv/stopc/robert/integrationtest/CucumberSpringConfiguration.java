package fr.gouv.stopc.robert.integrationtest;

import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = { RobertIntegrationTestsApplication.class })
@CucumberContextConfiguration
public class CucumberSpringConfiguration {
}
