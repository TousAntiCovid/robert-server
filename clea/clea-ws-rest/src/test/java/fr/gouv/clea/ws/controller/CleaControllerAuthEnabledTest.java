package fr.gouv.clea.ws.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.gouv.clea.ws.dto.ApiError;
import fr.gouv.clea.ws.service.impl.ReportService;
import fr.gouv.clea.ws.utils.UriConstants;
import fr.gouv.clea.ws.vo.ReportRequest;
import fr.gouv.clea.ws.vo.Visit;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
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
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {"clea.conf.security.report.checkAuthorization=true"})
class CleaControllerAuthEnabledTest {

    /*
     * needed to generate a jwt key, that we can decrypt using public key in test confs
     */
    private static final String privateJwtKet = "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQCSnywoJusbV6UYYbyf8KrQ5V5MD1X8zo40oCuF5IRYQ4mb+9brS11BbuEn6UJDUuCDiAs0vcunwa71Hv6dakKcqfAZaIvQEpacbfVJDY7TX9QxrbJYUlEkJsbj2zOA+3g+G+taw007I74rPUfA7axN9eRGknl2+pkXQS3bConLZR5U7MT/9r9me4HPC4Z8kRcE5Pvvh4US9Bad9fZkRY5q+KaI8qxvjoeqDgumZVHRDuIfwOPcPTkefTc7zTr3a2Y2MaXidafk/9v1gR0kgBYdxMEWJEyBuwpEbYo0CD7TitxmfH/D7Fw2qyQq9xHXVMLNS40+woEKlniisC2vg7/tAgMBAAECggEAZbIhXngf+gcAa7jeq9CxqdJtZTP94CskVzwA9A1b/hxaBebXWrwbIpdVc+lGHIwPTSu/GgXKi8C7KSkmUOiy6xazgewRjXBXJojd6J2Owu1ksFBZswjlXr3GlaQkRQImlG2pAHsVxj80V6lZa2dua2RxwME3nl6ScJ60v4i/qmKbNxcAcHOjK1mvtcOiyViCkJHG+qq+koVxTQ7se9hfXG6wLtVUpCQgKgkhuIUg1IwgkKoyOt9OKMhiJp+E1IwUOg3XazL3PA2pBIrp8mxhB/I75s9un7cBjOuv1ywZO3KfYLYD6hSq6RWeywRIVSH7vn2lAJtivrxBJAKDB6jZwQKBgQDN1YvgwSJYiFnSi+4S+Hoheh7qHHiXKaL4nubAhZlW1DKeh79OpUrbVMwHZdSQzf46TrFBnhQ0t4kFWgLxgqldY8lO/Hndly2i5c04EEEeCzcWaXcWv24omUMpbYD7JpRsj6JjS08zS2X8nkJjj2OgcUb4HcVCKof8Jj3i5GRzpwKBgQC2Wzt0MqRacDdWsCDTxP6nGJS8yA4oDBONFjdYaTuypqTM0SQ1XiyUvBwx1o79PZAhknb4/UXkMApj5e0BKmmtmNDypWgthQfPK7RamKu2FpcEMSnM4HSFvDYzWCrmddbWdGwm7FsU7W4FCMTKnMRBQ6K0UQPlEc1tnds6yW7ySwKBgQCZjbSjQBGaUGYJ7z/1QQ8DiHIlnoXL51Df/tMQTtp87yKwJ37tcdwtUc4/upTgTfxZjTkpRX+3cDA1INhPSXWF6RpV5X4YdF6kRqFZMK8TdbRr8NPZ0Yehm+yBrGJrenWBo4m2X4k/MAFuerX2RhNBryANm0/8M3RtBC5o5I+XwwKBgBCuWcrwUv5+42EHrYkRrSXF5t06A6mAKU2vqZJp1e8qtUqTGxfSrItShdW9Rck+l2+qwT1Xlcwg5OJshvijU5VwtDRuExCO6b72xYHAE30NpfTZNnSqV55gMCkUOKBqSSPG9Jm+5zoL2hOV0MKkAoPh1wFdo9iRf1Q2q3Y+NOrvAoGAU2RldX35DEub3EtWl70wEvNx1DuGPCBPL5OyMsJzJh3x5cpzHZc5B9TLjIT3YeRjKiaZxtJ/evulCw/ugScEFph4GtvJZgZYutVRO8LdsqYlb+1ikE/Hd9sqhp7PhDJhfQSV3uLGrfX5fd+bizNAr8swwAFxu35N8vdKOKHtnp8=";
    @Value("${controller.path.prefix}" + UriConstants.API_V1)
    private String pathPrefix;
    @Autowired
    private TestRestTemplate restTemplate;
    @Autowired
    private ObjectMapper objectMapper;
    @MockBean
    private ReportService reportService;
    @Captor
    private ArgumentCaptor<ReportRequest> reportRequestArgumentCaptor;

