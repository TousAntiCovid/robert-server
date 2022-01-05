package fr.gouv.stopc.robertserver.ws.utils;

import fr.gouv.stopc.robertserver.ws.vo.ReportBatchRequestVo;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

@Service
public class MetricsService {

    private final DistributionSummary robertWsReportHellomessageDistribution;

    public MetricsService(final MeterRegistry meterRegistry) {

        robertWsReportHellomessageDistribution = DistributionSummary.builder("robert.ws.report.hellomessage")
                .description(" Number of reported hello messages")
                .baseUnit("HelloMessage")
                .publishPercentiles(0.3, 0.5, 0.95)
                .publishPercentileHistogram(false)
                .sla(10, 100, 1000, 5000)
                .register(meterRegistry);
    }

    public void countHelloMessages(ReportBatchRequestVo reportBatchRequestVo) {

        final var helloMessagesCount = reportBatchRequestVo.getContacts().stream()
                .mapToInt(contact -> contact.getIds().size())
                .sum();

        robertWsReportHellomessageDistribution.record(helloMessagesCount);
    }

}
