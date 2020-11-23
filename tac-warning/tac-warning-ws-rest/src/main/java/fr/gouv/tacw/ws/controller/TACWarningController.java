package fr.gouv.tacw.ws.controller;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fr.gouv.tacw.ws.dto.ExposureStatusResponseDto;
import fr.gouv.tacw.ws.dto.ReportResponseDto;
import fr.gouv.tacw.ws.service.AuthorizationService;
import fr.gouv.tacw.ws.service.WarningService;
import fr.gouv.tacw.ws.utils.UriConstants;
import fr.gouv.tacw.ws.vo.ExposureStatusRequestVo;
import fr.gouv.tacw.ws.vo.ReportRequestVo;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping(value = { "${controller.path.prefix}" + UriConstants.API_V1 })
public class TACWarningController {
	@Autowired
	private AuthorizationService authorizationService;

	@Autowired
	private WarningService warningService;

	@PostMapping(value = UriConstants.STATUS)
	protected ExposureStatusResponseDto getStatus(
			@Valid @RequestBody(required = true) ExposureStatusRequestVo statusRequestVo) {
		log.debug("getStatus called");
		boolean atRisk = warningService.getStatus(statusRequestVo);
		return new ExposureStatusResponseDto(atRisk);
	}

	@PostMapping(value = UriConstants.REPORT)
	protected ReportResponseDto reportVisits(@Valid @RequestBody(required = true) ReportRequestVo reportRequestVo,
			@RequestHeader("Authorization") String jwtToken) {
		authorizationService.checkAuthorization(jwtToken);

		log.debug("reportVisits called");
		warningService.reportVisitsWhenInfected(reportRequestVo);
		return new ReportResponseDto(true, "Report successful");
	}
}
