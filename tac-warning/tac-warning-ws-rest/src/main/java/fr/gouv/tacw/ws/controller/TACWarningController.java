package fr.gouv.tacw.ws.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fr.gouv.tacw.ws.dto.ExposureStatusResponseDto;
import fr.gouv.tacw.ws.service.WarningService;
import fr.gouv.tacw.ws.utils.UriConstants;
import fr.gouv.tacw.ws.vo.ReportRequestVo;
import fr.gouv.tacw.ws.vo.ExposureStatusRequestVo;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping(value = { "${controller.path.prefix}" + UriConstants.API_V1 })
public class TACWarningController {

	@Autowired
	private WarningService warningService;
	
	@PostMapping(value = UriConstants.STATUS)
	protected ExposureStatusResponseDto getStatus(@RequestBody(required = true) ExposureStatusRequestVo statusRequestVo) {
		log.debug("getStatus called");
		boolean atRisk = warningService.getStatus(statusRequestVo);
		return new ExposureStatusResponseDto(atRisk);
	}

	@PostMapping(value = UriConstants.REPORT)
	protected void reportEnclosedAreaQRcodes(@RequestBody(required = true) ReportRequestVo reportRequestVo) {
		log.debug("reportEnclosedAreaQRcodes called");
	}
}
