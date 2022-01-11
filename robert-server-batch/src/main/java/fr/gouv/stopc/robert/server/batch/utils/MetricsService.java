package fr.gouv.stopc.robert.server.batch.utils;

import fr.gouv.stopc.robertserver.database.model.Contact;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.LongTaskTimer;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

@Service
public class MetricsService {

    private final Counter robertBatchHelloMessageTotal;

    private final LongTaskTimer robertBatchCryptoRPCRequestTimer;

    public MetricsService(final MeterRegistry meterRegistry) {

        robertBatchHelloMessageTotal = Counter.builder("robert.batch.hellomessage.total")
                .description("Number of HelloMessages of a Contact")
                .baseUnit("HelloMessage")
                .register(meterRegistry);

        robertBatchCryptoRPCRequestTimer = LongTaskTimer.builder("robert.crypto.rpc.request")
                .description("Execution time of RPC operations on the crypto server")
                .tag("operation", "validateContact")
                .register(meterRegistry);
    }

    public LongTaskTimer.Sample startRobertBatchCryptoRPCRequestTimer() {
        return robertBatchCryptoRPCRequestTimer.start();
    }

    public void countHelloMessages(final Contact contact) {
        if (null != contact.getMessageDetails() && !contact.getMessageDetails().isEmpty()) {
            robertBatchHelloMessageTotal.increment(contact.getMessageDetails().size());
        }
    }
}
