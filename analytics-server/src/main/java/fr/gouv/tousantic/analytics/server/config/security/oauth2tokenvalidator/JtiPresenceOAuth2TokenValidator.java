package fr.gouv.tousantic.analytics.server.config.security.oauth2tokenvalidator;

import org.apache.commons.lang3.StringUtils;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class JtiPresenceOAuth2TokenValidator implements OAuth2TokenValidator<Jwt> {

    static final OAuth2Error JTI_NOT_FOUND_OAUTH2ERROR = new OAuth2Error("invalid_token", "The token identifier (jti) is missing", null);

    private static final OAuth2TokenValidatorResult FAILURE_RESULT = OAuth2TokenValidatorResult.failure(JTI_NOT_FOUND_OAUTH2ERROR);
    private static final OAuth2TokenValidatorResult SUCCESS_RESULT = OAuth2TokenValidatorResult.success();


    @Override
    public OAuth2TokenValidatorResult validate(final Jwt jwt) {
        if (StringUtils.isEmpty(jwt.getId())) {
            return FAILURE_RESULT;
        }
        return SUCCESS_RESULT;
    }
}
