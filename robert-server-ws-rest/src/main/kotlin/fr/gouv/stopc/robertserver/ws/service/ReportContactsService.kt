package fr.gouv.stopc.robertserver.ws.service

import fr.gouv.stopc.robertserver.ws.api.model.Contact
import fr.gouv.stopc.robertserver.ws.kpis.KpiRepository
import fr.gouv.stopc.robertserver.ws.repository.ContactToProcessRepository
import fr.gouv.stopc.robertserver.ws.repository.model.ContactToProcess
import fr.gouv.stopc.robertserver.ws.repository.model.HelloMessageDetail
import fr.gouv.stopc.robertserver.ws.repository.model.KpiName.REPORTS_COUNT
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactive.awaitLast
import kotlinx.coroutines.reactor.asFlux
import org.springframework.stereotype.Service

@Service
class ReportContactsService(
    private val contactToProcessRepository: ContactToProcessRepository,
    private val kpiRepository: KpiRepository
) {

    /**
     * Saves reported contacts into the database.
     */
    suspend fun report(contacts: List<Contact>) {
        if (contacts.isNotEmpty()) {
            val contactsToProcess = contacts
                .asFlow()
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
            contactToProcessRepository.saveAll(contactsToProcess.asFlux()).awaitLast()
        }
        kpiRepository.incrementKpi(REPORTS_COUNT)
    }
}
