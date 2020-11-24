package fr.gouv.tacw.ws.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.when;

import java.security.KeyPair;
import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import fr.gouv.tacw.ws.exception.TacWarningUnauthorizedException;
import fr.gouv.tacw.ws.service.impl.AuthorizationServiceImpl;
import fr.gouv.tacw.ws.utils.PropertyLoader;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Encoders;
import io.jsonwebtoken.security.Keys;

@ExtendWith(MockitoExtension.class)
public class AuthorizationServiceTests {
	@Mock
	private PropertyLoader propertyLoader;
	private AuthorizationService authorizationService;

	private KeyPair keyPair;

	@BeforeEach
	public void setUp() {
		keyPair = Keys.keyPairFor(SignatureAlgorithm.RS256);
		String jwtPublicKey = Encoders.BASE64.encode(keyPair.getPublic().getEncoded());

		when(propertyLoader.getJwtReportAuthorizationDisabled()).thenReturn(false);
		when(propertyLoader.getJwtPublicKey()).thenReturn(jwtPublicKey);
		authorizationService = new AuthorizationServiceImpl(propertyLoader);
	}
	
	@Test
	public void testGivenAnInvalidJwtBearerWhenRequestingAuthorizationThenAuthorizationFails() {
		assertThatExceptionOfType(TacWarningUnauthorizedException.class)
			.isThrownBy(() -> authorizationService.checkAuthorization("unauthorized"));
	}
	
	@Test
	public void testGivenAValidJwtBearerWhenRequestingAuthorizationThenAuthorizationSucceeds() {
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
