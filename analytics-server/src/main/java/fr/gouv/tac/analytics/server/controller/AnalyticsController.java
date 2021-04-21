package fr.gouv.tac.analytics.server.controller;

import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validator;
import javax.validation.constraints.NotEmpty;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.gouv.tac.analytics.server.controller.vo.AnalyticsVo;
import fr.gouv.tac.analytics.server.service.AnalyticsService;
import fr.gouv.tac.analytics.server.utils.UriConstants;


@RestController
@RequestMapping(value = "${analyticsserver.controller.path.prefix}",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    public AnalyticsController(AnalyticsService analyticsService, ObjectMapper objectMapper, Validator validator) {
        this.analyticsService = analyticsService;
        this.objectMapper = objectMapper;
        this.validator = validator;
    }

    @PostMapping(path = UriConstants.API_V1 + UriConstants.ANALYTICS)
    public ResponseEntity<Void> addAnalytics(@NotEmpty @RequestBody String analyticsAsJson) throws JsonProcessingException {

        AnalyticsVo analyticsVo = objectMapper.readValue(analyticsAsJson, AnalyticsVo.class);

        Set<ConstraintViolation<AnalyticsVo>> constraintViolationSet = validator.validate(analyticsVo);
        if (! constraintViolationSet.isEmpty()) {
            throw new ConstraintViolationException(constraintViolationSet);
        }

        analyticsService.createAnalytics(analyticsAsJson);
        return ResponseEntity.ok().build();
    }

}
