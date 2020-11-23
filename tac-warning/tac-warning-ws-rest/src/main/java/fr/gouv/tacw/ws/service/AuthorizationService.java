package fr.gouv.tacw.ws.service;

import fr.gouv.tacw.ws.exception.TacWarningUnauthorizedException;


public interface AuthorizationService {
	boolean checkAuthorization(String jwtToken) throws TacWarningUnauthorizedException;
}
