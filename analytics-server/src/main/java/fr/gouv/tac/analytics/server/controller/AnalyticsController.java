package fr.gouv.tac.analytics.server.controller;

import javax.inject.Inject;
import javax.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fr.gouv.tac.analytics.server.controller.mapper.AnalyticsMapper;
import fr.gouv.tac.analytics.server.controller.vo.AnalyticsVo;
import fr.gouv.tac.analytics.server.service.AnalyticsService;
import lombok.RequiredArgsConstructor;


@RestController
@RequestMapping(value = "${analyticsserver.controller.analytics.path}")
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final AnalyticsMapper analyticsMapper;

    @PostMapping
    public ResponseEntity<Void> addAnalytics(@Valid @RequestBody final AnalyticsVo analyticsVo) {
        analyticsMapper.map(analyticsVo).ifPresent(analyticsService::createAnalytics);
        return ResponseEntity.ok().build();
    }

}