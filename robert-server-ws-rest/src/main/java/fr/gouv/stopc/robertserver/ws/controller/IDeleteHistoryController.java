package fr.gouv.stopc.robertserver.ws.controller;

import fr.gouv.stopc.robertserver.ws.dto.DeleteHistoryResponseDto;
import fr.gouv.stopc.robertserver.ws.exception.RobertServerException;
import fr.gouv.stopc.robertserver.ws.utils.UriConstants;
import fr.gouv.stopc.robertserver.ws.vo.DeleteHistoryRequestVo;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping({ "${controller.path.prefix}" + UriConstants.API_V6 })
public interface IDeleteHistoryController {

    @PostMapping(value = UriConstants.DELETE_HISTORY)
    ResponseEntity<DeleteHistoryResponseDto> deleteHistory(
            @Valid @RequestBody DeleteHistoryRequestVo deleteExposureRequestVo)
            throws RobertServerException;

}
