package fr.gouv.tacw.database;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import fr.gouv.tacw.database.service.TokenService;

@SpringBootTest
@Transactional
class TokenServiceTests {
	@Autowired
	TokenService tokenService;

	@Test
	void testRegisteredStaticTokenIsIncludedInInfectedTokens() {
		String token = "0YWN3LXR5cGUiOiJTVEFUSUMiLCJ0YWN3LXZlcnNpb24iOjEsImVyc";
		tokenService.registerExposedStaticToken(12345L, token);

		assertThat(tokenService.exposedStaticTokensIncludes(token)).isEqualTo(true);
	}

	@Test
	void testStaticTokenNotRegisteredIsNotIncludedInInfectedTokens() {
		String token = "0YWN3LXR5cGUiOiJTVEFUSUMiLCJ0YWN3LXZlcnNpb24iOjEsImVyc";

		assertThat(tokenService.exposedStaticTokensIncludes(token)).isEqualTo(false);
	}
}
