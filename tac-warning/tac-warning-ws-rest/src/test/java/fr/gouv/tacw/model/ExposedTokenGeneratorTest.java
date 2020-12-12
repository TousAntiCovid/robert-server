package fr.gouv.tacw.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import fr.gouv.tacw.database.model.ExposedStaticVisitEntity;
import fr.gouv.tacw.ws.configuration.TacWarningWsRestConfiguration;
import fr.gouv.tacw.ws.vo.QRCodeVo;
import fr.gouv.tacw.ws.vo.TokenTypeVo;
import fr.gouv.tacw.ws.vo.VenueCategoryVo;
import fr.gouv.tacw.ws.vo.VenueTypeVo;
import fr.gouv.tacw.ws.vo.VisitVo;

/*
* For privacy purposes, tokens are anonymized with token = hash(salt|uuid).
* When a user reports itself as infected, we compute all the possible token combinations
* and they are seen as exposed.
*/
@ExtendWith(SpringExtension.class)
@EnableConfigurationProperties(value = TacWarningWsRestConfiguration.class)
@TestPropertySource("classpath:application.properties")
public class ExposedTokenGeneratorTest {
	@Autowired
	private TacWarningWsRestConfiguration configuration;

	@Test
	public void testNumberOfGeneratedTokenIsOk() {
		VisitVo visit = visitVoExample();

		ExposedTokenGenerator tokenGenerator = new ExposedTokenGenerator(visit, this.configuration);
		List<ExposedStaticVisitEntity> tokens = tokenGenerator.generateAllExposedTokens().collect(Collectors.toList());

		assertThat(tokens).isNotNull();
		assertThat(tokens).isNotEmpty();
		assertThat(tokens.size()).isEqualTo(tokenGenerator.numberOfGeneratedTokens());
	}

	// TODO: more tests with invalid data, limit values

	@Test
	public void testGeneratedTokensIncludesDerivedToken() {
		VisitVo visit = visitVoExample();

		Stream<ExposedStaticVisitEntity> exposedVisits = new ExposedTokenGenerator(visit, this.configuration).generateAllExposedTokens();

		assertThat(exposedVisits.map(token -> token.getToken()))
				.contains("f9738b0006199fcf7d8a1f7b3523cd225b6620ced2588af7a410eeab16380c87");
	}

	protected VisitVo visitVoExample() {
		VisitVo visit = new VisitVo("24356657", 
				new QRCodeVo(TokenTypeVo.STATIC, VenueTypeVo.N, VenueCategoryVo.CAT1, 60,
						"0YWN3LXR5cGUiOiJTVEFUSUMiLCJ0YWN3LXZlcnNpb24iOjEsImVyc"));
		return visit;
	}
}
