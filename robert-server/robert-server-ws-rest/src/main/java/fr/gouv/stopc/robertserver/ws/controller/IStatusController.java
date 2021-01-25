package fr.gouv.stopc.robertserver.ws.controller;

import javax.validation.Valid;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fr.gouv.stopc.robertserver.ws.dto.StatusResponseDto;
import fr.gouv.stopc.robertserver.ws.dto.StatusResponseDtoV1ToV4;
import fr.gouv.stopc.robertserver.ws.exception.RobertServerException;
import fr.gouv.stopc.robertserver.ws.utils.UriConstants;
import fr.gouv.stopc.robertserver.ws.vo.StatusVo;

@RestController
@RequestMapping(path = "${controller.path.prefix}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
public interface IStatusController {

    @PostMapping(path = { UriConstants.API_V1 + UriConstants.STATUS, UriConstants.API_V2 + UriConstants.STATUS,
            UriConstants.API_V3 + UriConstants.STATUS, UriConstants.API_V4 + UriConstants.STATUS })
    ResponseEntity<StatusResponseDtoV1ToV4> getStatusV1ToV4(@Valid @RequestBody(required = true) StatusVo statusVo)
            throws RobertServerException;

    @PostMapping(path = UriConstants.API_V5 + UriConstants.STATUS)
    ResponseEntity<StatusResponseDto> getStatus(@Valid @RequestBody(required = true) StatusVo statusVo)
            throws RobertServerException;

}
