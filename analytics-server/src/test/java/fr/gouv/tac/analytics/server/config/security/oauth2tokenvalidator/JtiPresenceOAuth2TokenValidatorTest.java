package fr.gouv.tac.analytics.server.config.security.oauth2tokenvalidator;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
public class JtiPresenceOAuth2TokenValidatorTest {

    @Mock
    private Jwt token;

    private final JtiPresenceOAuth2TokenValidator jtiPresenceOAuth2TokenValidator = new JtiPresenceOAuth2TokenValidator();

    @Test
    public void shouldFailIfNoJtiInTheToken() {

        Mockito.when(token.getId()).thenReturn(null);

        final OAuth2TokenValidatorResult result = jtiPresenceOAuth2TokenValidator.validate(token);

        Assertions.assertThat(result.hasErrors()).isTrue();
        Assertions.assertThat(result.getErrors()).hasSize(1);
        final OAuth2Error oAuth2ErrorResult = result.getErrors().iterator().next();
        Assertions.assertThat(oAuth2ErrorResult.getErrorCode()).isEqualTo(JtiPresenceOAuth2TokenValidator.JTI_NOT_FOUND_OAUTH2ERROR.getErrorCode());
        Assertions.assertThat(oAuth2ErrorResult.getDescription()).isEqualTo(JtiPresenceOAuth2TokenValidator.JTI_NOT_FOUND_OAUTH2ERROR.getDescription());
        Assertions.assertThat(oAuth2ErrorResult.getUri()).isNull();
    }

    @Test
    public void shouldNotFailIfJtiIsPresent() {

        final String jti = "someId";
        Mockito.when(token.getId()).thenReturn(jti);

        final OAuth2TokenValidatorResult result = jtiPresenceOAuth2TokenValidator.validate(token);

        Assertions.assertThat(result.hasErrors()).isFalse();
    }

}