package fr.gouv.stopc.robertserver.ws.repository.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document(value = "idTable")
data class Registration(

    @Id
    val permanentIdentifier: ByteArray,

    val isNotified: Boolean = false,

    val atRisk: Boolean = false,

    val notifiedForCurrentRisk: Boolean = false,

    val lastStatusRequestEpoch: Int = 0,

    val latestRiskEpoch: Int = 0,

    val lastContactTimestamp: Long = 0,

    val lastFailedStatusRequestEpoch: Int = 0,

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
