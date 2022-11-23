package fr.gouv.stopc.robertserver.ws.repository.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document(value = "idTable")
data class Registration(

    @Id
    val permanentIdentifier: ByteArray,

    /**
     * Does this application has been informed about a risk at least one time since the service started?
     */
    val isNotified: Boolean = false,

    /**
     * Is this application currently _at risk_?
     */
    val atRisk: Boolean = false,

    /**
     * If this application is _at _risk, has it been informed?
     */
    val notifiedForCurrentRisk: Boolean = false,

    /**
     * Epoch of the last `/status` request made by the application.
     */
    val lastStatusRequestEpoch: Int = 0,

    /**
     * The epoch when this application was evaluated _at risk_.
     */
    val latestRiskEpoch: Int = 0,

    /**
     * The day of the last known risked contact.
     *
     * It is stored as an NTP timestamp at the start of the day.
     * Example: assuming the last risked contact was `2021-07-20T15:33:44Z`,
     * then the value will be `2021-07-20T00:00:00Z` represented as an NTP timestamp, which is `3835728000L`.
     */
    val lastContactTimestamp: Long = 0,

    /**
     * The epoch number of a rejected `/status` request.
     */
    val lastFailedStatusRequestEpoch: Int = 0,

    /**
     * A message explaining the `/status` request rejection.
     */
    val lastFailedStatusRequestMessage: String? = null,

    /**
     * Record the time difference perceived between the server time and the client
     * time To be set by any request that can be tied to an ID
     */
    val lastTimestampDrift: Long = 0,

    val exposedEpochs: List<EpochExposition> = listOf(),

    /**
     * A marker attribute set to `true` when [atRisk] attribute needs to be recomputed from latest [exposedEpochs].
     */
    val outdatedRisk: Boolean = false
)

data class EpochExposition(
    val epochId: Int,
    val expositionScores: List<Double>
)
