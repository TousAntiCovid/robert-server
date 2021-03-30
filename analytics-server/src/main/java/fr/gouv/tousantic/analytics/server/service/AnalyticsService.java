package fr.gouv.tousantic.analytics.server.service;

import fr.gouv.tousantic.analytics.server.model.kafka.Analytics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFutureCallback;

import javax.inject.Inject;

@Slf4j
@Service
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class AnalyticsService {

    private final KafkaTemplate<String, Analytics> kafkaTemplate;

    public void createAnalytics(final Analytics analytics) {
        kafkaTemplate.sendDefault(analytics).addCallback(new ListenableFutureCallback<>() {
            @Override
            public void onFailure(final Throwable throwable) {
                log.warn("Error sending message to kafka", throwable);
            }

            @Override
            public void onSuccess(final SendResult<String, Analytics> sendResult) {
                log.debug("Message successfully sent {}", sendResult);
            }
        });
    }
}
