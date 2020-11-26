package fr.gouv.tacw.ws.service;

import fr.gouv.tacw.model.DynamicToken;
import fr.gouv.tacw.model.StaticToken;
import fr.gouv.tacw.model.Token;


public interface ExposureStatusService {
	public boolean isExposed(Token token);
	public boolean isExposed(StaticToken staticTokenVo);
	public boolean isExposed(DynamicToken dynamicTokenVo);
}
