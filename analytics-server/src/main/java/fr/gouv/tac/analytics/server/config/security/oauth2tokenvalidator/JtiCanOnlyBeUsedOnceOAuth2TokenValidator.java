package fr.gouv.tac.analytics.server.config.security.oauth2tokenvalidator;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import javax.inject.Inject;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import fr.gouv.tac.analytics.server.service.TokenIdentifierService;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class JtiCanOnlyBeUsedOnceOAuth2TokenValidator implements OAuth2TokenValidator<Jwt> {

    public static final String ERR_MESSAGE = "The token identifier (jti) can only be used once";
    static final OAuth2Error JTI_USED_MORE_THAN_ONCE_OAUTH2ERROR = new OAuth2Error("invalid_token", ERR_MESSAGE, null);

    private static final ZoneId UTC_ZONEID = ZoneId.of("UTC");

    private static final OAuth2TokenValidatorResult FAILURE_RESULT = OAuth2TokenValidatorResult.failure(JTI_USED_MORE_THAN_ONCE_OAUTH2ERROR);
    private static final OAuth2TokenValidatorResult SUCCESS_RESULT = OAuth2TokenValidatorResult.success();

    private final TokenIdentifierService tokenIdentifierService;

    @Override
    public OAuth2TokenValidatorResult validate(final Jwt jwt) {

        final String jti = jwt.getId();

        if (!tokenIdentifierService.tokenIdentifierExist(jti)) {
            final ZonedDateTime expirationDate = jwt.getExpiresAt().atZone(UTC_ZONEID);
            tokenIdentifierService.save(jti, expirationDate);
            return SUCCESS_RESULT;
        }

        return FAILURE_RESULT;
    }
}
