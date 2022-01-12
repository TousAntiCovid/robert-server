package fr.gouv.stopc.robert.server.batch.utils;

import fr.gouv.stopc.robertserver.database.model.Contact;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

@Service
public class MetricsService {

    private final Counter robertBatchHelloMessageTotal;

    public MetricsService(final MeterRegistry meterRegistry) {

        robertBatchHelloMessageTotal = Counter.builder("robert.batch.hellomessage.total")
                .description("Number of HelloMessages of a Contact")
                .baseUnit("HelloMessage")
                .register(meterRegistry);
    }

    public void countHelloMessages(final Contact contact) {
        if (null != contact.getMessageDetails() && !contact.getMessageDetails().isEmpty()) {
            robertBatchHelloMessageTotal.increment(contact.getMessageDetails().size());
        }
    }
}
