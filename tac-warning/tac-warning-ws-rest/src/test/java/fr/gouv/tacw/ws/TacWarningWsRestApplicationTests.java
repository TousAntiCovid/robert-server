package fr.gouv.tacw.ws;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import fr.gouv.tacw.ws.dto.ExposureStatusResponseDto;
import fr.gouv.tacw.ws.service.WarningService;
import fr.gouv.tacw.ws.utils.UriConstants;
import fr.gouv.tacw.ws.vo.ExposureStatusRequestVo;
import fr.gouv.tacw.ws.vo.QRCodeVo;
import fr.gouv.tacw.ws.vo.ReportRequestVo;
import fr.gouv.tacw.ws.vo.TokenTypeVo;
import fr.gouv.tacw.ws.vo.VisitTokenVo;
import fr.gouv.tacw.ws.vo.VisitVo;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@MockBean(WarningService.class)
class TacWarningWsRestApplicationTests {
	@Autowired
	private TestRestTemplate restTemplate;

	@Autowired
	private WarningService warningService;

	@Value("${controller.path.prefix}" + UriConstants.API_V1)
	private String pathPrefixV1;

	@Test
	void testInfectedUserCanReportItselfAsInfected() {
		ReportRequestVo request = new ReportRequestVo(new ArrayList<VisitVo>());
		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth("foo");
		HttpEntity<ReportRequestVo> entity = new HttpEntity<>(request, headers);
		ResponseEntity<String> response = restTemplate.postForEntity(pathPrefixV1 + UriConstants.REPORT, entity,
				String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
	}

	@Test
	void testCanGetStatus() {
		ExposureStatusRequestVo request = new ExposureStatusRequestVo(new ArrayList<VisitTokenVo>());
		when(warningService.getStatus(any(ExposureStatusRequestVo.class))).thenReturn(true);

		ResponseEntity<ExposureStatusResponseDto> response = restTemplate
				.postForEntity(pathPrefixV1 + UriConstants.STATUS, request, ExposureStatusResponseDto.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
	}

	@Test
	void testWhenRequestExposureStatusWithInvalidMediaTypeThenGetUnsupportedMediaType() {
		ResponseEntity<ExposureStatusResponseDto> response = restTemplate.postForEntity(
				pathPrefixV1 + UriConstants.STATUS, 
				new HttpEntity<String>("foo"),
				ExposureStatusResponseDto.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
		verifyNoMoreInteractions(warningService);
	}

	@Test
	void testWhenRequestExposureStatusWithInvalidJsonDataThenGetBadRequest() throws JSONException {
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("id", 1);
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		ResponseEntity<ExposureStatusResponseDto> response = restTemplate.postForEntity(
				pathPrefixV1 + UriConstants.STATUS, 
				new HttpEntity<String>(jsonObject.toString(), headers),
				ExposureStatusResponseDto.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		verifyNoMoreInteractions(warningService);
	}

	@Test
	void testWhenExposureStatusRequestWithNullVisitTokensThenGetBadRequest() {
		ResponseEntity<ExposureStatusResponseDto> response = restTemplate.postForEntity(
				pathPrefixV1 + UriConstants.STATUS, 
				new ExposureStatusRequestVo(null), 
				ExposureStatusResponseDto.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		verifyNoMoreInteractions(warningService);
	}

	@Test
	void testWhenExposureStatusRequestWithNullTokenTypeThenGetBadRequest() {
		ArrayList<VisitTokenVo> visitTokens = new ArrayList<VisitTokenVo>();
		visitTokens.add(new VisitTokenVo(null, "payload"));
		ExposureStatusRequestVo entity = new ExposureStatusRequestVo(visitTokens);
		ResponseEntity<ExposureStatusResponseDto> response = restTemplate.postForEntity(
				pathPrefixV1 + UriConstants.STATUS, 
				entity, 
				ExposureStatusResponseDto.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		verifyNoMoreInteractions(warningService);
	}

	@Test
	void testWhenExposureStatusRequestWithInvalidTokenTypeThenGetBadRequest() throws JSONException {
		String json = "{\n"
				+ "  \"visitTokens\" : [ {\n"
				+ "    \"type\" : \"UNKNOWN\",\n"
				+ "    \"payload\" : \"0YWN3LXR5cGUiOiJTVEFUSUMiLCJ0YWN3LXZlcnNpb24iOjEsImVyc\"\n"
				+ "  } ]\n"
				+ "}";
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		ResponseEntity<ExposureStatusResponseDto> response = restTemplate.postForEntity(
				pathPrefixV1 + UriConstants.STATUS, 
				new HttpEntity<String>(json, headers),
				ExposureStatusResponseDto.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		verifyNoMoreInteractions(warningService);
	}

	@Test
	void testWhenExposureStatusRequestWithNullPayloadThenGetBadRequest() {
		ArrayList<VisitTokenVo> visitTokens = new ArrayList<VisitTokenVo>();
		visitTokens.add(new VisitTokenVo(TokenTypeVo.STATIC, null));
		ExposureStatusRequestVo entity = new ExposureStatusRequestVo(visitTokens);
		ResponseEntity<ExposureStatusResponseDto> response = restTemplate.postForEntity(
				pathPrefixV1 + UriConstants.STATUS, 
				entity, 
				ExposureStatusResponseDto.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		verifyNoMoreInteractions(warningService);
	}

	@Test
	void testWhenReportRequestWithInvalidMediaTypeThenGetUnsupportedMediaType() {
		ResponseEntity<String> response = restTemplate.postForEntity(
				pathPrefixV1 + UriConstants.REPORT, 
				new HttpEntity<String>("foo"),
				String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
		verifyNoMoreInteractions(warningService);
	}

	@Test
	void testWhenReportRequestWithInvalidJsonDataThenGetBadRequest() throws JSONException {
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("id", 1);
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		ResponseEntity<String> response = restTemplate.postForEntity(
				pathPrefixV1 + UriConstants.REPORT, 
				new HttpEntity<String>(jsonObject.toString(), headers),
				String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		verifyNoMoreInteractions(warningService);
	}

	@Test
	void testWhenReportRequestWithNullVisitTokensThenGetBadRequest() {
		ResponseEntity<String> response = restTemplate.postForEntity(
				pathPrefixV1 + UriConstants.REPORT, 
				new ReportRequestVo(null), 
				String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		verifyNoMoreInteractions(warningService);
	}

	@Test
	void testWhenReportRequestWithNullTimestampTypeThenGetBadRequest() {
		ArrayList<VisitVo> visitQrCodes = new ArrayList<VisitVo>();
		visitQrCodes.add(new VisitVo(null, new QRCodeVo(TokenTypeVo.STATIC, "venueType", 60, "uuid")));
		ReportRequestVo entity = new ReportRequestVo(visitQrCodes);
		ResponseEntity<String> response = restTemplate.postForEntity(
				pathPrefixV1 + UriConstants.REPORT, 
				entity, 
				String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		verifyNoMoreInteractions(warningService);
	}

	@Test
	void testWhenReportRequestWithNullQRCodeTypeThenGetBadRequest() {
		ArrayList<VisitVo> visitQrCodes = new ArrayList<VisitVo>();
		visitQrCodes.add(new VisitVo("12345", null));
		ReportRequestVo entity = new ReportRequestVo(visitQrCodes);
		ResponseEntity<String> response = restTemplate.postForEntity(
				pathPrefixV1 + UriConstants.REPORT, 
				entity, 
				String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		verifyNoMoreInteractions(warningService);
	}

	@Test
	void testWhenReportRequestWithInvalidQRCodeTypeThenGetBadRequest() throws JSONException {
		String json = "{\n"
				+ "   \"visits\": [\n"
				+ "     {\n"
				+ "       \"timestamp\": \"3814657232\",\n"
				+ "       \"qrCode\": {\n"
				+ "         \"type\": \"UNKNOWN\",\n"
				+ "         \"venueType\": \"N\",\n"
				+ "         \"venueCapacity\": 42,\n"
				+ "         \"uuid\": \"9fb80fea-b3ac-4603-b40a-0be8879dbe79\"\n"
				+ "       }\n"
				+ "     }\n"
				+ "   ]\n"
				+ "}\n"
				+ "";
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		ResponseEntity<String> response = restTemplate.postForEntity(
				pathPrefixV1 + UriConstants.REPORT, 
				new HttpEntity<String>(json, headers),
				String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		verifyNoMoreInteractions(warningService);
	}
	
	@Test
	void testWhenReportRequestWithNullUuidThenGetBadRequest() {
		ArrayList<VisitVo> visitQrCodes = new ArrayList<VisitVo>();
		visitQrCodes.add(new VisitVo("12345", new QRCodeVo(TokenTypeVo.STATIC, "venueType", 60, null)));
		ReportRequestVo entity = new ReportRequestVo(visitQrCodes);
		ResponseEntity<String> response = restTemplate.postForEntity(
				pathPrefixV1 + UriConstants.REPORT, 
				entity, 
				String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		verifyNoMoreInteractions(warningService);
	}
	
	@Test
	void testWhenReportRequestWithNullVenueCategoryOrVenueTypeOrVenueCapacityThenRequestSucceeds() {
		String json = "{\n"
				+ "   \"visits\": [\n"
				+ "     {\n"
				+ "       \"timestamp\": \"3814657232\",\n"
				+ "       \"qrCode\": {\n"
				+ "         \"type\": \"STATIC\",\n"
				+ "         \"uuid\": \"9fb80fea-b3ac-4603-b40a-0be8879dbe79\"\n"
				+ "       }\n"
				+ "     }\n"
				+ "   ]\n"
				+ "}\n"
				+ "";
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);

		ResponseEntity<String> response = restTemplate.postForEntity(
				pathPrefixV1 + UriConstants.REPORT, 
				new HttpEntity<String>(json, headers), 
				String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
	}		
	
	@Test
	void testWhenReportRequestWithValidVisitThenRequestSucceed() throws JSONException {
		String json = "{\n"
				+ "   \"visits\": [\n"
				+ "     {\n"
				+ "       \"timestamp\": \"3814657232\",\n"
				+ "       \"qrCode\": {\n"
				+ "         \"type\": \"STATIC\",\n"
				+ "         \"venueType\": \"N\",\n"
				+ "         \"venueCapacity\": 42,\n"
				+ "         \"uuid\": \"9fb80fea-b3ac-4603-b40a-0be8879dbe79\"\n"
				+ "       }\n"
				+ "     }\n"
				+ "   ]\n"
				+ "}\n"
				+ "";
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);

		ResponseEntity<String> response = restTemplate.postForEntity(
				pathPrefixV1 + UriConstants.REPORT, 
				new HttpEntity<String>(json, headers),
				String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
	}
}