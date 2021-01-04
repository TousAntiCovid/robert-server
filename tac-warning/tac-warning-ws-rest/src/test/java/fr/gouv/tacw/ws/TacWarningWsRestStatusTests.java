package fr.gouv.tacw.ws;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import fr.gouv.tacw.database.model.ExposedStaticVisitEntity;
import fr.gouv.tacw.database.utils.TimeUtils;
import fr.gouv.tacw.model.OpaqueVisit;
import fr.gouv.tacw.model.RiskLevel;
import fr.gouv.tacw.ws.controller.TACWarningController;
import fr.gouv.tacw.ws.dto.ExposureStatusResponseDto;
import fr.gouv.tacw.ws.service.TestTimestampService;
import fr.gouv.tacw.ws.service.WarningService;
import fr.gouv.tacw.ws.utils.UriConstants;
import fr.gouv.tacw.ws.vo.ExposureStatusRequestVo;
import fr.gouv.tacw.ws.vo.TokenTypeVo;
import fr.gouv.tacw.ws.vo.VisitTokenVo;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class TacWarningWsRestStatusTests {
    @Autowired
    private TestRestTemplate restTemplate;

    @MockBean
    private WarningService warningService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestTimestampService timestampService;

    @Value("${controller.path.prefix}" + UriConstants.API_V2)
    private String pathPrefixV2;

    @Value("${tacw.rest.max_visits}")
    private int maxVisits;

    @Captor
    private ArgumentCaptor<List<ExposedStaticVisitEntity>> staticTokensCaptor;

    @Captor
    private ArgumentCaptor<Stream<OpaqueVisit>> opaqueVisitsCaptor;

    private ListAppender<ILoggingEvent> tacWarningControllerLoggerAppender;

    @Test
    public void testCanGetStatus() throws Exception {
        ExposureStatusRequestVo request = new ExposureStatusRequestVo(new ArrayList<VisitTokenVo>());
        when(warningService.getStatus(any())).thenReturn(RiskLevel.TACW_HIGH);

        ResponseEntity<ExposureStatusResponseDto> response = restTemplate
                .postForEntity(pathPrefixV2 + UriConstants.STATUS, request, ExposureStatusResponseDto.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        assertThat(response.getBody().getRiskLevel()).isEqualTo(RiskLevel.TACW_HIGH);
        Instant instant = Instant.ofEpochSecond( Long.parseLong(response.getBody().getLastContactDate())
                - TimeUtils.SECONDS_FROM_01_01_1900 );
        assertThat(instant).isBefore(Instant.now());
    }

    @Test
    public void testWhenStatusExposureResponseReceivedThenRiskLevelIsAnInt() throws Exception {
        ExposureStatusRequestVo request = new ExposureStatusRequestVo(new ArrayList<VisitTokenVo>());
        when(warningService.getStatus(any())).thenReturn(RiskLevel.TACW_HIGH);

        mockMvc.perform(post(pathPrefixV2 + UriConstants.STATUS)
                .content(objectMapper.writeValueAsString(request))
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.riskLevel", is(RiskLevel.TACW_HIGH.getValue())));
    }

    @Test
    public void testCanCheckStatusWithMaxVisits() {
        this.setUpLogHandler();
        List<VisitTokenVo> visits = new ArrayList<VisitTokenVo>(maxVisits);
        IntStream.rangeClosed(1, maxVisits)
        .forEach(i -> visits.add(
                new VisitTokenVo(TokenTypeVo.STATIC, "0YWN3LXR5cGUiOiJTVEFUSUMiLCJ0YWN3LXZlcnNpb24iOjEsImVyc", timestampService.validTimestampString())));

        restTemplate.postForEntity(
                pathPrefixV2 + UriConstants.STATUS, 
                new ExposureStatusRequestVo(visits), 
                ExposureStatusResponseDto.class);

        verify(warningService).getStatus(opaqueVisitsCaptor.capture());
        assertThat(opaqueVisitsCaptor.getValue().count()).isEqualTo(maxVisits);
        assertThat(tacWarningControllerLoggerAppender.list.size()).isEqualTo(1); // no second log with filter
        ILoggingEvent log = tacWarningControllerLoggerAppender.list.get(0);
        assertThat(log.getMessage()).contains(
                String.format("Exposure status request for %d visits", maxVisits));
        assertThat(log.getLevel()).isEqualTo(Level.INFO);
    }

    @Test
    public void testCannotCheckStatusWithMoreThanMaxVisits() {
        this.setUpLogHandler();
        List<VisitTokenVo> visits = new ArrayList<VisitTokenVo>(maxVisits);
        IntStream.rangeClosed(1, maxVisits + 1)
        .forEach(i -> visits.add(
                new VisitTokenVo(TokenTypeVo.STATIC, "0YWN3LXR5cGUiOiJTVEFUSUMiLCJ0YWN3LXZlcnNpb24iOjEsImVyc", timestampService.validTimestampString())));

        restTemplate.postForEntity(
                pathPrefixV2 + UriConstants.STATUS, 
                new ExposureStatusRequestVo(visits), 
                ExposureStatusResponseDto.class);

        verify(warningService).getStatus(opaqueVisitsCaptor.capture());
        assertThat(opaqueVisitsCaptor.getValue().count()).isEqualTo(maxVisits);
        ILoggingEvent log = tacWarningControllerLoggerAppender.list.get(1); // first log is nb visits for ESR
        assertThat(log.getMessage()).contains(
                String.format("Filtered out %d visits of %d", 1, maxVisits+1));
        assertThat(log.getLevel()).isEqualTo(Level.INFO);	
    }

    @Test
    public void testWhenExposureStatusRequestWithNullVisitTokensThenGetBadRequest() {
        ResponseEntity<ExposureStatusResponseDto> response = restTemplate.postForEntity(
                pathPrefixV2 + UriConstants.STATUS, 
                new ExposureStatusRequestVo(null), 
                ExposureStatusResponseDto.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verifyNoMoreInteractions(warningService);
    }

    @Test
    public void testWhenExposureStatusRequestWithNullTokenTypeThenGetBadRequest() {
        ArrayList<VisitTokenVo> visitTokens = new ArrayList<VisitTokenVo>();
        visitTokens.add(new VisitTokenVo(null, "payload", "123456789"));
        ExposureStatusRequestVo entity = new ExposureStatusRequestVo(visitTokens);
        ResponseEntity<ExposureStatusResponseDto> response = restTemplate.postForEntity(
                pathPrefixV2 + UriConstants.STATUS, 
                entity, 
                ExposureStatusResponseDto.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verifyNoMoreInteractions(warningService);
    }

    @Test
    public void testWhenExposureStatusRequestWithInvalidTokenTypeThenGetBadRequest() throws JSONException {
        String json = "{\n"
                + "  \"visitTokens\" : [ {\n"
                + "    \"type\" : \"UNKNOWN\",\n"
                + "    \"payload\" : \"0YWN3LXR5cGUiOiJTVEFUSUMiLCJ0YWN3LXZlcnNpb24iOjEsImVyc\"\n"
                + "  } ]\n"
                + "}";
        ResponseEntity<ExposureStatusResponseDto> response = restTemplate.postForEntity(
                pathPrefixV2 + UriConstants.STATUS, 
                new HttpEntity<String>(json, this.newJsonHeader()),
                ExposureStatusResponseDto.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verifyNoMoreInteractions(warningService);
    }

    @Test
    public void testWhenExposureStatusRequestWithNullPayloadThenGetBadRequest() {
        ArrayList<VisitTokenVo> visitTokens = new ArrayList<VisitTokenVo>();
        visitTokens.add(new VisitTokenVo(TokenTypeVo.STATIC, null, "123456789"));
        ExposureStatusRequestVo entity = new ExposureStatusRequestVo(visitTokens);
        ResponseEntity<ExposureStatusResponseDto> response = restTemplate.postForEntity(
                pathPrefixV2 + UriConstants.STATUS, 
                entity, 
                ExposureStatusResponseDto.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verifyNoMoreInteractions(warningService);
    }

    @Test
    public void testWhenExposureStatusRequestWithNullTiemstampThenGetBadRequest() {
        ArrayList<VisitTokenVo> visitTokens = new ArrayList<VisitTokenVo>();
        visitTokens.add(new VisitTokenVo(TokenTypeVo.STATIC, "payload", null));
        ExposureStatusRequestVo entity = new ExposureStatusRequestVo(visitTokens);
        ResponseEntity<ExposureStatusResponseDto> response = restTemplate.postForEntity(
                pathPrefixV2 + UriConstants.STATUS, 
                entity, 
                ExposureStatusResponseDto.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verifyNoMoreInteractions(warningService);
    }

    @Test
    public void testWhenExposureStatusRequestWithInvalidMediaTypeThenGetUnsupportedMediaType() {
        ResponseEntity<ExposureStatusResponseDto> response = restTemplate.postForEntity(
                pathPrefixV2 + UriConstants.STATUS, 
                new HttpEntity<String>("foo"),
                ExposureStatusResponseDto.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        verifyNoMoreInteractions(warningService);
    }

    @Test
    public void testWhenExposureStatusRequestsWithInvalidJsonDataThenGetBadRequest() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("id", 1);
        ResponseEntity<ExposureStatusResponseDto> response = restTemplate.postForEntity(
                pathPrefixV2 + UriConstants.STATUS, 
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

    private void setUpLogHandler() {
        Logger tacWarningControllerLogger = (Logger) LoggerFactory.getLogger(TACWarningController.class);

        tacWarningControllerLoggerAppender = new ListAppender<>();
        tacWarningControllerLoggerAppender.start();

        tacWarningControllerLogger.addAppender(tacWarningControllerLoggerAppender);
    }

}