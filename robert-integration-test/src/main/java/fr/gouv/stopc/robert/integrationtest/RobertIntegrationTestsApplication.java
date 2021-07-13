package fr.gouv.stopc.robert.integrationtest;

import org.junit.runner.JUnitCore;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication(exclude = {
        DataSourceAutoConfiguration.class
})
@ConfigurationPropertiesScan("fr.gouv.stopc.robert.integrationtest.config")
public class RobertIntegrationTestsApplication {

    public static void main(String[] args) {
        JUnitCore.main(CucumberTest.class.getName());
    }
}
