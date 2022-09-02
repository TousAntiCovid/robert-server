package fr.gouv.stopc.robert.server.batch.service;

import fr.gouv.stopc.robertserver.database.model.Contact;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

@Service
public class MetricsService {

    private final DistributionSummary robertBatchHelloMessageTotal;

    public MetricsService(final MeterRegistry meterRegistry) {

        robertBatchHelloMessageTotal = DistributionSummary.builder("robert.batch.contact.hellomessage")
                .description("Hello message count per contact")
                .baseUnit("HelloMessage")
                .publishPercentiles(0.05, 0.3, 0.5, 0.8, 0.95)
                .publishPercentileHistogram(false)
                .sla(10, 100, 1000, 5000)
                .register(meterRegistry);
    }

    public void countHelloMessages(final Contact contact) {
        if (null != contact.getMessageDetails() && !contact.getMessageDetails().isEmpty()) {
            robertBatchHelloMessageTotal.record(contact.getMessageDetails().size());
        }
    }
}
