package fr.gouv.tacw.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import fr.gouv.tacw.ws.vo.QRCodeVo;
import fr.gouv.tacw.ws.vo.VisitTokenVo;
import fr.gouv.tacw.ws.vo.VisitVo;
import fr.gouv.tacw.ws.vo.TokenTypeVo;
import fr.gouv.tacw.ws.vo.VenueCategoryVo;
import fr.gouv.tacw.ws.vo.VenueTypeVo;

/*
* For privacy purposes, tokens area anonymized with token = hash(salt|uuid|time).
* When a user reports itself as infected, we compute all the possible token combinations
* and they are seen as exposed.
*/
public class ExposedTokenGeneratorTest {
	@Test
	public void testNumberOfGeneratedTokenIsOk() {
		VisitVo visit = visitVoExample();

		List<VisitTokenVo> tokens = new ExposedTokenGenerator(visit).generateAllExposedTokens();

		assertThat(tokens).isNotNull();
		assertThat(tokens).isNotEmpty();
		assertThat(tokens.size()).isEqualTo(ExposedTokenGenerator.numberOfGeneratedTokens());
	}

	// TODO: more tests with invalid data (e.g. wrong timestamp string), limit
	// values

	@Test
	public void testGeneratedTokensIncludesDerivedToken() {
		VisitVo visit = visitVoExample();

		List<VisitTokenVo> tokens = new ExposedTokenGenerator(visit).generateAllExposedTokens();

		assertThat(tokens.stream().map(token -> token.getPayload()))
				.contains("f4870249da823379ba646f4ead4fcf703416e3ef45e22a7c6fe8890665ccd733");
	}

	protected VisitVo visitVoExample() {
		VisitVo visit = new VisitVo("24356657", 
				new QRCodeVo(TokenTypeVo.STATIC, VenueTypeVo.N, VenueCategoryVo.CAT1, 60,
						"0YWN3LXR5cGUiOiJTVEFUSUMiLCJ0YWN3LXZlcnNpb24iOjEsImVyc"));
		return visit;
	}
}
