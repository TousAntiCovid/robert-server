package fr.gouv.stopc.robertserver.ws.service.impl;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import fr.gouv.stopc.robertserver.ws.config.RobertWsProperties;
import fr.gouv.stopc.robertserver.ws.service.CaptchaService;
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

    private final String expectedSuccessResultCode;

    public CaptchaServiceImpl(final RestTemplateBuilder restTemplateBuilder,
            final RobertWsProperties robertWsProperties) {
        captchaIsDisabled = !robertWsProperties.getCaptcha().isEnabled();
        restTemplate = restTemplateBuilder
                .rootUri(robertWsProperties.getCaptcha().getPrivateBaseUrl().toString())
                .build();
        expectedSuccessResultCode = robertWsProperties.getCaptcha().getSuccessCode();
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
                    "/captcha/{captchaId}/checkAnswer",
                    verificationRequest, CaptchaVerificationResponse.class, registerVo.getCaptchaId()
            );
            if (null != verificationResponse) {
                return expectedSuccessResultCode.equals(verificationResponse.getResult());
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
