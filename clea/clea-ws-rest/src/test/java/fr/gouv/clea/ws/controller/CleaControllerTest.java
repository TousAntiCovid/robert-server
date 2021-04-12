package fr.gouv.clea.ws.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import fr.gouv.clea.ws.dto.ApiError;
import fr.gouv.clea.ws.service.impl.ReportService;
import fr.gouv.clea.ws.utils.UriConstants;
import fr.gouv.clea.ws.vo.ReportRequest;
import fr.gouv.clea.ws.vo.Visit;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
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

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class CleaControllerTest {

    @Captor
    private ArgumentCaptor<ReportRequest> reportRequestArgumentCaptor;
    @Value("${controller.path.prefix}" + UriConstants.API_V1)
    private String pathPrefix;
    @Autowired
    private TestRestTemplate restTemplate;
    @Autowired
    private ObjectMapper objectMapper;
    @MockBean
    private ReportService reportService;

    static HttpHeaders newJsonHeader() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    @BeforeEach
    void init() {
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Test
    void testInfectedUserCanReportHimselfAsInfected() {
        List<Visit> visits = List.of(new Visit("qrCode", 0L));
        HttpEntity<ReportRequest> request = new HttpEntity<>(new ReportRequest(visits, 0L), newJsonHeader());
        ResponseEntity<String> response = restTemplate.postForEntity(pathPrefix + UriConstants.REPORT, request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void testWhenReportRequestWithInvalidMediaTypeThenGetUnsupportedMediaType() {
        ResponseEntity<String> response = restTemplate.postForEntity(pathPrefix + UriConstants.REPORT, "foo", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        verifyNoMoreInteractions(reportService);
    }

    @Test
    void testWhenReportRequestWithNullVisitListThenGetBadRequest() {
        HttpEntity<ReportRequest> request = new HttpEntity<>(new ReportRequest(null, 0L), newJsonHeader());
        ResponseEntity<String> response = restTemplate.postForEntity(pathPrefix + UriConstants.REPORT, request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verifyNoMoreInteractions(reportService);
    }

    @Test
    void testWhenReportRequestWithInvalidJsonDataThenGetBadRequest() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("id", 1);
        ResponseEntity<String> response = restTemplate.postForEntity(
                pathPrefix + UriConstants.REPORT,
                new HttpEntity<>(jsonObject.toString(), newJsonHeader()),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verifyNoMoreInteractions(reportService);
    }

    @Test
    @DisplayName("when pivotDate is null, reject everything")
    void nullPivotDate() throws JsonProcessingException {
        List<Visit> visits = List.of(new Visit(RandomStringUtils.randomAlphanumeric(20), RandomUtils.nextLong()));
        HttpEntity<ReportRequest> request = new HttpEntity<>(new ReportRequest(visits, null), newJsonHeader());
        ResponseEntity<String> response = restTemplate.postForEntity(pathPrefix + UriConstants.REPORT, request, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verifyNoMoreInteractions(reportService);
        ApiError apiError = objectMapper.readValue(response.getBody(), ApiError.class);
        assertThat(apiError.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(apiError.getTimestamp()).isBefore(Instant.now());
        assertThat(apiError.getMessage()).isEqualTo("Invalid request");
        assertThat(apiError.getValidationErrors().size()).isEqualTo(1);
        assertThat(apiError.getValidationErrors().stream().findFirst()).isPresent();
        assertThat(apiError.getValidationErrors().stream().findFirst().get().getObject()).isEqualTo("ReportRequest");
        assertThat(apiError.getValidationErrors().stream().findFirst().get().getField()).isEqualTo("pivotDateAsNtpTimestamp");
        assertThat(apiError.getValidationErrors().stream().findFirst().get().getRejectedValue()).isNull();
        assertThat(apiError.getValidationErrors().stream().findFirst().get().getMessage()).contains("nul");
    }

    @Test
    @DisplayName("when pivotDate is not numeric, reject everything")
    void notNumericPivotDate() throws JsonProcessingException {
        ReportRequest reportRequest = new ReportRequest(List.of(new Visit(RandomStringUtils.randomAlphanumeric(20), 1L)), 0L);
        String json = objectMapper.writeValueAsString(reportRequest);
        String badJson = json.replace("0", "a");
        HttpEntity<String> request = new HttpEntity<>(badJson, newJsonHeader());
        ResponseEntity<String> response = restTemplate.postForEntity(pathPrefix + UriConstants.REPORT, request, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verifyNoMoreInteractions(reportService);
        ApiError apiError = objectMapper.readValue(response.getBody(), ApiError.class);
        assertThat(apiError.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(apiError.getTimestamp()).isBefore(Instant.now());
        assertThat(apiError.getMessage()).isEqualTo("JSON parse error");
        assertThat(apiError.getValidationErrors()).isEmpty();
    }

    @Test
    @DisplayName("when visit list is null, reject everything")
    void nullVisitList() throws JsonProcessingException {
        HttpEntity<ReportRequest> request = new HttpEntity<>(new ReportRequest(null, 0L), newJsonHeader());
        ResponseEntity<String> response = restTemplate.postForEntity(pathPrefix + UriConstants.REPORT, request, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verifyNoMoreInteractions(reportService);
        ApiError apiError = objectMapper.readValue(response.getBody(), ApiError.class);
        assertThat(apiError.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(apiError.getTimestamp()).isBefore(Instant.now());
        assertThat(apiError.getMessage()).isEqualTo("Invalid request");
        assertThat(apiError.getValidationErrors().size()).isEqualTo(2);
    }

    @Test
    @DisplayName("when visit list is empty, reject everything")
    void emptyVisitList() throws JsonProcessingException {
        HttpEntity<ReportRequest> request = new HttpEntity<>(new ReportRequest(List.of(), 0L), newJsonHeader());
        ResponseEntity<String> response = restTemplate.postForEntity(pathPrefix + UriConstants.REPORT, request, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verifyNoMoreInteractions(reportService);
        ApiError apiError = objectMapper.readValue(response.getBody(), ApiError.class);
        assertThat(apiError.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(apiError.getTimestamp()).isBefore(Instant.now());
        assertThat(apiError.getMessage()).isEqualTo("Invalid request");
        assertThat(apiError.getValidationErrors().size()).isEqualTo(1);
    }

    @Test
    @DisplayName("when a qrCode is null reject just the visit")
    void nullQrCode() {
        HttpEntity<ReportRequest> request = new HttpEntity<>(
                new ReportRequest(List.of(new Visit("qr1", 1L), new Visit(null, 2L)), 3L),
                newJsonHeader()
        );
        ResponseEntity<String> response = restTemplate.postForEntity(pathPrefix + UriConstants.REPORT, request, String.class);
        Mockito.verify(reportService).report(reportRequestArgumentCaptor.capture());
        assertThat(reportRequestArgumentCaptor.getValue().getPivotDateAsNtpTimestamp()).isEqualTo(3L);
        assertThat(reportRequestArgumentCaptor.getValue().getVisits().size()).isEqualTo(1);
        assertThat(reportRequestArgumentCaptor.getValue().getVisits().stream().filter(it -> it.getQrCode() == null).findAny()).isEmpty();
        assertThat(reportRequestArgumentCaptor.getValue().getVisits().get(0).getQrCode()).isEqualTo("qr1");
        assertThat(reportRequestArgumentCaptor.getValue().getVisits().get(0).getQrCodeScanTimeAsNtpTimestamp()).isEqualTo(1L);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("when a qrCode is empty reject just the visit")
    void emptyQrCode() {
        HttpEntity<ReportRequest> request = new HttpEntity<>(
                new ReportRequest(List.of(new Visit("qr1", 1L), new Visit("", 2L)), 3L),
                newJsonHeader()
        );
        ResponseEntity<String> response = restTemplate.postForEntity(pathPrefix + UriConstants.REPORT, request, String.class);
        Mockito.verify(reportService).report(reportRequestArgumentCaptor.capture());
        assertThat(reportRequestArgumentCaptor.getValue().getPivotDateAsNtpTimestamp()).isEqualTo(3L);
        assertThat(reportRequestArgumentCaptor.getValue().getVisits().size()).isEqualTo(1);
        assertThat(reportRequestArgumentCaptor.getValue().getVisits().stream().filter(it -> it.getQrCode().isEmpty()).findAny()).isEmpty();
        assertThat(reportRequestArgumentCaptor.getValue().getVisits().get(0).getQrCode()).isEqualTo("qr1");
        assertThat(reportRequestArgumentCaptor.getValue().getVisits().get(0).getQrCodeScanTimeAsNtpTimestamp()).isEqualTo(1L);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("when a qrCode is blank reject just the visit")
    void blankQrCode() {
        HttpEntity<ReportRequest> request = new HttpEntity<>(
                new ReportRequest(List.of(new Visit("qr1", 1L), new Visit("     ", 2L)), 3L),
                newJsonHeader()
        );
        ResponseEntity<String> response = restTemplate.postForEntity(pathPrefix + UriConstants.REPORT, request, String.class);
        Mockito.verify(reportService).report(reportRequestArgumentCaptor.capture());
        assertThat(reportRequestArgumentCaptor.getValue().getPivotDateAsNtpTimestamp()).isEqualTo(3L);
        assertThat(reportRequestArgumentCaptor.getValue().getVisits().size()).isEqualTo(1);
        assertThat(reportRequestArgumentCaptor.getValue().getVisits().stream().filter(it -> it.getQrCode().isBlank()).findAny()).isEmpty();
        assertThat(reportRequestArgumentCaptor.getValue().getVisits().get(0).getQrCode()).isEqualTo("qr1");
        assertThat(reportRequestArgumentCaptor.getValue().getVisits().get(0).getQrCodeScanTimeAsNtpTimestamp()).isEqualTo(1L);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("when a qrScan is null reject just the visit")
    void nullQrScan() {
        HttpEntity<ReportRequest> request = new HttpEntity<>(
                new ReportRequest(List.of(new Visit("qr1", 1L), new Visit("qr2", null)), 3L),
                newJsonHeader()
        );
        ResponseEntity<String> response = restTemplate.postForEntity(pathPrefix + UriConstants.REPORT, request, String.class);
        Mockito.verify(reportService).report(reportRequestArgumentCaptor.capture());
        assertThat(reportRequestArgumentCaptor.getValue().getPivotDateAsNtpTimestamp()).isEqualTo(3L);
        assertThat(reportRequestArgumentCaptor.getValue().getVisits().size()).isEqualTo(1);
        assertThat(reportRequestArgumentCaptor.getValue().getVisits().stream().filter(it -> it.getQrCodeScanTimeAsNtpTimestamp() == null).findAny()).isEmpty();
        assertThat(reportRequestArgumentCaptor.getValue().getVisits().get(0).getQrCode()).isEqualTo("qr1");
        assertThat(reportRequestArgumentCaptor.getValue().getVisits().get(0).getQrCodeScanTimeAsNtpTimestamp()).isEqualTo(1L);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("when a qrScan is not numeric reject everything")
    void notNumericQrScan() throws JsonProcessingException {
        ReportRequest reportRequest = new ReportRequest(List.of(new Visit("qr1", 1L), new Visit("qr2", 2L)), 3L);
        String json = objectMapper.writeValueAsString(reportRequest);
        String badJson = json.replace("2", "a");
        HttpEntity<String> request = new HttpEntity<>(badJson, newJsonHeader());
        ResponseEntity<String> response = restTemplate.postForEntity(pathPrefix + UriConstants.REPORT, request, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verifyNoMoreInteractions(reportService);
        ApiError apiError = objectMapper.readValue(response.getBody(), ApiError.class);
        assertThat(apiError.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(apiError.getTimestamp()).isBefore(Instant.now());
        assertThat(apiError.getMessage()).isEqualTo("JSON parse error");
        assertThat(apiError.getValidationErrors()).isEmpty();
    }

    @Test
    @DisplayName("when no valid visit then reject everything")
    void noValidVisits() throws JsonProcessingException {
        ReportRequest reportRequest = new ReportRequest(List.of(new Visit(" ", 1L)), 2L);
        String json = objectMapper.writeValueAsString(reportRequest);
        HttpEntity<String> request = new HttpEntity<>(json, newJsonHeader());
        ResponseEntity<String> response = restTemplate.postForEntity(pathPrefix + UriConstants.REPORT, request, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verifyNoMoreInteractions(reportService);
        ApiError apiError = objectMapper.readValue(response.getBody(), ApiError.class);
        assertThat(apiError.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(apiError.getTimestamp()).isBefore(Instant.now());
        assertThat(apiError.getMessage()).isEqualTo("Invalid request");
        assertThat(apiError.getValidationErrors().size()).isEqualTo(1);
        assertThat(apiError.getValidationErrors().stream().findFirst()).isPresent();
        assertThat(apiError.getValidationErrors().stream().findFirst().get().getObject()).isEqualTo("Visit");
        assertThat(apiError.getValidationErrors().stream().findFirst().get().getField()).isEqualTo("qrCode");
        assertThat(apiError.getValidationErrors().stream().findFirst().get().getRejectedValue()).asString().isBlank();
        // TODO find a way to test this localized message: vide / empty
        // assertThat(apiError.getValidationErrors().stream().findFirst().get().getMessage()).isEqualTo("ne doit pas être vide");
    }
}
