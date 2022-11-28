package fr.gouv.stopc.robertserver.ws.repository.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document(value = "kpis")
data class Kpi(
    @Id
    val id: String,
    val name: String,
    val value: Long
)

/**
 * Names of the kpis stored.
 */
enum class KpiName(
    val key: String
) {
    ALERTED_USERS("alertedUsers"),
    EXPOSED_BUT_NOT_AT_RISK_USERS("exposedButNotAtRiskUsers"),
    INFECTED_USERS_NOT_NOTIFIED("infectedUsersNotNotified"),
    NOTIFIED_USERS_SCORED_AGAIN("notifiedUsersScoredAgain"),
    NOTIFIED_USERS("notifiedUsers"),
    USERS_ABOVE_RISK_THRESHOLD_BUT_RETENTION_PERIOD_EXPIRED("usersAboveRiskThresholdButRetentionPeriodExpired"),
    REPORTS_COUNT("reportsCount"), ;

    companion object {
        fun from(key: String) = values()
            .find { key == it.key }
            ?: throw IllegalStateException("Unknown KPI '$key'")
    }
}
