package fr.gouv.tac.analytics.server.config.security.oauth2tokenvalidator;

import java.time.Instant;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;


@ExtendWith(SpringExtension.class)
public class ExpirationTokenPresenceOAuth2TokenValidatorTest {

    @Mock
    private Jwt token;

    private final ExpirationTokenPresenceOAuth2TokenValidator expirationTokenPresenceOAuth2TokenValidator = new ExpirationTokenPresenceOAuth2TokenValidator();

    @Test
    public void shouldFailIfNoJtiInTheToken() {

        Mockito.when(token.getExpiresAt()).thenReturn(null);

        final OAuth2TokenValidatorResult result = expirationTokenPresenceOAuth2TokenValidator.validate(token);

        Assertions.assertThat(result.hasErrors()).isTrue();
        Assertions.assertThat(result.getErrors()).hasSize(1);
        final OAuth2Error oAuth2ErrorResult = result.getErrors().iterator().next();
        Assertions.assertThat(oAuth2ErrorResult.getErrorCode()).isEqualTo(ExpirationTokenPresenceOAuth2TokenValidator.EXPIRATION_NOT_FOUND_OAUTH2ERROR.getErrorCode());
        Assertions.assertThat(oAuth2ErrorResult.getDescription()).isEqualTo(ExpirationTokenPresenceOAuth2TokenValidator.EXPIRATION_NOT_FOUND_OAUTH2ERROR.getDescription());
        Assertions.assertThat(oAuth2ErrorResult.getUri()).isNull();
    }

    @Test
    public void shouldNotFailIfExpirationTokenIsPresent() {

        Mockito.when(token.getExpiresAt()).thenReturn(Instant.now());

        final OAuth2TokenValidatorResult result = expirationTokenPresenceOAuth2TokenValidator.validate(token);

        Assertions.assertThat(result.hasErrors()).isFalse();
    }

}