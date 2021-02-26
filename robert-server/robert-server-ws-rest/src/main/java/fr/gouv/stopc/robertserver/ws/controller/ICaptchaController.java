package fr.gouv.stopc.robertserver.ws.controller;

import fr.gouv.stopc.robertserver.ws.dto.CaptchaCreationDto;
import fr.gouv.stopc.robertserver.ws.exception.RobertServerException;
import fr.gouv.stopc.robertserver.ws.utils.UriConstants;
import fr.gouv.stopc.robertserver.ws.vo.CaptchaCreationVo;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;

@RestController
@RequestMapping(value = {"${controller.path.prefix}" + UriConstants.API_V2, 
		"${controller.path.prefix}" + UriConstants.API_V3,
        "${controller.path.prefix}" + UriConstants.API_V4,
		"${controller.path.prefix}" + UriConstants.API_V5})
public interface ICaptchaController {

    @PostMapping(value = UriConstants.CAPTCHA)
    @Consumes(MediaType.APPLICATION_JSON_VALUE)
    @Produces(MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<CaptchaCreationDto> createCaptcha(
            @Valid @RequestBody(required=true) CaptchaCreationVo captchaCreationVo)
            throws RobertServerException;

    @GetMapping(value = UriConstants.CAPTCHA + "/{captchaId}/image", produces = MediaType.IMAGE_PNG_VALUE)
//    @Produces(MediaType.IMAGE_PNG_VALUE)
    ResponseEntity<byte[]> getCaptchaImage(
            @PathVariable("captchaId") String captchaId)
            throws RobertServerException;

    @GetMapping(value = UriConstants.CAPTCHA + "/{captchaId}/audio",produces = "audio/wav")
//    @Produces("audio/wav")
    ResponseEntity<byte[]> getCaptchaAudio(
            @PathVariable("captchaId") String captchaId)
            throws RobertServerException;
}
