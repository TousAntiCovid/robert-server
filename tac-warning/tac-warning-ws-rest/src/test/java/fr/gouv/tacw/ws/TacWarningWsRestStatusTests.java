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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import fr.gouv.tacw.database.model.ExposedStaticVisitEntity;
import fr.gouv.tacw.database.model.RiskLevel;
import fr.gouv.tacw.database.model.ScoreResult;
import fr.gouv.tacw.model.OpaqueVisit;
import fr.gouv.tacw.ws.controller.TACWarningController;
import fr.gouv.tacw.ws.dto.ExposureStatusResponseDto;
import fr.gouv.tacw.ws.dto.ExposureStatusResponseV1Dto;
import fr.gouv.tacw.ws.service.TestTimestampService;
import fr.gouv.tacw.ws.service.WarningService;
import fr.gouv.tacw.ws.utils.BadArgumentsLoggerService;
import fr.gouv.tacw.ws.utils.UriConstants;
import fr.gouv.tacw.ws.vo.ExposureStatusRequestVo;
import fr.gouv.tacw.ws.vo.TokenTypeVo;
import fr.gouv.tacw.ws.vo.VisitTokenVo;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class TacWarningWsRestStatusTests {

    private static final String NULL = "nul";

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WarningService warningService;

    @Autowired
    private TestTimestampService timestampService;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${controller.path.prefix}" + UriConstants.API_V1)
    private String pathPrefixV1;

    @Value("${controller.path.prefix}" + UriConstants.API_V2)
    private String pathPrefixV2;

    @Value("${tacw.rest.max_visits}")
    private int maxVisits;

    @Captor
    private ArgumentCaptor<List<ExposedStaticVisitEntity>> staticTokensCaptor;

    @Captor
    private ArgumentCaptor<Stream<OpaqueVisit>> opaqueVisitsCaptor;

    private ListAppender<ILoggingEvent> tacWarningControllerLoggerAppender;
    private ListAppender<ILoggingEvent> badArgumentLoggerAppender;

    @Test
    public void testCanGetStatusV1() {
        ExposureStatusRequestVo request = new ExposureStatusRequestVo(new ArrayList<VisitTokenVo>());
        when(warningService.getStatus(any())).thenReturn(new ScoreResult(RiskLevel.HIGH, 0, -1));

        ResponseEntity<ExposureStatusResponseV1Dto> response = restTemplate
                .postForEntity(pathPrefixV1 + UriConstants.STATUS, request, ExposureStatusResponseV1Dto.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().isAtRisk()).isTrue();
    }

    @Test
    public void testCanGetStatusWithNullLastContactDate() {
        ExposureStatusRequestVo request = new ExposureStatusRequestVo(new ArrayList<VisitTokenVo>());
        when(warningService.getStatus(any())).thenReturn(new ScoreResult(RiskLevel.HIGH, 0, -1));

        ResponseEntity<ExposureStatusResponseDto> response = restTemplate
                .postForEntity(pathPrefixV2 + UriConstants.STATUS, request, ExposureStatusResponseDto.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getRiskLevel()).isEqualTo(RiskLevel.HIGH);
        assertThat(response.getBody().getLastContactDate()).isNull();
    }

    @Test
    public void testCanGetStatusWithExistingLastContactDate() {
        ExposureStatusRequestVo request = new ExposureStatusRequestVo(new ArrayList<VisitTokenVo>());
        when(warningService.getStatus(any())).thenReturn(new ScoreResult(RiskLevel.HIGH, 0, 1L));

        ResponseEntity<ExposureStatusResponseDto> response = restTemplate
                .postForEntity(pathPrefixV2 + UriConstants.STATUS, request, ExposureStatusResponseDto.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getRiskLevel()).isEqualTo(RiskLevel.HIGH);
        assertThat(response.getBody().getLastContactDate()).isEqualTo("1");
    }

    @Test
    public void testWhenStatusExposureResponseReceivedThenRiskLevelIsAnInt() throws JsonProcessingException, Exception {
        ExposureStatusRequestVo request = new ExposureStatusRequestVo(new ArrayList<VisitTokenVo>());
        when(warningService.getStatus(any())).thenReturn(new ScoreResult(RiskLevel.HIGH, 0, -1));

        mockMvc
                .perform(post(pathPrefixV2 + UriConstants.STATUS)
                        .content(objectMapper.writeValueAsString(request))
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.riskLevel", is(RiskLevel.HIGH.getValue())))
                .andExpect(jsonPath("$.lastContactDate").doesNotExist());;
    }

    @Test
    public void testCanGetStatusWhenOneVisitHasBadFormat() throws JsonMappingException, JsonProcessingException {
        final String VISITS_WITH_ERROR_JSON = "{\"visitTokens\":[{\"type\":\"STATIC\",\"payload\":\"payload\",\"timestamp\":\"timestamp\"},{\"type\":\"FOO\",\"payload\":\"payload\",\"timestamp\":\"timestamp\"}]}";
        ExposureStatusRequestVo esr = objectMapper.readValue(VISITS_WITH_ERROR_JSON, ExposureStatusRequestVo.class);
        when(warningService.getStatus(any())).thenReturn(new ScoreResult(RiskLevel.HIGH, 0, -1));

        ResponseEntity<ExposureStatusResponseDto> response = restTemplate
                .postForEntity(pathPrefixV2 + UriConstants.STATUS, esr, ExposureStatusResponseDto.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getRiskLevel()).isEqualTo(RiskLevel.HIGH);
        assertThat(response.getBody().getLastContactDate()).isNull();
        assertThat(tacWarningControllerLoggerAppender.list.size()).isEqualTo(2); // common log + filter
        ILoggingEvent log = tacWarningControllerLoggerAppender.list.get(1);
        assertThat(log.getMessage()).contains(
                String.format(TACWarningController.MALFORMED_VISIT_LOG_MESSAGE, 1, 2));
        assertThat(log.getLevel()).isEqualTo(Level.INFO);
        assertThat(badArgumentLoggerAppender.list.size()).isEqualTo(1);
        assertThat(badArgumentLoggerAppender.list.get(0).getMessage()).contains("type", NULL);
    }

    @Test
    public void testCanGetStatusWhenVisitHasExtraField() throws JsonMappingException, JsonProcessingException {
        when(warningService.getStatus(any())).thenReturn(new ScoreResult(RiskLevel.HIGH, 0, -1));

        String json = "{\n"
                + "  \"visitTokens\" : [ {\n"
                + "    \"type\" : \"STATIC\",\n"
                + "    \"extrafield\" : \"foo\",\n"
                + "    \"payload\" : \"0YWN3LXR5cGUiOiJTVEFUSUMiLCJ0YWN3LXZlcnNpb24iOjEsImVyc\"\n"
                + "  } ]\n"
                + "}";
        ResponseEntity<ExposureStatusResponseDto> response = restTemplate.postForEntity(
                pathPrefixV2 + UriConstants.STATUS,
                new HttpEntity<String>(json, this.newJsonHeader()),
                ExposureStatusResponseDto.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getRiskLevel()).isEqualTo(RiskLevel.HIGH);
        assertThat(response.getBody().getLastContactDate()).isNull();
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
                String.format(TACWarningController.STATUS_LOG_MESSAGE, maxVisits));
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
                String.format(TACWarningController.MAX_VISITS_FILTER_LOG_MESSAGE, 1, maxVisits+1));
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
        assertThat(badArgumentLoggerAppender.list.size()).isEqualTo(1);
        assertThat(badArgumentLoggerAppender.list.get(0).getMessage()).contains("visitTokens", NULL);
    }

    @Test
    public void testWhenExposureStatusRequestWithNullTokenTypeThenVisitIsIgnored() {
        when(warningService.getStatus(any())).thenReturn(new ScoreResult(RiskLevel.HIGH, 0, -1));
        ArrayList<VisitTokenVo> visitTokens = new ArrayList<VisitTokenVo>();
        visitTokens.add(new VisitTokenVo(null, "payload", "123456789"));
        ExposureStatusRequestVo entity = new ExposureStatusRequestVo(visitTokens);
        ResponseEntity<ExposureStatusResponseDto> response = restTemplate.postForEntity(
                pathPrefixV2 + UriConstants.STATUS,
                entity,
                ExposureStatusResponseDto.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(warningService).getStatus(opaqueVisitsCaptor.capture());
        assertThat(opaqueVisitsCaptor.getValue()).isEmpty();
        assertThatControllerLogContainsOneMalformedVisit();
        assertThat(badArgumentLoggerAppender.list.size()).isEqualTo(1);
        assertThat(badArgumentLoggerAppender.list.get(0).getMessage()).contains("type", NULL);
    }

    @Test
    public void testWhenExposureStatusRequestWithInvalidTokenTypeThenVisitIsIgnored() throws JSONException {
        when(warningService.getStatus(any())).thenReturn(new ScoreResult(RiskLevel.HIGH, 0, -1));
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

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThatControllerLogContainsOneMalformedVisit();
        assertThat(badArgumentLoggerAppender.list.size()).isEqualTo(1);
        assertThat(badArgumentLoggerAppender.list.get(0).getMessage()).contains("type", NULL);
    }

    @Test
    public void testWhenExposureStatusRequestWithNullPayloadThenVisitIsIgnored() {
        when(warningService.getStatus(any())).thenReturn(new ScoreResult(RiskLevel.HIGH, 0, -1));
        ArrayList<VisitTokenVo> visitTokens = new ArrayList<VisitTokenVo>();
        visitTokens.add(new VisitTokenVo(TokenTypeVo.STATIC, null, "123456789"));
        ExposureStatusRequestVo entity = new ExposureStatusRequestVo(visitTokens);
        ResponseEntity<ExposureStatusResponseDto> response = restTemplate.postForEntity(
                pathPrefixV2 + UriConstants.STATUS,
                entity,
                ExposureStatusResponseDto.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(warningService).getStatus(opaqueVisitsCaptor.capture());
        assertThat(opaqueVisitsCaptor.getValue()).isEmpty();
        assertThatControllerLogContainsOneMalformedVisit();
        assertThat(badArgumentLoggerAppender.list.size()).isEqualTo(1);
        assertThat(badArgumentLoggerAppender.list.get(0).getMessage()).contains("payload", NULL);
    }

    @Test
    public void testWhenExposureStatusRequestWithNullTimestampThenVisitIsIgnored() {
        when(warningService.getStatus(any())).thenReturn(new ScoreResult(RiskLevel.HIGH, 0, -1));
        ArrayList<VisitTokenVo> visitTokens = new ArrayList<VisitTokenVo>();
        visitTokens.add(new VisitTokenVo(TokenTypeVo.STATIC, "payload", null));
        ExposureStatusRequestVo entity = new ExposureStatusRequestVo(visitTokens);
        ResponseEntity<ExposureStatusResponseDto> response = restTemplate.postForEntity(
                pathPrefixV2 + UriConstants.STATUS,
                entity,
                ExposureStatusResponseDto.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(warningService).getStatus(opaqueVisitsCaptor.capture());
        assertThat(opaqueVisitsCaptor.getValue()).isEmpty();
        assertThatControllerLogContainsOneMalformedVisit();
        assertThat(badArgumentLoggerAppender.list.size()).isEqualTo(1);
        assertThat(badArgumentLoggerAppender.list.get(0).getMessage()).contains("timestamp", NULL);
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
        this.setUpLogHandler();
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("id", 1);
        ResponseEntity<ExposureStatusResponseDto> response = restTemplate.postForEntity(
                pathPrefixV2 + UriConstants.STATUS,
                new HttpEntity<String>(jsonObject.toString(), this.newJsonHeader()),
                ExposureStatusResponseDto.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verifyNoMoreInteractions(warningService);
        assertThat(badArgumentLoggerAppender.list.size()).isEqualTo(1);

    }

    @BeforeEach
    private void setUpLogHandler() {
        this.tacWarningControllerLoggerAppender = new ListAppender<>();
        this.tacWarningControllerLoggerAppender.start();
        ((Logger) LoggerFactory.getLogger(TACWarningController.class)).addAppender(this.tacWarningControllerLoggerAppender);

        this.badArgumentLoggerAppender = new ListAppender<>();
        this.badArgumentLoggerAppender.start();
        ((Logger) LoggerFactory.getLogger(BadArgumentsLoggerService.class)).addAppender(this.badArgumentLoggerAppender);
    }

    protected void assertThatControllerLogContainsOneMalformedVisit() {
        assertThat(this.tacWarningControllerLoggerAppender.list.size()).isEqualTo(2); // common log + filter
        ILoggingEvent log = this.tacWarningControllerLoggerAppender.list.get(1); // first log is nb visits for ESR
        assertThat(log.getMessage()).contains(
                String.format(TACWarningController.MALFORMED_VISIT_LOG_MESSAGE, 1, 1));
        assertThat(log.getLevel()).isEqualTo(Level.INFO);
    }

    protected HttpHeaders newJsonHeader() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

}
