package fr.gouv.tacw.ws.service;

import fr.gouv.tacw.ws.exception.TacWarningUnauthorizedException;
import io.jsonwebtoken.SignatureAlgorithm;


public interface AuthorizationService {
	// TODO validate with ANSSI which algo to use, move to proper place;
	SignatureAlgorithm algo = SignatureAlgorithm.RS256;

	boolean checkAuthorization(String jwtToken) throws TacWarningUnauthorizedException;
}
