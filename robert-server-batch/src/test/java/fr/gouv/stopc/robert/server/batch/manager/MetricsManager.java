package fr.gouv.stopc.robert.server.batch.manager;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.assertj.core.api.AbstractDoubleAssert;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.Condition;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;

import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class MetricsManager implements TestExecutionListener {

    private static MeterRegistry registry;

    private static Map<Meter.Id, Double> metrics;

    @Override
    public void beforeTestMethod(TestContext testContext) {
        registry = testContext.getApplicationContext().getBean(MeterRegistry.class);

        metrics = registry.getMeters()
                .stream()
                .collect(toMap(Meter::getId, this::meterValue));
    }

    private Double meterValue(Meter meter) {
        if (meter instanceof Counter) {
            return ((Counter) meter).count();
        } else if (meter instanceof Timer) {
            return (double) ((Timer) meter).count();
        } else {
            return -1.0;
        }
    }

    public static void assertThatLogsMatchingRegex(List<String> logs, String regex, int times) {
        final Condition<String> rowMatchingRegex = new Condition<>(value -> value.matches(regex), regex);
        Assertions.assertThat(logs).as("Number of logs matching the regular expression")
                .haveExactly(times, rowMatchingRegex);
    }

    public static AbstractDoubleAssert<?> assertThatCounterMetricIncrement(String label, String... tags) {
        final var counter = registry.counter(label, tags);
        final var increment = counter.count() - metrics.getOrDefault(counter.getId(), 0.0);
        return assertThat(increment)
                .as("Counter value increment for %s %s ", label, String.join(",", tags));
    }

    public static AbstractDoubleAssert<?> assertThatTimerMetricIncrement(String label, String... tags) {
        final var timer = registry.find(label).tags(tags).timer();
        assertThat(timer).isNotNull();
        final var increment = timer.count() - metrics.getOrDefault(timer.getId(), 0.0);
        return assertThat(increment)
                .as("Timer number of entries incremented for %s %s ", label, String.join(",", tags));
    }

}
