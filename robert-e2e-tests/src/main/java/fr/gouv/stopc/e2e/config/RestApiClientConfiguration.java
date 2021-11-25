package fr.gouv.stopc.e2e.config;

import fr.gouv.stopc.robert.client.ApiClient;
import fr.gouv.stopc.robert.client.api.CaptchaApi;
import fr.gouv.stopc.robert.client.api.DefaultApi;
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
    public DefaultApi robertApi() {
        return new DefaultApi(apiClient());
    }
}
