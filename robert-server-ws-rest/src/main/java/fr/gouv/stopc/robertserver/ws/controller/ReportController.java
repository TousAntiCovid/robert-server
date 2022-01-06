package fr.gouv.stopc.robertserver.ws.controller;

import fr.gouv.stopc.robertserver.ws.dto.ReportBatchResponseDto;
import fr.gouv.stopc.robertserver.ws.exception.RobertServerException;
import fr.gouv.stopc.robertserver.ws.service.ContactDtoService;
import fr.gouv.stopc.robertserver.ws.utils.MessageConstants;
import fr.gouv.stopc.robertserver.ws.utils.UriConstants;
import fr.gouv.stopc.robertserver.ws.vo.ReportBatchRequestVo;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;

@RestController
@RequiredArgsConstructor
@RequestMapping(value = {"${controller.path.prefix}" + UriConstants.API_V2,
        "${controller.path.prefix}" + UriConstants.API_V3
})
@Consumes(MediaType.APPLICATION_JSON_VALUE)
@Produces(MediaType.APPLICATION_JSON_VALUE)
public class ReportController {

    private final ReportControllerDelegate delegate;

    private final ContactDtoService contactDtoService;

    @PostMapping(value = UriConstants.REPORT)
    public ResponseEntity<ReportBatchResponseDto> reportContactHistory(ReportBatchRequestVo reportBatchRequestVo)
            throws RobertServerException {

        if (!delegate.isReportRequestValid(reportBatchRequestVo)) {
            return ResponseEntity.badRequest().build();
        }

        contactDtoService.saveContacts(reportBatchRequestVo.getContacts());

        ReportBatchResponseDto reportBatchResponseDto = ReportBatchResponseDto.builder()
                .message(MessageConstants.SUCCESSFUL_OPERATION.getValue())
                .success(Boolean.TRUE)
                .build();

        return ResponseEntity.ok(reportBatchResponseDto);
    }
}
