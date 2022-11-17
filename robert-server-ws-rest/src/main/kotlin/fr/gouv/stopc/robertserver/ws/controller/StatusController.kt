package fr.gouv.stopc.robertserver.ws.controller

import fr.gouv.stopc.robertserver.ws.api.StatusApi
import fr.gouv.stopc.robertserver.ws.api.model.ExposureStatusRequest
import fr.gouv.stopc.robertserver.ws.api.model.ExposureStatusResponse
import fr.gouv.stopc.robertserver.ws.common.RequestType.STATUS
import fr.gouv.stopc.robertserver.ws.service.IdentityService
import fr.gouv.stopc.robertserver.ws.service.JwtService
import fr.gouv.stopc.robertserver.ws.service.RegistrationService
import fr.gouv.stopc.robertserver.ws.service.RiskStatus.High
import fr.gouv.stopc.robertserver.ws.service.RiskStatus.None
import fr.gouv.stopc.robertserver.ws.service.RobertCredentials
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class StatusController(
    private val identityService: IdentityService,
    private val registrationService: RegistrationService,
    private val jwtService: JwtService
) : StatusApi {

    override suspend fun eSR(exposureStatusRequest: ExposureStatusRequest): ResponseEntity<ExposureStatusResponse> {
        val credentials = RobertCredentials(
            requestType = STATUS,
            ebid = exposureStatusRequest.ebid.asList(),
            epochId = exposureStatusRequest.epochId,
            time = exposureStatusRequest.time.asList(),
            mac = exposureStatusRequest.mac.asList()
        )
        val tuplesBundle = identityService.authenticateAndRenewTuples(credentials)
        return when (val risk = registrationService.status(tuplesBundle.idA, exposureStatusRequest.pushInfo)) {
            is None -> ResponseEntity.ok(
                ExposureStatusResponse(
                    tuples = tuplesBundle.encryptedTuples.toByteArray(),
                    config = emptyList(),
                    riskLevel = 0,
                    analyticsToken = jwtService.generateAnalyticsToken()
                )
            )
            is High -> ResponseEntity.ok(
                ExposureStatusResponse(
                    tuples = tuplesBundle.encryptedTuples.toByteArray(),
                    config = emptyList(),
                    riskLevel = 4,
                    lastContactDate = risk.lastContactDate.asNtpTimestamp().toString(),
                    lastRiskScoringDate = risk.lastRiskScoringDate.asNtpTimestamp().toString(),
                    declarationToken = jwtService.generateDeclarationToken(tuplesBundle.idA, risk),
                    analyticsToken = jwtService.generateAnalyticsToken()
                )
            )
        }
    }
}
