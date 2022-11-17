package fr.gouv.stopc.robertserver.ws.controller

import fr.gouv.stopc.robertserver.ws.api.KpiApi
import fr.gouv.stopc.robertserver.ws.api.model.RobertServerKpi
import fr.gouv.stopc.robertserver.ws.kpis.KpiRepository
import fr.gouv.stopc.robertserver.ws.repository.model.KpiName
import fr.gouv.stopc.robertserver.ws.repository.model.KpiName.ALERTED_USERS
import fr.gouv.stopc.robertserver.ws.repository.model.KpiName.EXPOSED_BUT_NOT_AT_RISK_USERS
import fr.gouv.stopc.robertserver.ws.repository.model.KpiName.INFECTED_USERS_NOT_NOTIFIED
import fr.gouv.stopc.robertserver.ws.repository.model.KpiName.NOTIFIED_USERS_SCORED_AGAIN
import fr.gouv.stopc.robertserver.ws.repository.model.KpiName.REPORTS_COUNT
import fr.gouv.stopc.robertserver.ws.repository.model.KpiName.USERS_ABOVE_RISK_THRESHOLD_BUT_RETENTION_PERIOD_EXPIRED
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class KpiController(
    private val kpiRepository: KpiRepository
) : KpiApi {

    override suspend fun kpis(): ResponseEntity<RobertServerKpi> {
        val kpiNames = KpiName.values().map(KpiName::key)
        return kpiRepository.findByNameIn(kpiNames)
            .collectMap({ KpiName.from(it.name) }, { it.value })
            .map { kpis ->
                RobertServerKpi(
                    alertedUsers = kpis[ALERTED_USERS] ?: 0,
                    exposedButNotAtRiskUsers = kpis[EXPOSED_BUT_NOT_AT_RISK_USERS] ?: 0,
                    infectedUsersNotNotified = kpis[INFECTED_USERS_NOT_NOTIFIED] ?: 0,
                    notifiedUsersScoredAgain = kpis[NOTIFIED_USERS_SCORED_AGAIN] ?: 0,
                    notifiedUsers = kpis[KpiName.NOTIFIED_USERS] ?: 0,
                    usersAboveRiskThresholdButRetentionPeriodExpired = kpis[USERS_ABOVE_RISK_THRESHOLD_BUT_RETENTION_PERIOD_EXPIRED] ?: 0,
                    reportsCount = kpis[REPORTS_COUNT] ?: 0
                )
            }
            .map { ResponseEntity.ok(it) }
            .awaitSingle()
    }
}
