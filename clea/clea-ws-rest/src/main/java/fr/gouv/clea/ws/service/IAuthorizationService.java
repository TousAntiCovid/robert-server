package fr.gouv.clea.ws.service;

import fr.gouv.clea.ws.exception.TacWarningUnauthorizedException;

public interface IAuthorizationService {

    boolean checkAuthorization(String jwtToken) throws TacWarningUnauthorizedException;
}
