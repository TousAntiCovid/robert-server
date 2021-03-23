package fr.gouv.clea.ws.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.security.KeyPair;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import fr.gouv.clea.ws.exception.CleaUnauthorizedException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Encoders;
import io.jsonwebtoken.security.Keys;

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
        assertThatExceptionOfType(CleaUnauthorizedException.class)
                .isThrownBy(() -> authorizationService.checkAuthorization("unauthorized"));
    }

    @Test
    void testGivenAValidJwtBearerWhenRequestingAuthorizationThenAuthorizationSucceeds() {
        long jwtLifeTime = 5;
        Instant now = Instant.now();
        Instant expiration = now.plus(jwtLifeTime, ChronoUnit.MINUTES);
        boolean isAuthorized = authorizationService.checkAuthorization(this.newJwtToken(now, expiration));
        assertThat(isAuthorized).isTrue();
    }

    @Test
    public void testGivenAValidJwtBearerAlreadyExpiredWhenRequestingAuthorizationThenAuthorizationFails() {
        Instant now = Instant.now();
        assertThatExceptionOfType(CleaUnauthorizedException.class)
                .isThrownBy(() -> authorizationService.checkAuthorization(this.newJwtToken(now, now)));
    }

    protected String newJwtToken(Instant now, Instant expiration) {
        return Jwts.builder()
                .setHeaderParam("type", "JWT")
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiration))
                .signWith(keyPair.getPrivate(), SignatureAlgorithm.RS256)
                .compact();
    }
}
