package fr.gouv.stopc.robertserver.ws.controller

import fr.gouv.stopc.robertserver.common.RobertRequestType.UNREGISTER
import fr.gouv.stopc.robertserver.ws.api.UnregisterApi
import fr.gouv.stopc.robertserver.ws.api.model.UnregisterRequest
import fr.gouv.stopc.robertserver.ws.api.model.UnregisterResponse
import fr.gouv.stopc.robertserver.ws.service.IdentityService
import fr.gouv.stopc.robertserver.ws.service.RegistrationService
import fr.gouv.stopc.robertserver.ws.service.RobertCredentials
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class UnregisterController(
    private val identityService: IdentityService,
    private val registrationService: RegistrationService
) : UnregisterApi {

    override fun unregister(unregisterRequest: UnregisterRequest): ResponseEntity<UnregisterResponse> {
        val credentials = RobertCredentials(
            requestType = UNREGISTER,
            ebid = unregisterRequest.ebid.asList(),
            epochId = unregisterRequest.epochId,
            time = unregisterRequest.time.asList(),
            mac = unregisterRequest.mac.asList()
        )
        val idA = identityService.authenticateAndDeleteIdentity(credentials)
        registrationService.unregister(idA)
        return ResponseEntity.ok(
            UnregisterResponse(
                success = true,
                message = null
            )
        )
    }
}
