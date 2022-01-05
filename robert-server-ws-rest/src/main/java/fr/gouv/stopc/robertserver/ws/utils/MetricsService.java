package fr.gouv.stopc.robertserver.ws.utils;

import fr.gouv.stopc.robertserver.ws.vo.ReportBatchRequestVo;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.Getter;
import org.springframework.stereotype.Service;

@Service
public class MetricsService {

    private MeterRegistry meterRegistry;

    private Counter robertWsReportHellomessageCounter;

    public MetricsService(final MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        this.robertWsReportHellomessageCounter = Counter.builder("robert.ws.report.hellomessage")
                .description("Number of HelloMessages that will be processed by the batch")
                .baseUnit("HelloMessage")
                .register(meterRegistry);
    }

    public void countHelloMessages(ReportBatchRequestVo reportBatchRequestVo) {

        final var helloMessagesCount = reportBatchRequestVo.getContacts().stream()
                .mapToInt(contact -> contact.getIds().size())
                .sum();
        this.robertWsReportHellomessageCounter.increment(helloMessagesCount);

    }

}
