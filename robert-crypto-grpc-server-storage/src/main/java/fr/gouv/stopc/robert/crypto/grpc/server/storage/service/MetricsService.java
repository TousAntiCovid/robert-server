package fr.gouv.stopc.robert.crypto.grpc.server.storage.service;

import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class MetricsService {

    private final DistributionSummary keyAccess;

    public MetricsService(final MeterRegistry meterRegistry) {
        keyAccess = DistributionSummary.builder("robert.crypto.hsm.key.access")
                .sla(1, 5, 10, 15)
                .register(meterRegistry);
    }

    public void recordKeyAccess(final LocalDate keyDate) {
        final var days = keyDate.until(LocalDate.now()).getDays();
        keyAccess.record(days);
    }
}
