package fr.gouv.tac.analytics.server.config.security.oauth2tokenvalidator;


import fr.gouv.tac.analytics.server.service.TokenIdentifierService;
import fr.gouv.tac.analytics.server.model.mongo.TokenIdentifier;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.ZonedDateTime;

@ExtendWith(SpringExtension.class)
public class JtiCanOnlyBeUsedOnceOAuth2TokenValidatorTest {

    @Mock
    private Jwt token;

    @Mock
    private TokenIdentifierService tokenIdentifierService;

    @InjectMocks
    private JtiCanOnlyBeUsedOnceOAuth2TokenValidator jtiCanOnlyBeUsedOnceOAuth2TokenValidator;


    @Test
    public void shouldFailIfTokenIdentifierAlreadyExist() {

        final String jti = "someId";
        final ZonedDateTime expirationDate = ZonedDateTime.now();

        Mockito.when(token.getId()).thenReturn(jti);
        Mockito.when(tokenIdentifierService.tokenIdentifierExist(jti)).thenReturn(true);

        final OAuth2TokenValidatorResult result = jtiCanOnlyBeUsedOnceOAuth2TokenValidator.validate(token);

        Assertions.assertThat(result.hasErrors()).isTrue();
        Assertions.assertThat(result.getErrors()).hasSize(1);
        final OAuth2Error oAuth2ErrorResult = result.getErrors().iterator().next();
        Assertions.assertThat(oAuth2ErrorResult.getErrorCode()).isEqualTo(JtiCanOnlyBeUsedOnceOAuth2TokenValidator.JTI_USED_MORE_THAN_ONCE_OAUTH2ERROR.getErrorCode());
        Assertions.assertThat(oAuth2ErrorResult.getDescription()).isEqualTo(JtiCanOnlyBeUsedOnceOAuth2TokenValidator.JTI_USED_MORE_THAN_ONCE_OAUTH2ERROR.getDescription());
        Assertions.assertThat(oAuth2ErrorResult.getUri()).isNull();

        Mockito.verify(tokenIdentifierService, Mockito.never()).save(jti, expirationDate);
    }

    @Test
    public void shouldSuccessWhenItsTheFirstTokenIdentifierUsage() {

        final String jti = "someId";
        final ZonedDateTime expirationDate = ZonedDateTime.now();

        Mockito.when(token.getId()).thenReturn(jti);
        Mockito.when(token.getExpiresAt()).thenReturn(expirationDate.toInstant());
        Mockito.when(tokenIdentifierService.tokenIdentifierExist(jti)).thenReturn(false);
        Mockito.when(tokenIdentifierService.save(jti, expirationDate)).thenReturn(new TokenIdentifier());

        final OAuth2TokenValidatorResult result = jtiCanOnlyBeUsedOnceOAuth2TokenValidator.validate(token);

        Assertions.assertThat(result.hasErrors()).isFalse();
    }
}