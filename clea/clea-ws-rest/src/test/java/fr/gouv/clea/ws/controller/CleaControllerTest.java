package fr.gouv.clea.ws.controller;

import fr.gouv.clea.ws.service.impl.ReportService;
import fr.gouv.clea.ws.utils.UriConstants;
import fr.gouv.clea.ws.vo.ReportRequest;
import fr.gouv.clea.ws.vo.Visit;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Disabled;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class CleaControllerTest {
    @Value("${controller.path.prefix}" + UriConstants.API_V1)
    private String pathPrefix;

    @Autowired
    private TestRestTemplate restTemplate;

    @MockBean
    private ReportService reportService;

    @Test
    public void testInfectedUserCanReportHimselfAsInfected() {
        List<Visit> visits = List.of(new Visit("qrCode", 0L));
        HttpEntity<ReportRequest> request = new HttpEntity<>(new ReportRequest(visits, 0L), this.newJsonHeader());
        ResponseEntity<String> response = restTemplate.postForEntity(pathPrefix + UriConstants.REPORT, request,
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    public void testWhenReportRequestWithInvalidMediaTypeThenGetUnsupportedMediaType() {
        ResponseEntity<String> response = restTemplate.postForEntity(pathPrefix + UriConstants.REPORT, "foo",
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        verifyNoMoreInteractions(reportService);
    }

    @Test
    public void testWhenReportRequestWithNullVisitListThenGetBadRequest() {
        HttpEntity<ReportRequest> request = new HttpEntity<>(new ReportRequest(null, 0L), this.newJsonHeader());
        ResponseEntity<String> response = restTemplate.postForEntity(pathPrefix + UriConstants.REPORT, request,
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verifyNoMoreInteractions(reportService);
    }

    @Test
    public void testWhenReportRequestWithInvalidJsonDataThenGetBadRequest() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("id", 1);
        ResponseEntity<String> response = restTemplate.postForEntity(
                pathPrefix + UriConstants.REPORT,
                new HttpEntity<String>(jsonObject.toString(), this.newJsonHeader()),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verifyNoMoreInteractions(reportService);
    }

    @Disabled(value = "Enable this test when JWT ready to be tested")
    @Test
    public void testWhenReportRequestWithMissingAuthenticationThenGetBadRequest() {
        List<Visit> visits = List.of();
        HttpEntity<ReportRequest> request = new HttpEntity<>(new ReportRequest(visits, 0L), this.newJsonHeader());
        ResponseEntity<String> response = restTemplate.postForEntity(pathPrefix + UriConstants.REPORT, request,
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Disabled(value = "Enable this test when JWT ready to be tested")
    @Test
    public void testWhenReportRequestWithNonValidAuthenticationThenGetUnauthorized() {
        List<Visit> visits = List.of();
        HttpHeaders headers = this.newJsonHeader();
        headers.setBearerAuth("invalid JWT token");
        HttpEntity<ReportRequest> reportEntity = new HttpEntity<>(new ReportRequest(visits, 0L), headers);

        ResponseEntity<String> response = restTemplate.postForEntity(pathPrefix + UriConstants.REPORT, reportEntity,
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    public HttpHeaders newJsonHeader() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
