package fr.gouv.tacw.database;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import fr.gouv.stopc.robert.server.common.utils.TimeUtils;
import fr.gouv.tacw.database.service.TokenService;

@SpringBootTest
@Transactional
class TokenServiceTests {
	@Autowired
	TokenService tokenService;
	
	@Value("${tacw.database.visit_token_retention_period_days}")
	private long retentionDays;

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
	
	@Test
	void testDeleteExpiredTokens() {
		final String[] expired_tokens = {
				"ac831a7dd6cbe40751ac8d434bfa65cf8ae804133691283bdf2b3b113aea0001",
				"ac831a7dd6cbe40751ac8d434bfa65cf8ae804133691283bdf2b3b113aea0002",
				"ac831a7dd6cbe40751ac8d434bfa65cf8ae804133691283bdf2b3b113aea0003",
				"ac831a7dd6cbe40751ac8d434bfa65cf8ae804133691283bdf2b3b113aea0004",
				"ac831a7dd6cbe40751ac8d434bfa65cf8ae804133691283bdf2b3b113aea0005",
		};
		
		final String[] valid_tokens = {
				"ac831a7dd6cbe40751ac8d434bfa65cf8ae804133691283bdf2b3b113aea1001",
				"ac831a7dd6cbe40751ac8d434bfa65cf8ae804133691283bdf2b3b113aea1002",
				"ac831a7dd6cbe40751ac8d434bfa65cf8ae804133691283bdf2b3b113aea1003",
		};
		
		final long currentNtpTime = TimeUtils.convertUnixMillistoNtpSeconds(System.currentTimeMillis());
		final long windowStart = currentNtpTime - (retentionDays*86400);
		
		for(String token: expired_tokens) {
			tokenService.registerExposedStaticToken(windowStart - 150, token);
		}
		
		for(String token: valid_tokens) {
			tokenService.registerExposedStaticToken(windowStart + 500, token);
		}
		
		tokenService.deleteExpiredTokens();
		
		for(String token: expired_tokens) {
			assertThat(tokenService.exposedStaticTokensIncludes(token)).isEqualTo(false);
		}
		
		for(String token: valid_tokens) {
	        assertThat(tokenService.exposedStaticTokensIncludes(token)).isEqualTo(true);
		}
	}
}
