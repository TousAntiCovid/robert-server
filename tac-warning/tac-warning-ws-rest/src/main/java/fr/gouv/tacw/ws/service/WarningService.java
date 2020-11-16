package fr.gouv.tacw.ws.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import fr.gouv.tacw.database.service.TokenService;
import fr.gouv.tacw.ws.vo.ExposureStatusRequestVo;
import fr.gouv.tacw.ws.vo.ReportRequestVo;

@Service
public class WarningService {
	@Autowired
	private TokenService tokenService;
	
	public boolean getStatus(ExposureStatusRequestVo statusRequestVo) {
		return statusRequestVo.getVisitTokens().stream()
				.filter(token -> token.getType().isStatic())
				.map(token -> StaticVisitToken.fromVo(token, tokenService))
				.anyMatch(token -> token.isInfected());
	}

	public void reportVisitsWhenInfected(ReportRequestVo reportRequestVo) {
		reportRequestVo.getQrCodes().stream()
			.filter(reportRequest -> reportRequest.getQrCode().getType().isStatic())
			.forEach(reportRequest -> tokenService.registerInfectedStaticToken(reportRequest.getQrCode().getUuid()));
	}
}
