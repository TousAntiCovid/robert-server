package fr.gouv.stopc.robertserver.ws.service

import fr.gouv.stopc.captchaserver.api.CaptchaApi
import fr.gouv.stopc.captchaserver.api.model.CaptchaVerifyRequest
import fr.gouv.stopc.captchaserver.api.model.CaptchaVerifyResponse.ResultEnum.SUCCESS
import fr.gouv.stopc.robertserver.ws.RobertWsProperties
import fr.gouv.stopc.robertserver.ws.common.logger
import org.springframework.stereotype.Service

/**
 * Verifies captcha challenge responses.
 */
@Service
class CaptchaService(
    private val captchaApi: CaptchaApi,
    private val config: RobertWsProperties
) {

    private val log = logger()

    fun verify(id: String, answer: String): Boolean {
        if (!config.captchaServer.enabled) {
            log.warn("Captcha protection is disabled")
            return true
        }
        return try {
            val response = captchaApi.verify(id, CaptchaVerifyRequest().answer(answer))
            response.result == SUCCESS
        } catch (e: Exception) {
            log.info("Captcha server error: ${e.message}")
            false
        }
    }
}
