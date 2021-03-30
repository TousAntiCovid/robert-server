package fr.gouv.tousantic.analytics.server.service;

import fr.gouv.tousantic.analytics.server.model.kafka.Analytics;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.util.concurrent.ListenableFuture;

@ExtendWith(SpringExtension.class)
public class AnalyticsServiceTest {

    @InjectMocks
    private AnalyticsService analyticsService;

    @Mock
    private KafkaTemplate<String, Analytics> kafkaTemplate;

    @Mock
    private ListenableFuture<SendResult<String, Analytics>> listenableFutureMock;


    @Test
    public void shouldCreateAnalyticsWhenNotNull() {


        // Given
        final Analytics analytics = new Analytics();

        Mockito.when(kafkaTemplate.sendDefault(analytics)).thenReturn(listenableFutureMock);

        // When
        this.analyticsService.createAnalytics(analytics);


    }

}