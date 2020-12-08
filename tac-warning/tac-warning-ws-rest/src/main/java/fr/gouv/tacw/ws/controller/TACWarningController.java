package fr.gouv.tacw.ws.controller;

import java.util.stream.Stream;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fr.gouv.tacw.model.OpaqueVisit;
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
@RequestMapping(value = { "${controller.path.prefix}" + UriConstants.API_V1 })
public class TACWarningController {
	@Autowired
	private AuthorizationService authorizationService;

	@Autowired
	private WarningService warningService;

	@Autowired
	TokenMapper tokenMapper;

	@Value("${tacw.rest.score_threshold}")
	private long threshold;
	
	@Value("${tacw.rest.max_visits}")
	private int maxVisits;
		
	@PostMapping(value = UriConstants.STATUS)
	protected ExposureStatusResponseDto getStatus(
			@Valid @RequestBody(required = true) ExposureStatusRequestVo statusRequestVo) {
		int nbVisitTokens = statusRequestVo.getVisitTokens().size();
		log.info(String.format("Exposure status request for %d visits", nbVisitTokens));
		int nbRejectedVisitTokens = nbVisitTokens - maxVisits;
		if (nbRejectedVisitTokens > 0) {
			log.info(String.format("Filtered out %d visits of %d while Exposure Status Request", nbRejectedVisitTokens, nbVisitTokens));
		}
		Stream<OpaqueVisit> tokens = statusRequestVo.getVisitTokens().stream()
				.limit(maxVisits)
				.map(tokenVo -> tokenMapper.getToken(tokenVo));		 
		boolean atRisk = warningService.getStatus(tokens, threshold);
		return new ExposureStatusResponseDto(atRisk);
	}

	@PostMapping(value = UriConstants.REPORT)
	protected ReportResponseDto reportVisits(@Valid @RequestBody(required = true) ReportRequestVo reportRequestVo,
			@RequestHeader("Authorization") String jwtToken) {
		authorizationService.checkAuthorization(jwtToken);
		warningService.reportVisitsWhenInfected(reportRequestVo);
		return new ReportResponseDto(true, "Report successful");
	}
}
