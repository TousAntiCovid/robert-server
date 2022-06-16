package fr.gouv.stopc.robertserver.ws.controller;

import fr.gouv.stopc.robertserver.ws.dto.UnregisterResponseDto;
import fr.gouv.stopc.robertserver.ws.utils.UriConstants;
import fr.gouv.stopc.robertserver.ws.vo.UnregisterRequestVo;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping({ "${controller.path.prefix}" + UriConstants.API_V6 })
public interface IUnregisterController {

    @PostMapping(value = UriConstants.UNREGISTER)
    ResponseEntity<UnregisterResponseDto> unregister(
            @Valid @RequestBody UnregisterRequestVo unregisterRequestVo);

}
