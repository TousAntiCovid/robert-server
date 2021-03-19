package fr.gouv.tacw.services;

import fr.gouv.tacw.exceptions.TacWarningUnauthorizedException;

public interface IAuthorizationService {

    boolean checkAuthorization(String jwtToken) throws TacWarningUnauthorizedException;
}
