package fr.gouv.tacw.ws.controller;

import java.util.Random;
import java.util.stream.Stream;

import javax.validation.Valid;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fr.gouv.tacw.database.utils.TimeUtils;
import fr.gouv.tacw.model.OpaqueVisit;
import fr.gouv.tacw.ws.configuration.TacWarningWsRestConfiguration;
import fr.gouv.tacw.ws.dto.ExposureStatusResponseDto;
import fr.gouv.tacw.ws.dto.ReportResponseDto;
import fr.gouv.tacw.ws.service.AuthorizationService;
import fr.gouv.tacw.ws.service.WarningService;
import fr.gouv.tacw.ws.utils.UriConstants;
import fr.gouv.tacw.ws.vo.ExposureStatusRequestVo;
import fr.gouv.tacw.ws.vo.ReportRequestVo;
import fr.gouv.tacw.ws.vo.mapper.TokenMapper;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping(value = { "${controller.path.prefix}" + UriConstants.API_V2 })
public class TACWarningController {
    private AuthorizationService authorizationService;

    private WarningService warningService;

    private TacWarningWsRestConfiguration configuration;

    private TokenMapper tokenMapper;

    public TACWarningController(AuthorizationService authorizationService, WarningService warningService,
            TacWarningWsRestConfiguration configuration, TokenMapper tokenMapper) {
        super();
        this.authorizationService = authorizationService;
        this.warningService = warningService;
        this.configuration = configuration;
        this.tokenMapper = tokenMapper;
    }

    @PostMapping(value = UriConstants.STATUS)
    protected ExposureStatusResponseDto getStatus(
            @Valid @RequestBody(required = true) ExposureStatusRequestVo statusRequestVo) {
        int nbVisitTokens = statusRequestVo.getVisitTokens().size();
        log.info(String.format("Exposure status request for %d visits", nbVisitTokens));
        int nbRejectedVisitTokens = nbVisitTokens - this.configuration.getMaxVisits();
        if (nbRejectedVisitTokens > 0) {
            log.info(String.format("Filtered out %d visits of %d while Exposure Status Request", nbRejectedVisitTokens, nbVisitTokens));
        }
        Stream<OpaqueVisit> tokens = statusRequestVo.getVisitTokens().stream()
                .limit(this.configuration.getMaxVisits())
                .map(tokenVo -> this.tokenMapper.getToken(tokenVo));		 
        return new ExposureStatusResponseDto(this.warningService.getStatus(tokens), this.randomDateFromLastFiveDays());
    }

    @PostMapping(value = UriConstants.REPORT)
    protected ReportResponseDto reportVisits(@Valid @RequestBody(required = true) ReportRequestVo reportRequestVo,
            @RequestHeader("Authorization") String jwtToken) {
        this.authorizationService.checkAuthorization(jwtToken);
        this.warningService.reportVisitsWhenInfected(reportRequestVo);
        return new ReportResponseDto(true, "Report successful");
    }

    protected String randomDateFromLastFiveDays() {
        int randomInt = new Random().ints(1,6).findFirst().getAsInt();
        return String.valueOf(TimeUtils.roundedCurrentTimeTimestamp() - 86400 * randomInt);
    }
}
