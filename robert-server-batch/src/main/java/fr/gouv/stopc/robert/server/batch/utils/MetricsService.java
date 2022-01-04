package fr.gouv.stopc.robert.server.batch.utils;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.Getter;
import org.springframework.stereotype.Service;

@Service
@Getter
public class MetricsService {

    private MeterRegistry meterRegistry;

    private Counter robertBatchHelloMessageTotal;

    public MetricsService(final MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        this.robertBatchHelloMessageTotal = Counter.builder("robert.batch.hellomessage.total")
                .description("Number of HelloMessages that will generate requests to the CryptoServer")
                .baseUnit("HelloMessage")
                .register(meterRegistry);
    }

}
