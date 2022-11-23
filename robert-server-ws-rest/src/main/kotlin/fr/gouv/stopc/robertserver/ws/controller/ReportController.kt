package fr.gouv.stopc.robertserver.ws.controller

import fr.gouv.stopc.robertserver.ws.api.ReportApi
import fr.gouv.stopc.robertserver.ws.api.model.ReportBatchRequest
import fr.gouv.stopc.robertserver.ws.api.model.ReportBatchResponse
import fr.gouv.stopc.robertserver.ws.service.JwtService
import fr.gouv.stopc.robertserver.ws.service.ReportContactsService
import fr.gouv.stopc.robertserver.ws.service.SubmissionCodeService
import org.springframework.http.HttpStatus.UNAUTHORIZED
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class ReportController(
    private val submissionCodeService: SubmissionCodeService,
    private val reportContactsService: ReportContactsService,
    private val jwtService: JwtService
) : ReportApi {

    override fun reportBatch(reportBatchRequest: ReportBatchRequest): ResponseEntity<ReportBatchResponse> {
        return if (submissionCodeService.verify(reportBatchRequest.token)) {
            reportContactsService.report(reportBatchRequest.contacts)
            ResponseEntity.ok(
                ReportBatchResponse(
                    success = true,
                    message = "Successful operation",
                    reportValidationToken = jwtService.generateReportValidationToken()
                )
            )
        } else {
            ResponseEntity.status(UNAUTHORIZED).build()
        }
    }
}
