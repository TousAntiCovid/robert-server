package fr.gouv.stopc.robertserver.ws.controller.impl;

import fr.gouv.stopc.robertserver.ws.config.RobertWsProperties;
import fr.gouv.stopc.robertserver.ws.controller.ICaptchaController;
import fr.gouv.stopc.robertserver.ws.dto.CaptchaCreationDto;
import fr.gouv.stopc.robertserver.ws.exception.RobertServerException;
import fr.gouv.stopc.robertserver.ws.vo.CaptchaCreationVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import javax.validation.Valid;

import java.net.URI;
import java.util.HashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class CaptchaControllerImpl implements ICaptchaController {

    private final RestTemplate restTemplate;

    private final RobertWsProperties robertWsProperties;

    @Override
    public ResponseEntity<CaptchaCreationDto> createCaptcha(
            @Valid CaptchaCreationVo captchaCreationVo) throws RobertServerException {

        ResponseEntity<CaptchaCreationDto> response = null;
        try {
            response = restTemplate.postForEntity(
                    UriComponentsBuilder.fromHttpUrl(
                            robertWsProperties.getCaptcha().getPrivateBaseUrl() + "/captcha"
                    ).build().toUri(),
                    captchaCreationVo,
                    CaptchaCreationDto.class
            );
            log.info("Captcha creation response: {}", response);
        } catch (RestClientException e) {
            log.error(
                    "Could not create captcha with type {} and locale {}; {}",
                    captchaCreationVo.getType(),
                    captchaCreationVo.getLocale(),
                    e.getMessage()
            );
            return ResponseEntity.badRequest().build();
        }
        return response;
    }

    @Override
    public ResponseEntity<byte[]> getCaptchaImage(String captchaId) throws RobertServerException {
        return this.getCaptchaCommon(captchaId, "image");
    }

    @Override
    public ResponseEntity<byte[]> getCaptchaAudio(String captchaId) throws RobertServerException {
        return this.getCaptchaCommon(captchaId, "audio");
    }

    private ResponseEntity<byte[]> getCaptchaCommon(String captchaId, String mediaType) {
        log.info("Getting captcha {} as {}", captchaId, mediaType);
        HashMap<String, String> uriVariables = new HashMap<String, String>();
        uriVariables.put("captchaId", captchaId);

        try {
            URI uri = UriComponentsBuilder.fromHttpUrl(
                    robertWsProperties.getCaptcha().getPublicBaseUrl() + "/captcha/{captchaId}."
                            + (mediaType == "audio" ? "wav" : "png")
            )
                    .build(uriVariables);

            log.info("Getting captcha from URL: {}", uri);

            RestTemplate restTemplate = new RestTemplate();
            RequestEntity<Void> request = RequestEntity
                    .get(uri)
                    .accept(mediaType == "image" ? MediaType.IMAGE_PNG : MediaType.ALL)
                    .build();
            ResponseEntity<byte[]> response = restTemplate.exchange(request, byte[].class);

            // ResponseEntity<Object> response = restTemplate.getForEntity(uri,
            // Object.class);
            // ResponseEntity<Object> response = restTemplate.exchange(uri, HttpMethod.GET,
            // entity, Object.class);
            log.info("Captcha resource access response: {}", response);
            return response;
        } catch (RestClientException e) {
            log.error("Could not get resource type {} for captcha {}; {}", mediaType, captchaId, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
}
