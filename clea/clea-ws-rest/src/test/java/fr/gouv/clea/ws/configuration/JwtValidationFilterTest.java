package fr.gouv.clea.ws.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.gouv.clea.ws.dto.ApiError;
import fr.gouv.clea.ws.utils.UriConstants;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Encoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.testcontainers.shaded.org.apache.commons.lang.RandomStringUtils;

import java.io.IOException;
import java.security.KeyPair;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class JwtValidationFilterTest {

    private final MockHttpServletRequest request = new MockHttpServletRequest("POST", UriConstants.API_V1 + UriConstants.REPORT);
    private final MockHttpServletResponse response = new MockHttpServletResponse();
    private final MockFilterChain chain = new MockFilterChain();
    private String jwtPublicKey;
    private KeyPair keyPair;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private HandlerExceptionResolver handlerExceptionResolver;

    @BeforeEach
    void init() {
        keyPair = Keys.keyPairFor(SignatureAlgorithm.RS256);
        jwtPublicKey = Encoders.BASE64.encode(keyPair.getPublic().getEncoded());
    }

    @Test
    @DisplayName("if authorization check is active, a valid token should not cause filter to reject request")
    void testEnabledAuthWithValidToken() {
        long jwtLifeTime = 5;
        Instant now = Instant.now();
        Instant expiration = now.plus(jwtLifeTime, ChronoUnit.MINUTES);
        JwtValidationFilter jwtValidationFilter = new JwtValidationFilter(true, jwtPublicKey, handlerExceptionResolver);
        request.addHeader(HttpHeaders.AUTHORIZATION, this.newJwtToken(now, expiration));
        jwtValidationFilter.doFilter(request, response, chain);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("if authorization check is active, a token with an expired date should cause filter to return 403")
    void testEnabledAuthWithExpiredToken() {
        Instant now = Instant.now();
        JwtValidationFilter jwtValidationFilter = new JwtValidationFilter(true, jwtPublicKey, handlerExceptionResolver);
        request.addHeader(HttpHeaders.AUTHORIZATION, this.newJwtToken(now, now));
        jwtValidationFilter.doFilter(request, response, chain);
        assertThat(response.getStatus()).isEqualTo(403);
    }

    @Test
    @DisplayName("if authorization check is active, a null token should cause filter to return 401")
    void testEnabledAuthWithNullToken() throws IOException {
        JwtValidationFilter jwtValidationFilter = new JwtValidationFilter(true, jwtPublicKey, handlerExceptionResolver);
        jwtValidationFilter.doFilter(request, response, chain);
        ApiError apiError = objectMapper.readValue(response.getContentAsString(), ApiError.class);
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(apiError.getMessage()).isEqualTo("Could not be authorized (Missing authorisation header/token)");
    }

    @Test
    @DisplayName("if authorization check is inactive, a null token should have no impact")
    void testDisabledAuthWithNullToken() {
        JwtValidationFilter jwtValidationFilter = new JwtValidationFilter(false, jwtPublicKey, handlerExceptionResolver);
        jwtValidationFilter.doFilter(request, response, chain);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("if authorization check is active, an invalid token should cause filter to return 403")
    void testEnabledAuthWithInvalidToken() throws IOException {
        JwtValidationFilter jwtValidationFilter = new JwtValidationFilter(true, jwtPublicKey, handlerExceptionResolver);
        request.addHeader(HttpHeaders.AUTHORIZATION, RandomStringUtils.randomAlphanumeric(9));
        jwtValidationFilter.doFilter(request, response, chain);
        ApiError apiError = objectMapper.readValue(response.getContentAsString(), ApiError.class);
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(apiError.getMessage()).isEqualTo("Could not be authenticated (Authorisation header/token invalid)");
    }

    @Test
    @DisplayName("if authorization check is inactive, an invalid token should have no impact")
    void testDisabledAuthWithInvalidToken() {
        JwtValidationFilter jwtValidationFilter = new JwtValidationFilter(false, jwtPublicKey, handlerExceptionResolver);
        request.addHeader(HttpHeaders.AUTHORIZATION, RandomStringUtils.randomAlphanumeric(9));
        jwtValidationFilter.doFilter(request, response, chain);
        assertThat(response.getStatus()).isEqualTo(200);
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