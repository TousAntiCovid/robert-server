package fr.gouv.tacw.ws.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import fr.gouv.tacw.database.service.TokenService;
import fr.gouv.tacw.ws.vo.ExposureStatusRequestVo;

@Service
public class WarningService {
	@Autowired
	private TokenService tokenService;
	
	public boolean getStatus(ExposureStatusRequestVo statusRequestVo) {
		return statusRequestVo.getVisitTokens().stream()
				.filter(token -> token.isStatic())
				.map(token -> StaticVisitToken.fromVo(token, tokenService))
				.anyMatch(token -> token.isInfected());
	}
}
