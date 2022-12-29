package fr.gouv.stopc.robertserver.ws.controller

import fr.gouv.stopc.robertserver.common.RobertRequestType.DELETE_HISTORY
import fr.gouv.stopc.robertserver.ws.api.DeleteExposureHistoryApi
import fr.gouv.stopc.robertserver.ws.api.model.AuthentifiedRequest
import fr.gouv.stopc.robertserver.ws.api.model.UnregisterResponse
import fr.gouv.stopc.robertserver.ws.service.IdentityService
import fr.gouv.stopc.robertserver.ws.service.RegistrationService
import fr.gouv.stopc.robertserver.ws.service.RobertCredentials
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class DeleteHistoryController(
    private val identityService: IdentityService,
    private val registrationService: RegistrationService
) : DeleteExposureHistoryApi {

    override fun deleteExposureHistory(authentifiedRequest: AuthentifiedRequest): ResponseEntity<UnregisterResponse> {
        val credentials = RobertCredentials(
            requestType = DELETE_HISTORY,
            ebid = authentifiedRequest.ebid.asList(),
            epochId = authentifiedRequest.epochId,
            time = authentifiedRequest.time.asList(),
            mac = authentifiedRequest.mac.asList()
        )
        val idA = identityService.authenticate(credentials)
        registrationService.deleteExposureHistory(idA)
        return ResponseEntity.ok(
            UnregisterResponse(
                success = true,
                message = null
            )
        )
    }
}
