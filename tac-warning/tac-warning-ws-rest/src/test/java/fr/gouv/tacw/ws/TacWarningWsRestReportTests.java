package fr.gouv.tacw.ws;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.util.ArrayList;

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

import fr.gouv.tacw.ws.service.WarningService;
import fr.gouv.tacw.ws.utils.UriConstants;
import fr.gouv.tacw.ws.vo.QRCodeVo;
import fr.gouv.tacw.ws.vo.ReportRequestVo;
import fr.gouv.tacw.ws.vo.TokenTypeVo;
import fr.gouv.tacw.ws.vo.VenueCategoryVo;
import fr.gouv.tacw.ws.vo.VisitVo;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@MockBean(WarningService.class)
class TacWarningWsRestReportTests {
	@Autowired
	private TestRestTemplate restTemplate;

	@Autowired
	private WarningService warningService;

	@Value("${controller.path.prefix}" + UriConstants.API_V1)
	private String pathPrefixV1;

	@Test
	void testInfectedUserCanReportItselfAsInfected() {
		ReportRequestVo reportRequest = new ReportRequestVo(new ArrayList<VisitVo>());
		ResponseEntity<String> response = restTemplate.postForEntity(pathPrefixV1 + UriConstants.REPORT, this.getReportEntityWithBearer(reportRequest),
				String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
	}

	@Test
	void testWhenReportRequestWithInvalidMediaTypeThenGetUnsupportedMediaType() {
		HttpHeaders headers = new HttpHeaders();
		this.addBearerAuthTo(headers);
		
		ResponseEntity<String> response = restTemplate.postForEntity(
				pathPrefixV1 + UriConstants.REPORT, 
				new HttpEntity<String>("foo", headers),
				String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
		verifyNoMoreInteractions(warningService);
	}

	@Test
	void testWhenReportRequestWithInvalidJsonDataThenGetBadRequest() throws JSONException {
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("id", 1);
		ResponseEntity<String> response = restTemplate.postForEntity(
				pathPrefixV1 + UriConstants.REPORT, 
				new HttpEntity<String>(jsonObject.toString(), this.newJsonHeaderWithBearer()),
				String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		verifyNoMoreInteractions(warningService);
	}

	@Test
	void testWhenReportRequestWithNullVisitTokensThenGetBadRequest() {
		ResponseEntity<String> response = restTemplate.postForEntity(
				pathPrefixV1 + UriConstants.REPORT, 
				this.getReportEntityWithBearer(new ReportRequestVo(null)), 
				String.class);
		
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		verifyNoMoreInteractions(warningService);
	}

	@Test
	void testWhenReportRequestWithNullTimestampTypeThenGetBadRequest() {
		ArrayList<VisitVo> visitQrCodes = new ArrayList<VisitVo>();
		visitQrCodes.add(new VisitVo(null, 
				new QRCodeVo(TokenTypeVo.STATIC, "venueType",VenueCategoryVo.CAT1, 60, "uuid")));
		ReportRequestVo reportRequestVo = new ReportRequestVo(visitQrCodes);
		
		ResponseEntity<String> response = restTemplate.postForEntity(
				pathPrefixV1 + UriConstants.REPORT, 
				this.getReportEntityWithBearer(reportRequestVo), 
				String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		verifyNoMoreInteractions(warningService);
	}

	@Test
	void testWhenReportRequestWithNullQRCodeTypeThenGetBadRequest() {
		ArrayList<VisitVo> visitQrCodes = new ArrayList<VisitVo>();
		visitQrCodes.add(new VisitVo("12345", null));
		ReportRequestVo reportRequestVo = new ReportRequestVo(visitQrCodes);
		
		ResponseEntity<String> response = restTemplate.postForEntity(
				pathPrefixV1 + UriConstants.REPORT, 
				this.getReportEntityWithBearer(reportRequestVo), 
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
		
		ResponseEntity<String> response = restTemplate.postForEntity(
				pathPrefixV1 + UriConstants.REPORT, 
				new HttpEntity<String>(json, this.newJsonHeaderWithBearer()),
				String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		verifyNoMoreInteractions(warningService);
	}
	
	@Test
	void testWhenReportRequestWithNullUuidThenGetBadRequest() {
		ArrayList<VisitVo> visitQrCodes = new ArrayList<VisitVo>();
		visitQrCodes.add(
				new VisitVo("12345", new QRCodeVo(TokenTypeVo.STATIC, "venueType", VenueCategoryVo.CAT1, 60, null)));
		ReportRequestVo reportRequestVo = new ReportRequestVo(visitQrCodes);
		
		ResponseEntity<String> response = restTemplate.postForEntity(
				pathPrefixV1 + UriConstants.REPORT, 
				this.getReportEntityWithBearer(reportRequestVo), 
				String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		verifyNoMoreInteractions(warningService);
	}

	@Test
	void testWhenReportRequestWithInvalidVenueCategoryThenRequestSucceeds() throws JSONException {
		String json = "{\n"
				+ "   \"visits\": [\n"
				+ "     {\n"
				+ "       \"timestamp\": \"3814657232\",\n"
				+ "       \"qrCode\": {\n"
				+ "         \"type\": \"STATIC\",\n"
				+ "         \"venueCategorie\": \"CAT FOO\",\n"
				+ "         \"uuid\": \"9fb80fea-b3ac-4603-b40a-0be8879dbe79\"\n"
				+ "       }\n"
				+ "     }\n"
				+ "   ]\n"
				+ "}\n"
				+ "";
		
		ResponseEntity<String> response = restTemplate.postForEntity(
				pathPrefixV1 + UriConstants.REPORT, 
				new HttpEntity<String>(json, this.newJsonHeaderWithBearer()),
				String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
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

		ResponseEntity<String> response = restTemplate.postForEntity(
				pathPrefixV1 + UriConstants.REPORT, 
				new HttpEntity<String>(json, this.newJsonHeaderWithBearer()), 
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
				+ "         \"venueCategory\": \"CAT1\",\n"
				+ "         \"venueCapacity\": 42,\n"
				+ "         \"uuid\": \"9fb80fea-b3ac-4603-b40a-0be8879dbe79\"\n"
				+ "       }\n"
				+ "     }\n"
				+ "   ]\n"
				+ "}\n"
				+ "";

		ResponseEntity<String> response = restTemplate.postForEntity(
				pathPrefixV1 + UriConstants.REPORT, 
				new HttpEntity<String>(json, this.newJsonHeaderWithBearer()),
				String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
	}

	protected HttpEntity<ReportRequestVo> getReportEntityWithBearer(ReportRequestVo entity) {
		HttpHeaders headers = new HttpHeaders();
		this.addBearerAuthTo(headers);
		return new HttpEntity<>(entity, headers);
	}
	
	protected HttpHeaders newJsonHeader() {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		return headers;
	}
	
	protected HttpHeaders newJsonHeaderWithBearer() {
		HttpHeaders headers = this.newJsonHeader();
		this.addBearerAuthTo(headers);
		return headers;
	}

	protected void addBearerAuthTo(HttpHeaders headers) {
		headers.setBearerAuth("sample-bearer");
	}	
}