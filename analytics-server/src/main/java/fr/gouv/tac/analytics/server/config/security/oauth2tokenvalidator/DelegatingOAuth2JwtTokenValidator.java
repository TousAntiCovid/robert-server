package fr.gouv.tac.analytics.server.config.security.oauth2tokenvalidator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

public class DelegatingOAuth2JwtTokenValidator implements OAuth2TokenValidator<Jwt> {

    private final List<OAuth2TokenValidator<Jwt>> oAuth2TokenValidators;

    @SafeVarargs
    public DelegatingOAuth2JwtTokenValidator(final OAuth2TokenValidator<Jwt>... tokenValidators) {
        oAuth2TokenValidators = Arrays.asList(tokenValidators);
    }

    @Override
    public OAuth2TokenValidatorResult validate(final Jwt token) {
        final Collection<OAuth2Error> errors = new ArrayList<>();

        final Iterator<OAuth2TokenValidator<Jwt>> oAuth2TokenValidatorIterator = oAuth2TokenValidators.iterator();

        while (oAuth2TokenValidatorIterator.hasNext() && errors.isEmpty()) {
            final OAuth2TokenValidator<Jwt> oAuth2TokenValidator = oAuth2TokenValidatorIterator.next();
            errors.addAll(oAuth2TokenValidator.validate(token).getErrors());
        }

        return OAuth2TokenValidatorResult.failure(errors);
    }
}
