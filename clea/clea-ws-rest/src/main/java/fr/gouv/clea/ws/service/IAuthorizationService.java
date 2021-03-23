package fr.gouv.clea.ws.service;

import fr.gouv.clea.ws.exception.CleaUnauthorizedException;

public interface IAuthorizationService {

    boolean checkAuthorization(String jwtToken) throws CleaUnauthorizedException;
}
