package fr.gouv.tac.analytics.server.controller;

import javax.validation.Validator;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.gouv.tac.analytics.server.service.AnalyticsService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;

@ExtendWith(SpringExtension.class)
public class AnalyticsControllerTest {

    @Mock
    private AnalyticsService analyticsService;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private Validator validator;


    @InjectMocks
    private AnalyticsController analyticsController;

    @Test
    public void shouldCreateAnalytics() throws JsonProcessingException {

        final String analyticsAsString = "";

        final ResponseEntity<Void> result = analyticsController.addAnalytics(analyticsAsString);

        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);

        Mockito.verify(analyticsService).createAnalytics(analyticsAsString);
    }


    //TODO ==> ADD TEST IN CASE VALIDATION ERROR ==> RAISING EXCEPTION
}