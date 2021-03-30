package fr.gouv.tousantic.analytics.server.controller;

import fr.gouv.tousantic.analytics.server.service.AnalyticsService;
import fr.gouv.tousantic.analytics.server.controller.mapper.AnalyticsMapper;
import fr.gouv.tousantic.analytics.server.controller.vo.AnalyticsVo;
import fr.gouv.tousantic.analytics.server.model.kafka.Analytics;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Optional;

@ExtendWith(SpringExtension.class)
public class AnalyticsControllerTest {

    @Mock
    private AnalyticsService analyticsService;

    @Mock
    private AnalyticsMapper analyticsMapper;

    @InjectMocks
    private AnalyticsController analyticsController;

    @Test
    public void shouldCreateAnalytics() {

        final AnalyticsVo analyticsVo = new AnalyticsVo();
        final Optional<Analytics> analytics = Optional.of(new Analytics());

        Mockito.when(analyticsMapper.map(analyticsVo)).thenReturn(analytics);

        final ResponseEntity<Void> result = analyticsController.addAnalytics(analyticsVo);

        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);

        Mockito.verify(analyticsService).createAnalytics(analytics.get());
    }
}