package fr.gouv.stopc.robertserver.ws.service.impl;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import fr.gouv.stopc.robertserver.ws.service.CaptchaService;
import fr.gouv.stopc.robertserver.ws.utils.PropertyLoader;
import fr.gouv.stopc.robertserver.ws.vo.RegisterVo;
import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
public class CaptchaServiceImpl implements CaptchaService {

    private final boolean captchaIsDisabled;

    private final RestTemplate restTemplate;

    private final String expectedSuccessResultValue;

    public CaptchaServiceImpl(final RestTemplateBuilder restTemplateBuilder, final PropertyLoader propertyLoader) {
        captchaIsDisabled = propertyLoader.getDisableCaptcha();
        final var baseUrl = propertyLoader.getCaptchaVerificationUrl().replaceAll("/api/v1/.*", "");
        restTemplate = restTemplateBuilder
                .rootUri(baseUrl)
                .build();
        expectedSuccessResultValue = propertyLoader.getCaptchaSuccessCode();
    }

    @Override
    public boolean verifyCaptcha(final RegisterVo registerVo) {
        if (captchaIsDisabled) {
            log.warn("Captcha verification is disabled");
            return true;
        }

        if (null == registerVo || null == registerVo.getCaptcha() || null == registerVo.getCaptchaId()) {
            log.info("Incomplete captcha verification informations");
            return false;
        }

        final var verificationRequest = new CaptchaVerificationRequest(registerVo.getCaptcha());
        try {
            final var verificationResponse = restTemplate.postForObject(
                    "/api/v1/captcha/{captchaId}/checkAnswer",
                    verificationRequest, CaptchaVerificationResponse.class, registerVo.getCaptchaId()
            );
            if (null != verificationResponse) {
                return expectedSuccessResultValue.equals(verificationResponse.getResult());
            }
        } catch (HttpClientErrorException e) {
            log.info(
                    "Captcha endpoint returned a client error status code, this means the captcha validation request attributes are invalid",
                    e
            );
        } catch (RestClientException e) {
            log.error("Captcha endpoint returned a server error status code, check captcha service logs", e);
        }
        return false;
    }

    @Value
    private static class CaptchaVerificationRequest {

        String answer;
    }

    @Value
    @Builder(setterPrefix = "with")
    @JsonDeserialize(builder = CaptchaVerificationResponse.CaptchaVerificationResponseBuilder.class)
    private static class CaptchaVerificationResponse {

        String result;
    }
}
