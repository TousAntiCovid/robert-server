package fr.gouv.stopc.robertserver.ws.service.impl;

import fr.gouv.stopc.robertserver.ws.dto.VerifyResponseDto;
import fr.gouv.stopc.robertserver.ws.service.IRestApiService;
import fr.gouv.stopc.robertserver.ws.utils.PropertyLoader;
import fr.gouv.stopc.robertserver.ws.vo.PushInfoVo;
import io.micrometer.core.instrument.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RestApiServiceImpl implements IRestApiService {

    private final PropertyLoader propertyLoader;

    private final RestTemplate restTemplate;

    private final WebClient webClient;

    @Override
    public Optional<VerifyResponseDto> verifyReportToken(String token) {

        if (StringUtils.isEmpty(token)) {
            return Optional.empty();
        }

        try {
            ResponseEntity<VerifyResponseDto> response = restTemplate.getForEntity(
                    buildReportTokenVerificationURI(token),
                    VerifyResponseDto.class
            );

            return Optional.ofNullable(response.getBody());
        } catch (RestClientException e) {
            log.error("Unable to verify the token due to {}", e.getMessage());
        }

        return Optional.empty();
    }

    private URI buildReportTokenVerificationURI(String code) {
        return UriComponentsBuilder.fromUri(this.propertyLoader.getServerCodeUrl())
                .path("/api/v1/verify")
                .queryParam("code", code)
                .build()
                .toUri();
    }

    private URI buildRegistertPushNotifURI() {

        return UriComponentsBuilder.newInstance().scheme("http")
                .host(this.propertyLoader.getPushServerHost())
                .port(this.propertyLoader.getPushServerPort())
                .path(this.propertyLoader.getInternalPathPrefix())
                .path(this.propertyLoader.getPushApiVersion())
                .path(this.propertyLoader.getPushApiPath())
                .build()
                .encode()
                .toUri();
    }

    private URI buildUnregistertPushNotifURI(String pushToken) {

        Map<String, String> parameters = new HashMap<>();
        parameters.put("token", pushToken);

        return UriComponentsBuilder.fromUri(this.buildRegistertPushNotifURI())
                .path(this.propertyLoader.getPushApiTokenPath())
                .build(parameters);
    }

    @Override
    public void registerPushNotif(PushInfoVo pushInfoVo) {

        if (this.isValidPushInfo(pushInfoVo)) {

            Mono<Void> response = this.webClient
                    .post()
                    .uri(this.buildRegistertPushNotifURI())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(pushInfoVo)
                    .retrieve()
                    .bodyToMono(Void.class);

            response.doOnSuccess(t -> log.info("Register to push notification successful"))
                    .doOnError(error -> log.error("Register to push notification failed due to {}", error.getMessage()))
                    .subscribe();
        }
    }

    @Override
    public void unregisterPushNotif(String pushToken) {

        if (StringUtils.isNotBlank(pushToken)) {
            Mono<Void> response = this.webClient
                    .delete()
                    .uri(this.buildUnregistertPushNotifURI(pushToken))
                    .retrieve()
                    .bodyToMono(Void.class);

            response.doOnSuccess(t -> log.info("Unregister to push notification successful"))
                    .doOnError(
                            error -> log.error("Unregister to push notification failed due to {}", error.getMessage())
                    )
                    .subscribe();
        }
    }

    private boolean isValidPushInfo(final PushInfoVo pushInfoVo) {

        if (Objects.isNull(pushInfoVo)) {
            return false;
        }

        if (StringUtils.isBlank(pushInfoVo.getToken())) {
            log.warn("Token is mandatory to register to push notification");
            return false;
        }

        if (StringUtils.isBlank(pushInfoVo.getTimezone())) {
            log.warn("Timezone is mandatory to register to push notification");
            return false;
        }

        if (StringUtils.isBlank(pushInfoVo.getLocale())) {
            log.warn("Locale is mandatory to register to push notification");
            return false;
        }

        return true;
    }
}
