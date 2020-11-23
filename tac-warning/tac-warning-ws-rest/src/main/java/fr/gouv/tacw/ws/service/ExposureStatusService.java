package fr.gouv.tacw.ws.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import fr.gouv.tacw.database.service.TokenService;
import fr.gouv.tacw.model.DynamicToken;
import fr.gouv.tacw.model.StaticToken;
import fr.gouv.tacw.model.Token;


@Service
public class ExposureStatusService {
	@Autowired
	private TokenService tokenService;

	public boolean isExposed(Token token) {
		return token.isExposed(this);
	}

	public boolean isExposed(StaticToken staticTokenVo) {
		return tokenService.exposedStaticTokensIncludes(staticTokenVo.getPayload());
	}

	public boolean isExposed(DynamicToken dynamicTokenVo) {
		// TO IMPLEMENT
		return false;
	}
}
