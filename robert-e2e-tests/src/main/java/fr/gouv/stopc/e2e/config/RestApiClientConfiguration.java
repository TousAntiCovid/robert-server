package fr.gouv.stopc.e2e.config;

import fr.gouv.stopc.robert.client.ApiClient;
import fr.gouv.stopc.robert.client.api.CaptchaApi;
import fr.gouv.stopc.robert.client.api.KpiApi;
import fr.gouv.stopc.robert.client.api.RobertApi;
import fr.gouv.stopc.robert.client.api.RobertLegacyApi;
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
    public ApiClient kpiClient() {
        return new ApiClient()
                .setBasePath(applicationProperties.getWsRestBaseUrl() + "/internal/api/v2");
    }

    @Bean
    public CaptchaApi captchaApi() {
        return new CaptchaApi(apiClient());
    }

    @Bean
    public RobertLegacyApi robertLegacyApi() {
        return new RobertLegacyApi(apiClient());
    }

    @Bean
    public RobertApi robertApi() {
        return new RobertApi(apiClient());
    }

    @Bean
    public KpiApi kpiApi() {
        return new KpiApi(kpiClient());
    }
}
