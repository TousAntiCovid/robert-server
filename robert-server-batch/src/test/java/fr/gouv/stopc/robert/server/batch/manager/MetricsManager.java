package fr.gouv.stopc.robert.server.batch.manager;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import org.assertj.core.api.AbstractDoubleAssert;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;

import java.util.Map;

import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class MetricsManager implements TestExecutionListener {

    private static MeterRegistry registry;

    private static Map<Meter.Id, Double> metrics;

    @Override
    public void beforeTestMethod(TestContext testContext) throws Exception {
        TestExecutionListener.super.beforeTestMethod(testContext);
        registry = testContext.getApplicationContext().getBean(MeterRegistry.class);

        metrics = registry.getMeters()
                .stream()
                .collect(toMap(Meter::getId, this::meterValue));
    }

    private Double meterValue(Meter meter) {
        if (meter instanceof Counter) {
            return ((Counter) meter).count();
        } else {
            return -1.0;
        }
    }

    public static AbstractDoubleAssert<?> assertThatCounterMetricIncrement(String label, String... tags) {
        final var counter = registry.counter(label, tags);
        final var increment = counter.count() - metrics.getOrDefault(counter.getId(), 0.0);
        return assertThat(increment)
                .as("Counter value increment for %s %s ", label, String.join(",", tags));
    }

}
