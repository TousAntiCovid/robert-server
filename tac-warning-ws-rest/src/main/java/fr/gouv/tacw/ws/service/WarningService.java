package fr.gouv.tacw.ws.service;

import org.springframework.stereotype.Service;

import fr.gouv.tacw.ws.vo.ExposureStatusRequestVo;

@Service
public class WarningService {
	public boolean getStatus(ExposureStatusRequestVo statusRequestVo) {
		return statusRequestVo.getVisitTokens().stream()
				.filter(token -> token.isStatic())
				.map(StaticVisitToken::fromVo)
				.anyMatch(token -> token.isInfected());
	}
}
