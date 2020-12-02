package fr.gouv.tacw.ws;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

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

import fr.gouv.tacw.ws.dto.ExposureStatusResponseDto;
import fr.gouv.tacw.ws.service.WarningService;
import fr.gouv.tacw.ws.utils.UriConstants;
import fr.gouv.tacw.ws.vo.ExposureStatusRequestVo;
import fr.gouv.tacw.ws.vo.TokenTypeVo;
import fr.gouv.tacw.ws.vo.VisitTokenVo;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@MockBean(WarningService.class)
class TacWarningWsRestStatusTests {
	@Autowired
	private TestRestTemplate restTemplate;

	@Autowired
	private WarningService warningService;

	@Value("${controller.path.prefix}" + UriConstants.API_V1)
	private String pathPrefixV1;

	@Test
	void testCanGetStatus() {
		ExposureStatusRequestVo request = new ExposureStatusRequestVo(new ArrayList<VisitTokenVo>());
		when(warningService.getStatus(any(), anyLong())).thenReturn(true);

		ResponseEntity<ExposureStatusResponseDto> response = restTemplate
				.postForEntity(pathPrefixV1 + UriConstants.STATUS, request, ExposureStatusResponseDto.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
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
		visitTokens.add(new VisitTokenVo(null, "payload", "123456789"));
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
		ResponseEntity<ExposureStatusResponseDto> response = restTemplate.postForEntity(
				pathPrefixV1 + UriConstants.STATUS, 
				new HttpEntity<String>(json, this.newJsonHeader()),
				ExposureStatusResponseDto.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		verifyNoMoreInteractions(warningService);
	}

	@Test
	void testWhenExposureStatusRequestWithNullPayloadThenGetBadRequest() {
		ArrayList<VisitTokenVo> visitTokens = new ArrayList<VisitTokenVo>();
		visitTokens.add(new VisitTokenVo(TokenTypeVo.STATIC, null, "123456789"));
		ExposureStatusRequestVo entity = new ExposureStatusRequestVo(visitTokens);
		ResponseEntity<ExposureStatusResponseDto> response = restTemplate.postForEntity(
				pathPrefixV1 + UriConstants.STATUS, 
				entity, 
				ExposureStatusResponseDto.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		verifyNoMoreInteractions(warningService);
	}

	@Test
	void testWhenExposureStatusRequestWithNullTiemstampThenGetBadRequest() {
		ArrayList<VisitTokenVo> visitTokens = new ArrayList<VisitTokenVo>();
		visitTokens.add(new VisitTokenVo(TokenTypeVo.STATIC, "payload", null));
		ExposureStatusRequestVo entity = new ExposureStatusRequestVo(visitTokens);
		ResponseEntity<ExposureStatusResponseDto> response = restTemplate.postForEntity(
				pathPrefixV1 + UriConstants.STATUS, 
				entity, 
				ExposureStatusResponseDto.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		verifyNoMoreInteractions(warningService);
	}

	@Test
	void testWhenExposureStatusRequestWithInvalidMediaTypeThenGetUnsupportedMediaType() {
		ResponseEntity<ExposureStatusResponseDto> response = restTemplate.postForEntity(
				pathPrefixV1 + UriConstants.STATUS, 
				new HttpEntity<String>("foo"),
				ExposureStatusResponseDto.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
		verifyNoMoreInteractions(warningService);
	}

	@Test
	void testWhenExposureStatusRequestsWithInvalidJsonDataThenGetBadRequest() throws JSONException {
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("id", 1);
		ResponseEntity<ExposureStatusResponseDto> response = restTemplate.postForEntity(
				pathPrefixV1 + UriConstants.STATUS, 
				new HttpEntity<String>(jsonObject.toString(), this.newJsonHeader()),
				ExposureStatusResponseDto.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		verifyNoMoreInteractions(warningService);
	}
	
	protected HttpHeaders newJsonHeader() {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		return headers;
	}
}