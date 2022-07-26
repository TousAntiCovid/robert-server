package fr.gouv.stopc.e2e;

import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

import java.util.List;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

@SpringBootApplication
@ConfigurationPropertiesScan
public class RobertEnd2EndTestsApplication {

    public static void main(String[] args) {
        final var launcherDiscoveryRequest = LauncherDiscoveryRequestBuilder.request()
                .selectors(selectClass(CucumberTest.class))
                .build();

        final var launcher = LauncherFactory.create();
        final var summaryGeneratingListener = new SummaryGeneratingListener();

        launcher.registerTestExecutionListeners(summaryGeneratingListener);
        launcher.execute(launcherDiscoveryRequest);

        TestExecutionSummary summary = summaryGeneratingListener.getSummary();

        List<TestExecutionSummary.Failure> failures = summary.getFailures();

        System.exit(failures.size() > 0 ? 1 : 0);
    }
}
