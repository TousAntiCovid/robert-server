package fr.gouv.stopc.e2e.config;

import fr.gouv.stopc.openapi.client.ApiClient;
import fr.gouv.stopc.openapi.client.captcha.CaptchaApi;
import fr.gouv.stopc.openapi.client.robert.DefaultApi;
import fr.gouv.stopc.openapi.client.robert.RobertApi;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class RestApiClientConfiguration {

    private final ApplicationProperties applicationProperties;

    @Bean
    public ApiClient apiClient() {
        return new ApiClient()
                .setBasePath(applicationProperties.getWsRestBaseUrl() + "/api/v6");
    }

    @Bean
    public CaptchaApi captchaApi() {
        return new CaptchaApi(apiClient());
    }

    @Bean
    public DefaultApi robertLegacyApi() {
        return new DefaultApi(apiClient());
    }

    @Bean
    public RobertApi robertApi() {
        return new RobertApi(apiClient());
    }
}
