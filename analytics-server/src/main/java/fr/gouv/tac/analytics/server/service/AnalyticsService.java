package fr.gouv.tac.analytics.server.service;

import javax.inject.Inject;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFutureCallback;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class AnalyticsService {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public void createAnalytics(final String analyticsAsJson) {
        kafkaTemplate.sendDefault(analyticsAsJson).addCallback(new ListenableFutureCallback<>() {
            @Override
            public void onFailure(final Throwable throwable) {
                log.warn("Error sending message to kafka", throwable);
            }

            @Override
            public void onSuccess(final SendResult<String, String> sendResult) {
                log.debug("Message successfully sent {}", sendResult);
            }
        });
    }
}
