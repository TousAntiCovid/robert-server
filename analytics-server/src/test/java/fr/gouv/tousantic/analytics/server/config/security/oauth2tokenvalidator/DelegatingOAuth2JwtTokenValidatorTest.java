package fr.gouv.tousantic.analytics.server.config.security.oauth2tokenvalidator;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
public class DelegatingOAuth2JwtTokenValidatorTest {

    @Mock
    private OAuth2TokenValidator<Jwt> validator1;

    @Mock
    private OAuth2TokenValidator<Jwt> validator2;

    @Mock
    private Jwt token;

    private DelegatingOAuth2JwtTokenValidator delegatingOAuth2JwtTokenValidator;

    @BeforeEach
    public void setUp() {
        delegatingOAuth2JwtTokenValidator = new DelegatingOAuth2JwtTokenValidator(validator1, validator2);
    }

    @Test
    public void shouldCallEachValidatorIfNoError() {

        Mockito.when(validator1.validate(token)).thenReturn(OAuth2TokenValidatorResult.success());
        Mockito.when(validator2.validate(token)).thenReturn(OAuth2TokenValidatorResult.success());

        final OAuth2TokenValidatorResult result = delegatingOAuth2JwtTokenValidator.validate(token);

        Assertions.assertThat(result.hasErrors()).isFalse();

        final InOrder inOrder = Mockito.inOrder(validator1, validator2);

        inOrder.verify(validator1, Mockito.times(1)).validate(token);
        inOrder.verify(validator2, Mockito.times(1)).validate(token);
    }

    @Test
    public void shouldStopValidationWhenAnErrorIsEncountered() {

        final OAuth2TokenValidatorResult failure = OAuth2TokenValidatorResult.failure(new OAuth2Error("errorCode", "error message", "some uri"));

        Mockito.when(validator1.validate(token)).thenReturn(failure);

        final OAuth2TokenValidatorResult result = delegatingOAuth2JwtTokenValidator.validate(token);

        Assertions.assertThat(result.hasErrors()).isTrue();
        Assertions.assertThat(result.getErrors()).containsExactlyInAnyOrderElementsOf(failure.getErrors());

        Mockito.verify(validator2, Mockito.never()).validate(token);
    }

}