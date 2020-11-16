package fr.gouv.tacw.ws;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import fr.gouv.tacw.ws.dto.ExposureStatusResponseDto;
import fr.gouv.tacw.ws.dto.ReportResponseDto;
import fr.gouv.tacw.ws.utils.UriConstants;
import fr.gouv.tacw.ws.vo.ExposureStatusRequestVo;
import fr.gouv.tacw.ws.vo.QRCodeVo;
import fr.gouv.tacw.ws.vo.ReportRequestVo;
import fr.gouv.tacw.ws.vo.VisitTokenVo;
import fr.gouv.tacw.ws.vo.VisitVo;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class TacWarningIntegrationTests {
	@Autowired
	private TestRestTemplate restTemplate;

	@Value("${controller.path.prefix}" + UriConstants.API_V1)
	private String pathPrefixV1;

	@Test
	void testStatusOfVisitTokenNotInfectedIsNotAtRisk() {
		List<String> tokens = Stream.of("0YWN3LXR5cGUiOiJTVEFUSUMiLCJ0YWN3LXZlcnNpb24iOjEsImVyc")
				.collect(Collectors.toList());
		ExposureStatusRequestVo statusRequestVo = new ExposureStatusRequestVo(this.staticVisitTokenVoFrom(tokens));

		ResponseEntity<ExposureStatusResponseDto> response = restTemplate
				.postForEntity(pathPrefixV1 + UriConstants.STATUS, statusRequestVo, ExposureStatusResponseDto.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody().isAtRisk()).isFalse();
	}

	@Test
	public void testStatusOfVisitTokenInfectedIsAtRisk() {
		List<VisitVo> visits = new ArrayList<VisitVo>();
		visits.add( new VisitVo("timestamp", new QRCodeVo(
				VisitTokenVo.tokenType.STATIC, 
				"restaurant", 
				60,
				"0YWN3LXR5cGUiOiJTVEFUSUMiLCJ0YWN3LXZlcnNpb24iOjEsImVyc")));
		List<VisitTokenVo> tokensVo = this.staticVisitTokenVoFrom(
				visits.stream().map(visit -> visit.getQrCode().getUuid()).collect(Collectors.toList()) );
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
		visits.add(new VisitVo("timestamp", new QRCodeVo(VisitTokenVo.tokenType.STATIC, "restaurant", 60, "UUID")));

		this.report(visits);
	}

	private void report(List<VisitVo> visits) {
		ResponseEntity<ReportResponseDto> response = restTemplate.postForEntity(pathPrefixV1 + UriConstants.REPORT,
				new ReportRequestVo(visits), ReportResponseDto.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody().isSuccess()).isEqualTo(true);
	}

	protected List<VisitTokenVo> staticVisitTokenVoFrom(List<String> tokens) {
		return tokens.stream().map(token -> new VisitTokenVo(VisitTokenVo.tokenType.STATIC, token))
				.collect(Collectors.toList());
	}

}