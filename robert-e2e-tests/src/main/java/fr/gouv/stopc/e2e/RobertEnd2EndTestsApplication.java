package fr.gouv.stopc.e2e;

import lombok.extern.slf4j.Slf4j;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

@SpringBootApplication
@ConfigurationPropertiesScan
@Slf4j
public class RobertEnd2EndTestsApplication {

    public static void main(String[] args) {
        if (args.length != 0) {
            final var tags = String.join(" && ", args);
            System.setProperty("cucumber.filter.tags", tags);
            log.info("Using cucumber.filter.tags={}", tags);
        }

        final var launcherDiscoveryRequest = LauncherDiscoveryRequestBuilder.request()
                .selectors(selectClass(CucumberTest.class))
                .build();

        final var summaryGeneratingListener = new SummaryGeneratingListener();
        final var launcher = LauncherFactory.create();
        launcher.registerTestExecutionListeners(summaryGeneratingListener);
        launcher.execute(launcherDiscoveryRequest);

        final var failures = summaryGeneratingListener.getSummary()
                .getFailures();

        System.exit(failures.size() > 0 ? 1 : 0);
    }
}
