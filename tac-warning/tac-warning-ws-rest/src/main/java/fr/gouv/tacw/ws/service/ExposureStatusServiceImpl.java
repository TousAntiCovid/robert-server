package fr.gouv.tacw.ws.service;

import org.springframework.stereotype.Service;

import fr.gouv.tacw.database.service.TokenService;
import fr.gouv.tacw.model.DynamicToken;
import fr.gouv.tacw.model.StaticToken;
import fr.gouv.tacw.model.Token;


@Service
public class ExposureStatusServiceImpl implements ExposureStatusService {
	private TokenService tokenService;

	public ExposureStatusServiceImpl(TokenService tokenService) {
		super();
		this.tokenService = tokenService;
	}

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
