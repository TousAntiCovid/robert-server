package fr.gouv.stopc.robert.server.batch.utils;

import fr.gouv.stopc.robertserver.database.model.Contact;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.Getter;
import org.springframework.stereotype.Service;

@Service
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

    public void countHelloMessages(Contact contact){
        if (contact.getMessageDetails() != null && !contact.getMessageDetails().isEmpty()) {
            this.robertBatchHelloMessageTotal.increment(contact.getMessageDetails().size());
        }
    }
}
