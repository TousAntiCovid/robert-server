package fr.gouv.stopc.robertserver.ws.service

import fr.gouv.stopc.pushserver.api.PushTokenApi
import fr.gouv.stopc.pushserver.api.model.PushRequest
import fr.gouv.stopc.robertserver.common.RobertClock
import fr.gouv.stopc.robertserver.common.RobertClock.ROBERT_EPOCH
import fr.gouv.stopc.robertserver.common.RobertClock.RobertInstant
import fr.gouv.stopc.robertserver.ws.RobertWsProperties
import fr.gouv.stopc.robertserver.ws.api.model.PushInfo
import fr.gouv.stopc.robertserver.ws.repository.KpiRepository
import fr.gouv.stopc.robertserver.ws.repository.RegistrationRepository
import fr.gouv.stopc.robertserver.ws.repository.model.KpiName.ALERTED_USERS
import fr.gouv.stopc.robertserver.ws.repository.model.KpiName.NOTIFIED_USERS
import fr.gouv.stopc.robertserver.ws.repository.model.Registration
import fr.gouv.stopc.robertserver.ws.service.model.IdA
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.stereotype.Service

/**
 * Manages client application state.
 */
@Service
class RegistrationService(
    private val clock: RobertClock,
    private val registrationRepository: RegistrationRepository,
    private val kpiRepository: KpiRepository,
    private val pushTokenApi: PushTokenApi,
    private val config: RobertWsProperties
) {

    /**
     * Creates a client application.
     *
     * @param idA the application identifier
     * @param pushInfo optional _push informations_ for Apple device
     */
    suspend fun register(idA: IdA, pushInfo: PushInfo?) {
        registrationRepository.save(Registration(idA.toByteArray())).awaitSingleOrNull()
        createOrUpdatePushInfo(pushInfo)
    }

    /**
     * Returns the [RiskStatus] of a client application.
     *
     * @param idA the application identifier
     * @param pushInfo optional _push informations_ for Apple device
     */
    suspend fun status(idA: IdA, pushInfo: PushInfo?): RiskStatus {
        val registration = registrationRepository.findById(idA.toByteArray()).awaitSingleOrNull()
            ?: throw MissingRegistrationException(idA)

        validateStatusRequestThrottling(registration)

        val updatedRegistration = registrationRepository.save(
            registration.copy(
                lastStatusRequestEpoch = clock.now().asEpochId(),
                isNotified = registration.isNotified || registration.atRisk,
                notifiedForCurrentRisk = registration.notifiedForCurrentRisk || registration.atRisk
            )
        ).awaitSingle()

        createOrUpdatePushInfo(pushInfo)

        return if (registration.atRisk) {
            if (!registration.isNotified) {
                kpiRepository.incrementKpi(ALERTED_USERS)
            }
            if (!registration.notifiedForCurrentRisk) {
                kpiRepository.incrementKpi(NOTIFIED_USERS)
            }
            RiskStatus.High(
                lastRiskScoringDate = clock.atEpoch(updatedRegistration.latestRiskEpoch),
                lastContactDate = clock.atNtpTimestamp(updatedRegistration.lastContactTimestamp),
                lastStatusRequest = clock.atEpoch(updatedRegistration.lastStatusRequestEpoch)
            )
        } else {
            RiskStatus.None()
        }
    }

    /**
     * Ensure no /status request has been made during last X epochs, X being a configuration parameter.
     * @throws RequestRateExceededException when the request rate is exceeded
     */
    private suspend fun validateStatusRequestThrottling(registration: Registration) {
        val now = clock.now()
        val previousEsr = clock.atEpoch(registration.lastStatusRequestEpoch)
        val epochsSinceLastStatusRequest = previousEsr.until(now)
            .abs()
            .dividedBy(ROBERT_EPOCH.duration)
        if (epochsSinceLastStatusRequest < config.minEpochsBetweenStatusRequests) {
            registrationRepository.save(
                registration.copy(
                    lastFailedStatusRequestEpoch = now.asEpochId(),
                    lastFailedStatusRequestMessage = "Discarding ESR request because it is too close to the previous one: previous ESR request epoch ${previousEsr.asEpochId()} vs now ${now.asEpochId()} < ${config.minEpochsBetweenStatusRequests} epochs"
                )
            ).awaitSingle()
            throw RequestRateExceededException(1, config.minEpochsBetweenStatusRequests)
        }
    }

    /**
     * Send _push informations_ to the Push Notification service.
     */
    private suspend inline fun createOrUpdatePushInfo(pushInfo: PushInfo?) {
        if (null != pushInfo) {
            pushTokenApi.registerPushToken(
                PushRequest()
                    .token(pushInfo.token)
                    .locale(pushInfo.locale)
                    .timezone(pushInfo.timezone)
            ).awaitSingleOrNull()
        }
    }

    /**
     * Clear exposure history for the given _application identifier_.
     *
     * @param idA the application identifier
     */
    suspend fun deleteExposureHistory(idA: IdA) {
        val registration = registrationRepository.findById(idA.toByteArray()).awaitSingleOrNull()
            ?: throw MissingRegistrationException(idA)
        registrationRepository.save(
            registration.copy(exposedEpochs = emptyList())
        ).awaitSingle()
    }

    /**
     * Removes all data related to an _application identifier_.
     *
     * @param idA the application identifier
     */
    suspend fun unregister(idA: IdA) {
        val registration = registrationRepository.findById(idA.toByteArray()).awaitSingleOrNull()
            ?: throw MissingRegistrationException(idA)
        registrationRepository.delete(registration).awaitSingleOrNull()
    }
}

class MissingRegistrationException(val idA: IdA) : RuntimeException("Missing registration for idA '$idA'")

class RequestRateExceededException(limit: Int, epochWindow: Int) :
    RuntimeException("Number of requests exceeded the limit of $limit over time window of $epochWindow epochs")

/**
 * Represents the risk status of a registered application.
 */
sealed class RiskStatus {

    /**
     * Means the registered application is not at risk.
     */
    data class None(private val none: Any = Any()) : RiskStatus()

    /**
     * Means the registered application **is** at risk and contains relevant informations to be sent the application
     */
    data class High(

        /**
         * The instant the application was evaluated _at risk_ by the batch application.
         */
        val lastRiskScoringDate: RobertInstant,

        /**
         * The last instant a _risked contact_ was detected for the application.
         */
        val lastContactDate: RobertInstant,

        /**
         * The last known status instant.
         */
        val lastStatusRequest: RobertInstant
    ) : RiskStatus()
}
