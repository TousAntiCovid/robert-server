package fr.gouv.tacw.ws;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.security.KeyPair;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import fr.gouv.tacw.model.ExposedTokenGenerator;
import fr.gouv.tacw.ws.dto.ExposureStatusResponseDto;
import fr.gouv.tacw.ws.dto.ReportResponseDto;
import fr.gouv.tacw.ws.service.AuthorizationService;
import fr.gouv.tacw.ws.utils.PropertyLoader;
import fr.gouv.tacw.ws.utils.UriConstants;
import fr.gouv.tacw.ws.vo.ExposureStatusRequestVo;
import fr.gouv.tacw.ws.vo.QRCodeVo;
import fr.gouv.tacw.ws.vo.ReportRequestVo;
import fr.gouv.tacw.ws.vo.TokenTypeVo;
import fr.gouv.tacw.ws.vo.VenueCategoryVo;
import fr.gouv.tacw.ws.vo.VenueTypeVo;
import fr.gouv.tacw.ws.vo.VisitTokenVo;
import fr.gouv.tacw.ws.vo.VisitVo;
import io.jsonwebtoken.io.Encoders;
import io.jsonwebtoken.security.Keys;


@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@MockBean(PropertyLoader.class)
class TacWarningIntegrationTests {
	@Autowired
	private TestRestTemplate restTemplate;

	@Value("${controller.path.prefix}" + UriConstants.API_V1)
	private String pathPrefixV1;

	@Autowired
	private PropertyLoader propertyLoader;
	
	private KeyPair keyPair;

	private long referenceTime;
	
	@BeforeEach
	public void setUp() {
		keyPair = Keys.keyPairFor(AuthorizationService.algo);
		referenceTime = System.currentTimeMillis();

		when(propertyLoader.getJwtReportAuthorizationDisabled()).thenReturn(false);
		when(propertyLoader.getJwtPublicKey()).thenReturn(Encoders.BASE64.encode(keyPair.getPublic().getEncoded()));
	}
	
	@Test
	void testStatusOfVisitTokenNotInfectedIsNotAtRisk() {
		List<VisitVo> tokens = Stream
				.of(new VisitVo("12345", 
						new QRCodeVo(TokenTypeVo.STATIC, VenueTypeVo.N, VenueCategoryVo.CAT1, 60, "TokenNotInExposedList")))
				.collect(Collectors.toList());
		ExposureStatusRequestVo statusRequestVo = new ExposureStatusRequestVo(this.staticVisitTokenVoFrom(tokens));

		ResponseEntity<ExposureStatusResponseDto> response = restTemplate
				.postForEntity(pathPrefixV1 + UriConstants.STATUS, statusRequestVo, ExposureStatusResponseDto.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody().isAtRisk()).isFalse();
	}

	@Test
	public void testStatusOfVisitTokenInfectedIsAtRisk() {
		List<VisitVo> visits = this.buildExposedVisits("4YWN3LXR5cGUiOiJTVEFUSUMiLCJ0YWN3LXZlcnNpb24iOjEsImVyd", 3);
		List<VisitTokenVo> tokensVo = this.staticVisitTokenVoFrom(visits);
		this.report(visits);

		ResponseEntity<ExposureStatusResponseDto> response = restTemplate.postForEntity(
				pathPrefixV1 + UriConstants.STATUS, new ExposureStatusRequestVo(tokensVo),
				ExposureStatusResponseDto.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody().isAtRisk()).isTrue();
	}
	
	@Test
	public void testCanReportVisitsWhenInfected() {
		List<VisitVo> visits = new ArrayList<VisitVo>();
		visits.add(new VisitVo("12345", 
				new QRCodeVo(TokenTypeVo.STATIC, VenueTypeVo.N, VenueCategoryVo.CAT1, 60, "UUID")));

		this.report(visits);
	}

	/**
	 * For privacy purposes, tokens area anonymized with token =
	 * hash(salt|uuid|time). When a user reports itself as infected, we compute all
	 * the possible token combinations and they are seen as exposed. This test
	 * checks that the generation of the tokens has been done!
	 */
	@Test
	public void testGivenAVisitTokenInfectedNotIdenticalToReportedTokenWhenCheckingStatusThenIsAtRisk() {
		List<VisitVo> visits = this.buildExposedVisits("0YWN3LXR5cGUiOiJTVEFUSUMiLCJ0YWN3LXZlcnNpb24iOjEsImVyc", 3);
		String exposedToken = "f9738b0006199fcf7d8a1f7b3523cd225b6620ced2588af7a410eeab16380c87";
		List<VisitTokenVo> visitTokens = new ArrayList<VisitTokenVo>();
		visitTokens.add(new VisitTokenVo(TokenTypeVo.STATIC, exposedToken, visits.get(1).getTimestamp()));
		this.report(visits);
		
		ResponseEntity<ExposureStatusResponseDto> response = restTemplate.postForEntity(
				pathPrefixV1 + UriConstants.STATUS, new ExposureStatusRequestVo(visitTokens),
				ExposureStatusResponseDto.class);
	
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody().isAtRisk()).isTrue();
	}

	private void report(List<VisitVo> visits) {
		HttpEntity<ReportRequestVo> entity;
		try {
			entity = new HttpJwtHeaderUtils(keyPair.getPrivate()).
					getReportEntityWithBearer(new ReportRequestVo(visits));
		} catch (Exception e) {
			throw new RuntimeException(e);
		} 

		ResponseEntity<ReportResponseDto> response = restTemplate.postForEntity(pathPrefixV1 + UriConstants.REPORT,
				entity, ReportResponseDto.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody().isSuccess()).isEqualTo(true);
	}

	protected List<VisitVo>	buildExposedVisits(String venueToken, int nbVisits) {
		return 
			IntStream.rangeClosed(1, nbVisits)
				.mapToObj( i -> this.buildExposedVisit(venueToken) )
				.collect(Collectors.toList());
	}

	protected VisitVo buildExposedVisit(String venueToken) {
		return new VisitVo(
			String.valueOf(referenceTime), 
			new QRCodeVo(TokenTypeVo.STATIC, VenueTypeVo.N, VenueCategoryVo.CAT1, 60,
					venueToken) );
	}

	protected List<VisitTokenVo> staticVisitTokenVoFrom(List<VisitVo> visits) {
		return visits.stream().map(visit -> this.staticVisitTokenVoFrom(visit)).collect(Collectors.toList());
	}

	protected VisitTokenVo staticVisitTokenVoFrom(VisitVo visit) {
		String exposedToken = new ExposedTokenGenerator(visit, null).hash(1);
		return new VisitTokenVo(TokenTypeVo.STATIC, exposedToken, visit.getTimestamp());
	}
}