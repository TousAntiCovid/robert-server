package fr.gouv.stopc.robertserver.ws.controller

import fr.gouv.stopc.robertserver.common.RobertClock
import fr.gouv.stopc.robertserver.ws.api.RegisterApi
import fr.gouv.stopc.robertserver.ws.api.model.RegisterRequest
import fr.gouv.stopc.robertserver.ws.api.model.RegisterSuccessResponse
import fr.gouv.stopc.robertserver.ws.common.base64Decode
import fr.gouv.stopc.robertserver.ws.service.CaptchaService
import fr.gouv.stopc.robertserver.ws.service.IdentityService
import fr.gouv.stopc.robertserver.ws.service.RegistrationService
import org.springframework.http.HttpStatus.CREATED
import org.springframework.http.HttpStatus.UNAUTHORIZED
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class RegisterController(
    private val clock: RobertClock,
    private val captchaService: CaptchaService,
    private val identityService: IdentityService,
    private val registrationService: RegistrationService
) : RegisterApi {

    override suspend fun register(registerRequest: RegisterRequest): ResponseEntity<RegisterSuccessResponse> {
        return if (captchaService.verify(registerRequest.captchaId, registerRequest.captcha)) {
            val tuplesBundle = identityService.register(registerRequest.clientPublicECDHKey.base64Decode())
            registrationService.register(tuplesBundle.idA, registerRequest.pushInfo)
            ResponseEntity.status(CREATED)
                .body(
                    RegisterSuccessResponse(
                        tuples = tuplesBundle.encryptedTuples.toByteArray(),
                        timeStart = clock.atEpoch(0).asNtpTimestamp(),
                        config = listOf()
                    )
                )
        } else {
            ResponseEntity.status(UNAUTHORIZED).build()
        }
    }
}
