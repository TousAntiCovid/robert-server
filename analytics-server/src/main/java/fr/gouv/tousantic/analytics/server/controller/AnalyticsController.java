package fr.gouv.tousantic.analytics.server.controller;

import fr.gouv.tousantic.analytics.server.service.AnalyticsService;
import fr.gouv.tousantic.analytics.server.controller.mapper.AnalyticsMapper;
import fr.gouv.tousantic.analytics.server.controller.vo.AnalyticsVo;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.inject.Inject;
import javax.validation.Valid;


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
