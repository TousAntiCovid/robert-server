package fr.gouv.tacw.ws.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fr.gouv.tacw.ws.utils.UriConstants;
import fr.gouv.tacw.ws.vo.ReportRequestVo;
import fr.gouv.tacw.ws.vo.StatusRequestVo;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping(value = { "${controller.path.prefix}" + UriConstants.API_V1 })
public class TACWarningController {

	@PostMapping(value = UriConstants.STATUS)
	void getStatus(@RequestBody(required = true) StatusRequestVo statusRequestVo) {
		log.debug("getStatus called");
	}

	@PostMapping(value = UriConstants.REPORT)
	void reportEnclosedAreaQRcodes(@RequestBody(required = true) ReportRequestVo reportRequestVo) {
		log.debug("reportEnclosedAreaQRcodes called");
	}
}
