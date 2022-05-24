package fr.gouv.stopc.robert.server.batch.manager;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.assertj.core.api.AbstractDoubleAssert;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;

import java.util.HashMap;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class MetricsManager implements TestExecutionListener {

    private static MeterRegistry registry;

    private static HashMap<String, Double> metrics = new HashMap<>();

    private void loadAllMetrics() {
        registry.getMeters().forEach(
                meter -> {
                    final var name = meter.getId().getName();
                    if (meter instanceof Counter) {
                        metrics.put(name, ((Counter) meter).count());
                    } else if (meter instanceof Timer) {
                        metrics.put(name, Double.valueOf(((Timer) meter).count()));
                    } else if (meter instanceof DistributionSummary) {
                        metrics.put(name, Double.valueOf(((DistributionSummary) meter).count()));
                    }
                }
        );
    }

    @Override
    public void beforeTestMethod(TestContext testContext) throws Exception {
        TestExecutionListener.super.beforeTestMethod(testContext);
        registry = testContext.getApplicationContext().getBean(MeterRegistry.class);
        loadAllMetrics();
    }

    public static AbstractDoubleAssert<?> assertThatCounterMetricIncrement(String metricLabel) {
        var newMetricValue = registry.counter(metricLabel).count();
        var increment = newMetricValue - metrics.get(metricLabel);
        return assertThat(increment).as("Increment between before test method and now");
    }

}
