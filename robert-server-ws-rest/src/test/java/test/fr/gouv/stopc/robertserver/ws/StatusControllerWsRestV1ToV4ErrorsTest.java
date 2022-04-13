package test.fr.gouv.stopc.robertserver.ws;

import fr.gouv.stopc.robertserver.ws.controller.impl.StatusControllerImpl;
import fr.gouv.stopc.robertserver.ws.service.IRestApiService;
import fr.gouv.stopc.robertserver.ws.utils.UriConstants;
import fr.gouv.stopc.robertserver.ws.vo.StatusVo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@LegacyIntegrationTest
public class StatusControllerWsRestV1ToV4ErrorsTest {

    @Value("${controller.path.prefix}" + UriConstants.API_V4)
    private String pathPrefixV4;

    @Autowired
    private TestRestTemplate restTemplate;

    HttpEntity<StatusVo> requestEntity;

    private URI targetUrl;

    private StatusVo statusBody;

    private HttpHeaders headers;

    @MockBean
    private IRestApiService restApiService;

    @SpyBean
    private StatusControllerImpl statusController;

    @BeforeEach
    public void setUp() {
        this.targetUrl = UriComponentsBuilder.fromUriString(this.pathPrefixV4).path(UriConstants.STATUS).build()
                .encode().toUri();
        this.statusBody = StatusVo.builder()
                .ebid("012345678912")
                .epochId(1)
                .time("12345678")
                .mac("01234567890123456789012345678901234567891234")
                .build();
        this.requestEntity = new HttpEntity<>(this.statusBody, this.headers);
    }

    @Test
    public void testWhenGetStatusReturnsBadRequestThenGetStatusV1ToV4ReturnsBadRequest() {
        when(statusController.getStatus(any())).thenReturn(ResponseEntity.badRequest().build());

        ResponseEntity<String> response = this.restTemplate.exchange(
                this.targetUrl, HttpMethod.POST,
                this.requestEntity, String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    public void testWhenGetStatusReturnsInternalServerErrorThenGetStatusV1ToV4ReturnsInternalServerError() {
        when(statusController.getStatus(any()))
                .thenReturn(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());

        ResponseEntity<String> response = this.restTemplate.exchange(
                this.targetUrl, HttpMethod.POST,
                this.requestEntity, String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    public void testWhenGetStatusReturnsNotFoundThenGetStatusV1ToV4ReturnsNotFound() {
        when(statusController.getStatus(any())).thenReturn(ResponseEntity.status(HttpStatus.NOT_FOUND).build());

        ResponseEntity<String> response = this.restTemplate.exchange(
                this.targetUrl, HttpMethod.POST,
                this.requestEntity, String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    public void testWhenGetStatusReturnsNullThenGetStatusV1ToV4ReturnsInternalServerError() {
        when(statusController.getStatus(any())).thenReturn(null);

        ResponseEntity<String> response = this.restTemplate.exchange(
                this.targetUrl, HttpMethod.POST,
                this.requestEntity, String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void whenGetStatusReturns430ThenGetStatusV5Returns430WithNoBody() {
        when(statusController.getStatus(any())).thenReturn(ResponseEntity.status(430).build());

        ResponseEntity<String> response = this.restTemplate.exchange(
                this.targetUrl, HttpMethod.POST,
                this.requestEntity, String.class
        );

        assertThat(response.getStatusCodeValue()).isEqualTo(430);
        assertThat(response.getBody()).isNull();
    }
}
