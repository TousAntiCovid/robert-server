package fr.gouv.tac.analytics.server.config.security.oauth2tokenvalidator;

import java.util.Objects;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class ExpirationTokenPresenceOAuth2TokenValidator implements OAuth2TokenValidator<Jwt> {

    public static final String ERR_MESSAGE = "The token expiration (exp) is missing";
    static final OAuth2Error EXPIRATION_NOT_FOUND_OAUTH2ERROR = new OAuth2Error("invalid_token", ERR_MESSAGE, null);

    private static final OAuth2TokenValidatorResult FAILURE_RESULT = OAuth2TokenValidatorResult.failure(EXPIRATION_NOT_FOUND_OAUTH2ERROR);
    private static final OAuth2TokenValidatorResult SUCCESS_RESULT = OAuth2TokenValidatorResult.success();


    @Override
    public OAuth2TokenValidatorResult validate(final Jwt jwt) {
        if (Objects.isNull(jwt.getExpiresAt())) {
            return FAILURE_RESULT;
        }
        return SUCCESS_RESULT;
    }
}
