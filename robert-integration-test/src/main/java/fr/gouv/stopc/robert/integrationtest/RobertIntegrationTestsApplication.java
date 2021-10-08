package fr.gouv.stopc.robert.integrationtest;

import org.junit.runner.JUnitCore;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.awaitility.pollinterval.FibonacciPollInterval.fibonacci;
import static org.hamcrest.Matchers.is;

@SpringBootApplication(exclude = {
        DataSourceAutoConfiguration.class
})
@ConfigurationPropertiesScan("fr.gouv.stopc.robert.integrationtest.config")
public class RobertIntegrationTestsApplication {

    public static void main(String[] args) {
//        var exitCode = 1;
//        try {
////            await("Robert platform is ready")
////                    .atMost(1, MINUTES)
////                    .pollInterval(fibonacci(SECONDS))
////                    .until(() -> JUnitCore.runClasses(CucumberTest.class).wasSuccessful(), is(true));
//
//            exitCode = JUnitCore.runClasses(CucumberTest.class).wasSuccessful() ? 0 : 1;
//        } finally {
//            System.exit(exitCode);
//        }
        JUnitCore.main(CucumberTest.class.getName());
    }
}
