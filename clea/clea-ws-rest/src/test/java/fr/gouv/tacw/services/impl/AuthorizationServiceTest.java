package fr.gouv.tacw.services.impl;

import fr.gouv.tacw.exceptions.TacWarningUnauthorizedException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Encoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class AuthorizationServiceTest {

    private AuthorizationService authorizationService;

    private KeyPair keyPair;

    @BeforeEach
    void init() {
        keyPair = Keys.keyPairFor(SignatureAlgorithm.RS256);
        String jwtPublicKey = Encoders.BASE64.encode(keyPair.getPublic().getEncoded());
        authorizationService = new AuthorizationService(true, jwtPublicKey);
    }

    @Test
    void testGivenAnInvalidJwtBearerWhenRequestingAuthorizationThenAuthorizationFails() {
        assertThatExceptionOfType(TacWarningUnauthorizedException.class)
                .isThrownBy(() -> authorizationService.checkAuthorization("unauthorized"));
    }

    @Test
    void testGivenAValidJwtBearerWhenRequestingAuthorizationThenAuthorizationSucceeds() {
        long jwtLifeTime = 5 * 60000;
        Date now = new Date();
        Date expiration = new Date(now.getTime() + jwtLifeTime * 60000);
        boolean isAuthorized = authorizationService.checkAuthorization(this.newJwtToken(now, expiration));
        assertThat(isAuthorized).isTrue();
    }

    @Test
    public void testGivenAValidJwtBearerAlreadyExpiredWhenRequestingAuthorizationThenAuthorizationFails() {
        Date now = new Date();
        assertThatExceptionOfType(TacWarningUnauthorizedException.class)
                .isThrownBy(() -> authorizationService.checkAuthorization(this.newJwtToken(now, now)));
    }

    protected String newJwtToken(Date now, Date expiration) {
        return Jwts.builder()
                .setHeaderParam("type", "JWT")
                .setIssuedAt(now)
                .setExpiration(expiration)
                .signWith(keyPair.getPrivate(), SignatureAlgorithm.RS256)
                .compact();
    }
}
