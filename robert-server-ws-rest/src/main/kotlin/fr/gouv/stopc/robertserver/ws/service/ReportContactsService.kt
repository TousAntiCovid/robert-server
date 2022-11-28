package fr.gouv.stopc.robertserver.ws.service

import fr.gouv.stopc.robertserver.ws.api.model.Contact
import fr.gouv.stopc.robertserver.ws.repository.ContactToProcessRepository
import fr.gouv.stopc.robertserver.ws.repository.KpiRepository
import fr.gouv.stopc.robertserver.ws.repository.model.ContactToProcess
import fr.gouv.stopc.robertserver.ws.repository.model.HelloMessageDetail
import fr.gouv.stopc.robertserver.ws.repository.model.KpiName.REPORTS_COUNT
import io.micrometer.core.instrument.DistributionSummary
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Service
import java.time.Duration

@Service
class ReportContactsService(
    private val contactToProcessRepository: ContactToProcessRepository,
    private val kpiRepository: KpiRepository,
    meterRegistry: MeterRegistry
) {

    private val helloMessagesDistribution = DistributionSummary.builder("robert.ws.report.hellomessage")
        .description("Hello message count per report")
        .baseUnit("HelloMessage")
        .distributionStatisticExpiry(Duration.ofDays(7))
        .publishPercentiles(0.05, 0.3, 0.5, 0.8, 0.95)
        .publishPercentileHistogram(false)
        .serviceLevelObjectives(10.0, 100.0, 1000.0, 5000.0)
        .register(meterRegistry)

    /**
     * Saves reported contacts into the database.
     */
    fun report(contacts: List<Contact>) {
        if (contacts.isNotEmpty()) {
            val contactsToProcess = contacts
                .map { contact ->
                    ContactToProcess(
                        ebid = contact.ebid.asList(),
                        ecc = contact.ecc.asList(),
                        messageDetails = contact.ids.map { hello ->
                            HelloMessageDetail(
                                timeCollectedOnDevice = hello.timeCollectedOnDevice,
                                timeFromHelloMessage = hello.timeFromHelloMessage,
                                mac = hello.mac.asList(),
                                rssiCalibrated = hello.rssiCalibrated
                            )
                        }
                    )
                }
            contactToProcessRepository.saveAll(contactsToProcess)
        }
        kpiRepository.incrementKpi(REPORTS_COUNT)
        val helloMessageCount = contacts
            .flatMap(Contact::ids)
            .count()
            .toDouble()
        helloMessagesDistribution.record(helloMessageCount)
    }
}
