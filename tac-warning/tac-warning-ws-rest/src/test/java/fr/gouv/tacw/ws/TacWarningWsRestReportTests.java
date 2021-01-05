package fr.gouv.tacw.ws;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.security.KeyPair;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import fr.gouv.tacw.ws.configuration.TacWarningWsRestConfiguration;
import fr.gouv.tacw.ws.controller.TACWarningController;
import fr.gouv.tacw.ws.service.AuthorizationService;
import fr.gouv.tacw.ws.service.impl.WarningServiceImpl;
import fr.gouv.tacw.ws.utils.BadArgumentsLoggerService;
import fr.gouv.tacw.ws.utils.UriConstants;
import fr.gouv.tacw.ws.vo.QRCodeVo;
import fr.gouv.tacw.ws.vo.ReportRequestVo;
import fr.gouv.tacw.ws.vo.TokenTypeVo;
import fr.gouv.tacw.ws.vo.VenueCategoryVo;
import fr.gouv.tacw.ws.vo.VenueTypeVo;
import fr.gouv.tacw.ws.vo.VisitVo;
import io.jsonwebtoken.io.Encoders;
import io.jsonwebtoken.security.Keys;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class TacWarningWsRestReportTests {
	private static final String NULL = "nul";

	@Value("${controller.path.prefix}" + UriConstants.API_V2)
	private String pathPrefixV2;

	@Autowired
	private TestRestTemplate restTemplate;

	@MockBean
	private WarningServiceImpl warningService;
	
	@MockBean
	private TacWarningWsRestConfiguration configuration;
	
	@Captor
	private ArgumentCaptor<List<VisitVo>> visitsCaptor;
	
	private KeyPair keyPair;

    private ListAppender<ILoggingEvent> tacWarningControllerLoggerAppender;

    private ListAppender<ILoggingEvent> badArgumentLoggerAppender;
	
	@BeforeEach
	public void setUp() {
		keyPair = Keys.keyPairFor(AuthorizationService.algo);

		when(this.configuration.isJwtReportAuthorizationDisabled()).thenReturn(false);
		when(this.configuration.getRobertJwtPublicKey()).thenReturn(Encoders.BASE64.encode(keyPair.getPublic().getEncoded()));
		
		this.tacWarningControllerLoggerAppender = new ListAppender<>();
		this.tacWarningControllerLoggerAppender.start();
		((Logger) LoggerFactory.getLogger(TACWarningController.class)).addAppender(this.tacWarningControllerLoggerAppender);

		this.badArgumentLoggerAppender = new ListAppender<>();
		this.badArgumentLoggerAppender.start();
		((Logger) LoggerFactory.getLogger(BadArgumentsLoggerService.class)).addAppender(this.badArgumentLoggerAppender);
	}
	
	@Test
	public void testInfectedUserCanReportItselfAsInfected() {
		ReportRequestVo reportRequest = new ReportRequestVo(new ArrayList<VisitVo>());
		ResponseEntity<String> response = restTemplate.postForEntity(pathPrefixV2 + UriConstants.REPORT, this.getReportEntityWithBearer(reportRequest),
				String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
	}

    @Test
    public void testInfectedUserCanReportItselfAsInfectedWhenOneVisitHasBadFormat() throws JsonMappingException, JsonProcessingException {
        ArrayList<VisitVo> visitQrCodes = new ArrayList<VisitVo>();
        visitQrCodes.add(new VisitVo("6789", 
                new QRCodeVo(TokenTypeVo.STATIC, VenueTypeVo.L, VenueCategoryVo.CAT2, 100, "uuid2")));
        visitQrCodes.add(new VisitVo(null, 
                new QRCodeVo(TokenTypeVo.STATIC, VenueTypeVo.N, VenueCategoryVo.CAT1, 60, "uuid"))); // null timestamp
        visitQrCodes.add(new VisitVo("12345", 
                new QRCodeVo(TokenTypeVo.STATIC, VenueTypeVo.N, VenueCategoryVo.CAT1, 60, "uuid")));
        ReportRequestVo reportRequest = new ReportRequestVo(visitQrCodes);

        ResponseEntity<String> response = restTemplate.postForEntity(pathPrefixV2 + UriConstants.REPORT, this.getReportEntityWithBearer(reportRequest),
            String.class);
                
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(warningService).reportVisitsWhenInfected(visitsCaptor.capture());
        assertThat(visitsCaptor.getValue().size()).isEqualTo(2);
        assertThat(tacWarningControllerLoggerAppender.list.size()).isEqualTo(2); // common log + filter
        ILoggingEvent log = tacWarningControllerLoggerAppender.list.get(1);
        assertThat(log.getMessage()).contains(
                String.format(TACWarningController.MALFORMED_VISIT_LOG_MESSAGE, 1, 3));
        assertThat(log.getLevel()).isEqualTo(Level.INFO);
        assertThat(badArgumentLoggerAppender.list.size()).isEqualTo(1);
        assertThat(badArgumentLoggerAppender.list.get(0).getMessage()).contains("timestamp", NULL);
        
    }
        
	@Test
	public void testWhenReportRequestWithMissingAuthenticationThenGetBadRequest() {
		ReportRequestVo reportRequest = new ReportRequestVo(new ArrayList<VisitVo>());
		ResponseEntity<String> response = restTemplate.postForEntity(pathPrefixV2 + UriConstants.REPORT, reportRequest,
				String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
	}
	
	@Test
	public void testWhenReportRequestWithNonValidAuthenticationThenGetUnauthorized() {
		ReportRequestVo reportRequest = new ReportRequestVo(new ArrayList<VisitVo>());
		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth("invalid JWT token");
		HttpEntity<ReportRequestVo> reportEntity = new HttpEntity<>(reportRequest, headers);
		
		ResponseEntity<String> response = restTemplate.postForEntity(pathPrefixV2 + UriConstants.REPORT, reportEntity,
				String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
	}

	@Test
	public void testWhenReportRequestWithInvalidMediaTypeThenGetUnsupportedMediaType() {
		HttpHeaders headers = new HttpHeaders();
		new HttpJwtHeaderUtils(keyPair.getPrivate()).addBearerAuthTo(headers);
		
		ResponseEntity<String> response = restTemplate.postForEntity(
				pathPrefixV2 + UriConstants.REPORT, 
				new HttpEntity<String>("foo", headers),
				String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
		verifyNoMoreInteractions(warningService);
	}

	@Test
	public void testWhenReportRequestWithInvalidJsonDataThenGetBadRequest() throws JSONException {
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("id", 1);
		ResponseEntity<String> response = restTemplate.postForEntity(
				pathPrefixV2 + UriConstants.REPORT, 
				new HttpEntity<String>(jsonObject.toString(), this.newJsonHeaderWithBearer()),
				String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		verifyNoMoreInteractions(warningService);
	}

	@Test
	public void testWhenReportRequestWithNullVisitTokensThenGetBadRequest() {
		ResponseEntity<String> response = restTemplate.postForEntity(
				pathPrefixV2 + UriConstants.REPORT, 
				this.getReportEntityWithBearer(new ReportRequestVo(null)), 
				String.class);
		
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		verifyNoMoreInteractions(warningService);
	}

	@Test
	public void testWhenReportRequestWithNullTimestampTypeThenVisitIsFiltered() {
		ArrayList<VisitVo> visitQrCodes = new ArrayList<VisitVo>();
		visitQrCodes.add(new VisitVo(null, 
				new QRCodeVo(TokenTypeVo.STATIC, VenueTypeVo.N, VenueCategoryVo.CAT1, 60, "uuid")));
		ReportRequestVo reportRequestVo = new ReportRequestVo(visitQrCodes);
		
		ResponseEntity<String> response = restTemplate.postForEntity(
				pathPrefixV2 + UriConstants.REPORT, 
				this.getReportEntityWithBearer(reportRequestVo), 
				String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(warningService).reportVisitsWhenInfected(visitsCaptor.capture());
        assertThat(visitsCaptor.getValue().size()).isEqualTo(0);
        assertThatControllerLogContainsOneMalformedVisit();
        assertThat(badArgumentLoggerAppender.list.size()).isEqualTo(1);
        assertThat(badArgumentLoggerAppender.list.get(0).getMessage()).contains("timestamp", NULL);
	}

	@Test
	public void testWhenReportRequestWithNullQRCodeTypeThenVisitIsFiltered() {
		ArrayList<VisitVo> visitQrCodes = new ArrayList<VisitVo>();
		visitQrCodes.add(new VisitVo("12345", null));
		ReportRequestVo reportRequestVo = new ReportRequestVo(visitQrCodes);
		
		ResponseEntity<String> response = restTemplate.postForEntity(
				pathPrefixV2 + UriConstants.REPORT, 
				this.getReportEntityWithBearer(reportRequestVo), 
				String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(warningService).reportVisitsWhenInfected(visitsCaptor.capture());
        assertThat(visitsCaptor.getValue().size()).isEqualTo(0);
        assertThatControllerLogContainsOneMalformedVisit();
        assertThat(badArgumentLoggerAppender.list.size()).isEqualTo(1);
        assertThat(badArgumentLoggerAppender.list.get(0).getMessage()).contains("qrCode", NULL);
	}

	@Test
	public void testWhenReportRequestWithInvalidQRCodeTypeThenGetVisitIsFiltered() throws JSONException {
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
				pathPrefixV2 + UriConstants.REPORT, 
				new HttpEntity<String>(json, this.newJsonHeaderWithBearer()),
				String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(warningService).reportVisitsWhenInfected(visitsCaptor.capture());
        assertThat(visitsCaptor.getValue().size()).isEqualTo(0);
        assertThatControllerLogContainsOneMalformedVisit();
        assertThat(badArgumentLoggerAppender.list.size()).isEqualTo(1);
        assertThat(badArgumentLoggerAppender.list.get(0).getMessage()).contains("qrCode", NULL);
	}
	
	@Test
	public void testWhenReportRequestWithNullUuidThenVisitIsFiltered() {
		ArrayList<VisitVo> visitQrCodes = new ArrayList<VisitVo>();
		visitQrCodes.add(
				new VisitVo("12345", new QRCodeVo(TokenTypeVo.STATIC, VenueTypeVo.N, VenueCategoryVo.CAT1, 60, null)));
		ReportRequestVo reportRequestVo = new ReportRequestVo(visitQrCodes);
		
		ResponseEntity<String> response = restTemplate.postForEntity(
				pathPrefixV2 + UriConstants.REPORT, 
				this.getReportEntityWithBearer(reportRequestVo), 
				String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		verify(warningService).reportVisitsWhenInfected(visitsCaptor.capture());
		assertThat(visitsCaptor.getValue().size()).isEqualTo(0);
        assertThatControllerLogContainsOneMalformedVisit();
        assertThat(badArgumentLoggerAppender.list.size()).isEqualTo(1);
        assertThat(badArgumentLoggerAppender.list.get(0).getMessage()).contains("uuid", NULL);
	}
	
	@Test
	public void testWhenReportRequestWithNullVenueCategoryOrVenueTypeOrVenueCapacityThenRequestSucceeds() {
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
				pathPrefixV2 + UriConstants.REPORT, 
				new HttpEntity<String>(json, this.newJsonHeaderWithBearer()), 
				String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
	}
	
	@Test
	public void testWhenReportRequestWithValidVisitThenRequestSucceed() throws JSONException {
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
				pathPrefixV2 + UriConstants.REPORT, 
				new HttpEntity<String>(json, this.newJsonHeaderWithBearer()),
				String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
	}


    protected void assertThatControllerLogContainsOneMalformedVisit() {
        assertThat(tacWarningControllerLoggerAppender.list.size()).isEqualTo(2); // common log + filter
        ILoggingEvent log = tacWarningControllerLoggerAppender.list.get(1); // first log is nb visits for report
        assertThat(log.getMessage()).contains(
                String.format(TACWarningController.MALFORMED_VISIT_LOG_MESSAGE, 1, 1));
        assertThat(log.getLevel()).isEqualTo(Level.INFO);
    }
    
	private MultiValueMap<String, String> newJsonHeaderWithBearer() {
		return new HttpJwtHeaderUtils(keyPair.getPrivate()).newJsonHeaderWithBearer();
	}

	private Object getReportEntityWithBearer(ReportRequestVo reportRequest) {
		return new HttpJwtHeaderUtils(keyPair.getPrivate()).getReportEntityWithBearer(reportRequest);
	}
}