package test.fr.gouv.stopc.robertserver.ws.service.impl;

import fr.gouv.stopc.robertserver.ws.config.RobertWsProperties;
import fr.gouv.stopc.robertserver.ws.config.RobertWsProperties.Captcha;
import fr.gouv.stopc.robertserver.ws.service.impl.CaptchaServiceImpl;
import fr.gouv.stopc.robertserver.ws.vo.RegisterVo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.client.MockServerRestTemplateCustomizer;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.test.web.client.MockRestServiceServer;

import java.net.MalformedURLException;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class CaptchaServiceImplTest {

    private CaptchaServiceImpl captchaService;

    private RegisterVo registerVo;

    private MockRestServiceServer mockServer;

    @BeforeEach
    void beforeEach() throws MalformedURLException {
        final var captchaConfig = new Captcha(
                true, new URL("http://localhost/public/api/v1"), new URL("http://localhost/private/api/v1"), "SUCCESS"
        );
        final var config = new RobertWsProperties(captchaConfig, 5);

        final var mockServerCustomizer = new MockServerRestTemplateCustomizer();
        captchaService = new CaptchaServiceImpl(new RestTemplateBuilder(mockServerCustomizer), config);
        mockServer = mockServerCustomizer.getServer();

        registerVo = RegisterVo.builder().captcha("captcha").captchaId("captchaId").build();
    }

    @Test
    void null_registerVo_should_result_in_unverified_captcha() {
        // When
        boolean isVerified = captchaService.verifyCaptcha(null);

        // Then
        assertFalse(isVerified);
    }

    @Test
    void registerVo_with_null_captcha_should_result_in_unverified_captcha() {
        // Given
        registerVo.setCaptcha(null);

        // When
        boolean isVerified = captchaService.verifyCaptcha(registerVo);

        // Then
        assertFalse(isVerified);
    }

    @Test
    void valid_captcha_challenge_response_should_result_in_successfully_verified_captcha() {

        // Given
        mockServer.expect(requestTo("/captcha/captchaId/checkAnswer"))
                .andRespond(
                        withSuccess()
                                .body("{ \"result\": \"SUCCESS\" }")
                                .contentType(APPLICATION_JSON)
                );

        // When
        boolean isVerified = captchaService.verifyCaptcha(registerVo);

        // Then
        assertTrue(isVerified);
    }

    @Test
    void incorrect_captcha_challenge_response_should_result_in_unverified_captcha() {

        // Given
        mockServer.expect(requestTo("/captcha/captchaId/checkAnswer"))
                .andRespond(
                        withSuccess()
                                .body("{ \"result\": \"FAILED\" }")
                                .contentType(APPLICATION_JSON)
                );

        // When
        boolean isVerified = captchaService.verifyCaptcha(registerVo);

        // Then
        assertFalse(isVerified);
    }

    @Test
    void status_400_api_response_should_result_in_unverified_captcha() {

        // Given
        mockServer.expect(requestTo("/captcha/captchaId/checkAnswer"))
                .andRespond(withBadRequest());

        // When
        boolean isVerified = captchaService.verifyCaptcha(registerVo);

        // Then
        assertFalse(isVerified);
    }

    @Test
    void status_500_api_response_should_result_in_unverified_captcha() {

        // Given
        mockServer.expect(requestTo("/captcha/captchaId/checkAnswer"))
                .andRespond(withServerError());

        // When
        boolean isVerified = captchaService.verifyCaptcha(registerVo);

        // Then
        assertFalse(isVerified);
    }

    @Test
    void unexisting_captcha_id_should_result_in_unverified_captcha() {

        // Given
        mockServer.expect(requestTo("/captcha/captchaId/checkAnswer"))
                .andRespond(
                        withStatus(NOT_FOUND)
                                .body(
                                        "{"
                                                + "    \"code\": \"0002\","
                                                + "    \"message\": \"The captcha does not exist\""
                                                + "}"
                                )
                                .contentType(APPLICATION_JSON)
                );

        // When
        boolean isVerified = captchaService.verifyCaptcha(registerVo);

        // Then
        assertFalse(isVerified);
    }

}
