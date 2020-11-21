package fr.gouv.tacw.ws.service;

import org.springframework.beans.factory.annotation.Autowired;

import fr.gouv.tacw.database.service.TokenService;
import fr.gouv.tacw.ws.controller.TACWarningController;
import fr.gouv.tacw.ws.vo.VisitTokenVo;
import lombok.extern.slf4j.Slf4j;

public class StaticVisitToken {
	@Autowired
	private TokenService tokenService;

	public static StaticVisitToken fromVo(VisitTokenVo vo, TokenService tokenService) {
		return new StaticVisitToken(vo.getPayload(), tokenService);
	}

	private final String payload;

	public StaticVisitToken(String payload, TokenService tokenService) {
		this.payload = payload;
		this.tokenService = tokenService;
	}

	protected boolean isInfected() {
		return tokenService.exposedStaticTokensIncludes(this.payload);
	}
}
