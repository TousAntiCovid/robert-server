package fr.gouv.tac.e2e;

import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

import java.util.List;

import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.awaitility.pollinterval.FibonacciPollInterval.fibonacci;
import static org.hamcrest.Matchers.is;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

@SpringBootApplication
@ConfigurationPropertiesScan
public class RobertIntegrationTestsApplication {

    public static void main(String[] args) {
        var exitCode = 1;
        try {
            await("Robert platform is ready")
                    .atMost(3, MINUTES)
                    .pollInterval(fibonacci(SECONDS))
                    .until(() -> runCucumberTestClass(SmokeTests.class), is(0));

            exitCode = runCucumberTestClass(CucumberTest.class);
        } finally {
            System.exit(exitCode);
        }
    }

    private static <T> int runCucumberTestClass(Class<T> classToRun) {

        var launcherDiscoveryRequest = LauncherDiscoveryRequestBuilder.request()
                .selectors(selectClass(classToRun))
                .build();

        var launcher = LauncherFactory.create();
        var summaryGeneratingListener = new SummaryGeneratingListener();

        launcher.registerTestExecutionListeners(summaryGeneratingListener);
        launcher.execute(launcherDiscoveryRequest);

        TestExecutionSummary summary = summaryGeneratingListener.getSummary();

        List<TestExecutionSummary.Failure> failures = summary.getFailures();

        return failures.size() > 0 ? 1 : 0;
    }
}
