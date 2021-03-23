package fr.gouv.clea.ws.controller;

import fr.gouv.clea.ws.utils.UriConstants;
import fr.gouv.clea.ws.vo.ReportRequest;
import fr.gouv.clea.ws.vo.Visit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {"clea.conf.security.report.checkAuthorization=true"})
public class CleaControllerAuthEnabledTest {

    @Value("${controller.path.prefix}" + UriConstants.API_V1)
    private String pathPrefix;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    public void testWhenReportRequestWithMissingAuthenticationThenGetBadRequest() {
        List<Visit> visits = List.of();
        HttpEntity<ReportRequest> request = new HttpEntity<>(new ReportRequest(visits, 0L), CleaControllerTest.newJsonHeader());
        ResponseEntity<String> response = restTemplate.postForEntity(pathPrefix + UriConstants.REPORT, request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    public void testWhenReportRequestWithNonValidAuthenticationThenGetUnauthorized() {
        List<Visit> visits = List.of();
        HttpHeaders headers = CleaControllerTest.newJsonHeader();
        headers.setBearerAuth("invalid JWT token");
        HttpEntity<ReportRequest> reportEntity = new HttpEntity<>(new ReportRequest(visits, 0L), headers);

        ResponseEntity<String> response = restTemplate.postForEntity(pathPrefix + UriConstants.REPORT, reportEntity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