    @Test
    void testWhenReportRequestWithMissingAuthenticationThenUNAUTHORIZED() throws JsonProcessingException {
        List<Visit> visits = List.of(
                new Visit(RandomStringUtils.randomAlphanumeric(20), RandomUtils.nextLong()),
                new Visit(RandomStringUtils.randomAlphanumeric(20), RandomUtils.nextLong()),
                new Visit(RandomStringUtils.randomAlphanumeric(20), RandomUtils.nextLong())
        );
        HttpEntity<ReportRequest> request = new HttpEntity<>(new ReportRequest(visits, 0L), CleaControllerTest.newJsonHeader());
        ResponseEntity<String> response = restTemplate.postForEntity(pathPrefix + UriConstants.REPORT, request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        ApiError apiError = objectMapper.readValue(response.getBody(), ApiError.class);
        assertThat(apiError.getHttpStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(apiError.getTimestamp()).isBefore(Instant.now());
        assertThat(apiError.getMessage()).isEqualTo("Could not be authorized (Missing authorisation header/token)");
        assertThat(apiError.getValidationErrors()).isEmpty();
    }

    @Test
    void testWhenReportRequestWithNonValidAuthenticationThenGetFORBIDDEN() throws JsonProcessingException {
        List<Visit> visits = List.of(
                new Visit(RandomStringUtils.randomAlphanumeric(20), RandomUtils.nextLong()),
                new Visit(RandomStringUtils.randomAlphanumeric(20), RandomUtils.nextLong()),
                new Visit(RandomStringUtils.randomAlphanumeric(20), RandomUtils.nextLong())
        );
        HttpHeaders headers = CleaControllerTest.newJsonHeader();
        headers.setBearerAuth("invalid JWT token");
        HttpEntity<ReportRequest> reportEntity = new HttpEntity<>(new ReportRequest(visits, 0L), headers);

        ResponseEntity<String> response = restTemplate.postForEntity(pathPrefix + UriConstants.REPORT, reportEntity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        ApiError apiError = objectMapper.readValue(response.getBody(), ApiError.class);
        assertThat(apiError.getHttpStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        assertThat(apiError.getTimestamp()).isBefore(Instant.now());
        assertThat(apiError.getMessage()).isEqualTo("Could not be authenticated (Authorisation header/token invalid)");
        assertThat(apiError.getValidationErrors()).isEmpty();
    }

    @Test
    @DisplayName("unsecured apis should return 200 when called without auth token")
    void testUnsecuredApiWithoutToken() {
        ResponseEntity<String> response = restTemplate.getForEntity("/actuator", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        response = restTemplate.getForEntity("/swagger-ui/", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("secured apis should return 200 when called with a valid auth token")
    void validRequestWithAuthEnabledAndValidToken() throws NoSuchAlgorithmException, InvalidKeySpecException {
        Instant now = Instant.now();
        Instant yesterday = now.minus(1, ChronoUnit.DAYS);
        Instant tomorrow = now.plus(1, ChronoUnit.DAYS);
        HttpHeaders headers = CleaControllerTest.newJsonHeader();
        headers.setBearerAuth(this.newJwtToken(yesterday, tomorrow));

        HttpEntity<ReportRequest> request = new HttpEntity<>(
                new ReportRequest(List.of(new Visit("qr1", 1L)), 2L),
                headers
        );
        ResponseEntity<String> response = restTemplate.postForEntity(pathPrefix + UriConstants.REPORT, request, String.class);
        Mockito.verify(reportService).report(reportRequestArgumentCaptor.capture());
        assertThat(reportRequestArgumentCaptor.getValue().getPivotDateAsNtpTimestamp()).isEqualTo(2L);
        assertThat(reportRequestArgumentCaptor.getValue().getVisits().size()).isEqualTo(1);
        assertThat(reportRequestArgumentCaptor.getValue().getVisits().get(0).getQrCode()).isEqualTo("qr1");
        assertThat(reportRequestArgumentCaptor.getValue().getVisits().get(0).getQrCodeScanTimeAsNtpTimestamp()).isEqualTo(1L);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private String newJwtToken(Instant now, Instant expiration) throws NoSuchAlgorithmException, InvalidKeySpecException {
        SignatureAlgorithm rs256 = SignatureAlgorithm.RS256;
        byte[] decoded = Decoders.BASE64.decode(privateJwtKet);
        KeyFactory keyFactory = KeyFactory.getInstance(rs256.getFamilyName());
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decoded);
        return Jwts.builder()
                .setHeaderParam("type", "JWT")
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiration))
                .signWith(keyFactory.generatePrivate(keySpec), rs256)
                .compact();
    }
}
