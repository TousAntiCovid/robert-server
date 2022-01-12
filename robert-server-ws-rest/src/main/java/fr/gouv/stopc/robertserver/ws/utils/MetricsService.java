package fr.gouv.stopc.robertserver.ws.utils;

import fr.gouv.stopc.robertserver.ws.vo.ReportBatchRequestVo;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

@Service
public class MetricsService {

    private final Counter robertWsReportHellomessageCounter;

    public MetricsService(final MeterRegistry meterRegistry) {

        robertWsReportHellomessageCounter = Counter.builder("robert.ws.report.hellomessage")
                .description(" Number of reported hello messages")
                .baseUnit("HelloMessage")
                .register(meterRegistry);
    }

    public void countHelloMessages(ReportBatchRequestVo reportBatchRequestVo) {

        final var helloMessagesCount = reportBatchRequestVo.getContacts().stream()
                .mapToInt(contact -> contact.getIds().size())
                .sum();
        robertWsReportHellomessageCounter.increment(helloMessagesCount);

    }

}
