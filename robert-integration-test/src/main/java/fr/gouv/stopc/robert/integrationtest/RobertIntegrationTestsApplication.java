package fr.gouv.stopc.robert.integrationtest;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

import java.util.List;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

@SpringBootApplication(exclude = {
        DataSourceAutoConfiguration.class
})
@ConfigurationPropertiesScan("fr.gouv.stopc.robert.integrationtest.config")
public class RobertIntegrationTestsApplication {

    public static void main(String[] args) {
        var launcherDiscoveryRequest = LauncherDiscoveryRequestBuilder.request()
                .selectors(selectClass(CucumberTest.class))
                .build();

        var launcher = LauncherFactory.create();
        var summaryGeneratingListener = new SummaryGeneratingListener();

        launcher.registerTestExecutionListeners(summaryGeneratingListener);
        launcher.execute(launcherDiscoveryRequest);

        TestExecutionSummary summary = summaryGeneratingListener.getSummary();

        List<TestExecutionSummary.Failure> failures = summary.getFailures();
        System.out.println("getTestsSucceededCount() - " + summary.getTestsSucceededCount());
        failures.forEach(failure -> System.out.println("failure - " + failure.getException()));

        var exitCode = failures.size()>0 ? 1 : 0;
        System.exit(exitCode);
      }
}
