package fr.gouv.stopc.robertserver.ws.service

import fr.gouv.stopc.robertserver.common.logger
import fr.gouv.stopc.submissioncode.api.SubmissionCodeApi
import org.springframework.stereotype.Service

/**
 * Verifies codes used for /report requests.
 */
@Service
class SubmissionCodeService(private val submissionCodeApi: SubmissionCodeApi) {

    private val log = logger()

    fun verify(code: String): Boolean {
        return try {
            val response = submissionCodeApi.verify(code, null)
            if (!response.valid) {
                log.info("Invalid report token of length ${code.length}")
            }
            return response.valid
        } catch (e: Exception) {
            log.info("Submission code server error: ${e.message}")
            false
        }
    }
}
