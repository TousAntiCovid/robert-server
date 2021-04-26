package fr.gouv.clea.ws.service;

import fr.gouv.clea.ws.exception.CleaForbiddenException;

public interface IAuthorizationService {

    boolean checkAuthorization(String jwtToken) throws CleaForbiddenException;
}
