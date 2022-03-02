package fr.gouv.stopc.robertserver.ws.controller;

import javax.validation.Valid;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fr.gouv.stopc.robertserver.ws.dto.CaptchaCreationDto;
import fr.gouv.stopc.robertserver.ws.exception.RobertServerException;
import fr.gouv.stopc.robertserver.ws.utils.UriConstants;
import fr.gouv.stopc.robertserver.ws.vo.CaptchaCreationVo;

@RestController
@RequestMapping(value = {"${controller.path.prefix}" + UriConstants.API_V2, 
		"${controller.path.prefix}" + UriConstants.API_V3,
		"${controller.path.prefix}" + UriConstants.API_V4,
        "${controller.path.prefix}" + UriConstants.API_V5,
        "${controller.path.prefix}" + UriConstants.API_V6})
public interface ICaptchaController {

    @PostMapping(value = UriConstants.CAPTCHA)
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
