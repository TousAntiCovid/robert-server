package test.fr.gouv.stopc.robertserver.ws;

import com.google.protobuf.ByteString;
import fr.gouv.stopc.robert.crypto.grpc.server.client.service.ICryptoServerGrpcClient;
import fr.gouv.stopc.robert.crypto.grpc.server.messaging.CreateRegistrationResponse;
import fr.gouv.stopc.robert.server.common.service.IServerConfigurationService;
import fr.gouv.stopc.robert.server.common.utils.TimeUtils;
import fr.gouv.stopc.robertserver.database.model.Registration;
import fr.gouv.stopc.robertserver.database.service.impl.RegistrationService;
import fr.gouv.stopc.robertserver.ws.dto.RegisterResponseDto;
import fr.gouv.stopc.robertserver.ws.utils.UriConstants;
import fr.gouv.stopc.robertserver.ws.vo.PushInfoVo;
import fr.gouv.stopc.robertserver.ws.vo.RegisterVo;
import io.micrometer.core.instrument.util.StringUtils;
import org.bson.internal.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Optional;

import static fr.gouv.stopc.robertserver.ws.test.MockServerManager.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@LegacyIntegrationTest
public class RegisterControllerWsRestTest {

    @Value("${controller.path.prefix}" + UriConstants.API_V2)
    private String pathPrefixV2;

    @Value("${controller.path.prefix}" + UriConstants.API_V3)
    private String pathPrefixV3;

    @Value("${controller.path.prefix}" + UriConstants.API_V4)
    private String pathPrefixV4;

    @Value("${controller.path.prefix}" + UriConstants.API_V5)
    private String pathPrefixV5;

    @Value("${controller.path.prefix}" + UriConstants.API_V6)
    private String pathPrefix;

    @Autowired
    private TestRestTemplate restTemplate;

    HttpEntity<RegisterVo> requestEntity;

    private URI targetUrl;

    private RegisterVo body;

    private HttpHeaders headers;

    @MockBean
    private RegistrationService registrationService;

    @MockBean
    private ICryptoServerGrpcClient cryptoServerClient;

    @Autowired
    private IServerConfigurationService serverConfigurationService;

    private int currentEpoch;

    @BeforeEach
    public void before() {

        assert (this.restTemplate != null);
        this.headers = new HttpHeaders();
        this.headers.setContentType(MediaType.APPLICATION_JSON);
        this.targetUrl = UriComponentsBuilder.fromUriString(this.pathPrefix).path(UriConstants.REGISTER).build()
                .encode().toUri();

        this.currentEpoch = this.getCurrentEpoch();

        // TODO: review this or find a better way to validate epochid
        // Sanity check: this test will fail twenty years after the start of the service
        // (used to prevent epoch calculation errors)
        // Robert protocol uses 24 bits to store epochId. It means we can have 2^24 =
        // 16777216 epochs.
        // We use 4 epochs per hour and it can last for hundred of years.
        assertTrue(this.currentEpoch <= 4 * 24 * 365 * 20);
    }

    @Test
    public void testBadHttpVerb() {
        this.body = RegisterVo.builder().captcha("TEST").captchaId("92c9623a7c474c4a92661614cd29d08b-success").build();

        this.requestEntity = new HttpEntity<>(this.body, this.headers);

        ResponseEntity<String> response = this.restTemplate.exchange(
                this.targetUrl.toString(), HttpMethod.GET,
                this.requestEntity, String.class
        );

        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, response.getStatusCode());
        verify(this.registrationService, times(0)).saveRegistration(ArgumentMatchers.any());
    }

    @Test
    public void testNullCaptcha() {
        this.body = RegisterVo.builder().captcha(null).build();

        this.requestEntity = new HttpEntity<>(this.body, this.headers);

        ResponseEntity<String> response = this.restTemplate.exchange(
                this.targetUrl.toString(), HttpMethod.POST,
                this.requestEntity, String.class
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(this.registrationService, times(0)).saveRegistration(ArgumentMatchers.any());
    }

    @Test
    public void testNoCaptcha() {
        this.body = RegisterVo.builder().build();

        this.requestEntity = new HttpEntity<>(this.body, this.headers);

        ResponseEntity<String> response = this.restTemplate.exchange(
                this.targetUrl.toString(), HttpMethod.POST,
                this.requestEntity, String.class
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(this.registrationService, times(0)).saveRegistration(ArgumentMatchers.any());
    }

    @Test
    public void testBadRequests() {
        String captchaId = "92c9623a7c474c4a92661614cd29d08b-success";

        assertEquals(
                HttpStatus.BAD_REQUEST,
                this.restTemplate.exchange(
                        this.targetUrl.toString(), HttpMethod.POST, new HttpEntity<>(
                                RegisterVo.builder().captcha("").captchaId(captchaId)
                                        .clientPublicECDHKey(Base64.encode("an12kmdpsd".getBytes())).build(),
                                this.headers
                        ), String.class
                ).getStatusCode()
        );
        assertEquals(
                HttpStatus.BAD_REQUEST,
                this.restTemplate.exchange(
                        this.targetUrl.toString(), HttpMethod.POST, new HttpEntity<>(
                                RegisterVo.builder().captchaId(captchaId)
                                        .clientPublicECDHKey(Base64.encode("an12kmdpsd".getBytes())).build(),
                                this.headers
                        ), String.class
                ).getStatusCode()
        );
        assertEquals(
                HttpStatus.BAD_REQUEST,
                this.restTemplate.exchange(
                        this.targetUrl.toString(), HttpMethod.POST, new HttpEntity<>(
                                RegisterVo.builder().captcha("mycaptcha").captchaId("")
                                        .clientPublicECDHKey(Base64.encode("an12kmdpsd".getBytes())).build(),
                                this.headers
                        ), String.class
                ).getStatusCode()
        );
        assertEquals(
                HttpStatus.BAD_REQUEST,
                this.restTemplate.exchange(
                        this.targetUrl.toString(), HttpMethod.POST, new HttpEntity<>(
                                RegisterVo.builder().captcha("mycaptcha")
                                        .clientPublicECDHKey(Base64.encode("an12kmdpsd".getBytes())).build(),
                                this.headers
                        ), String.class
                ).getStatusCode()
        );
        assertEquals(
                HttpStatus.BAD_REQUEST,
                this.restTemplate.exchange(
                        this.targetUrl.toString(), HttpMethod.POST, new HttpEntity<>(
                                RegisterVo.builder().captcha("mycaptcha").captchaId(captchaId).clientPublicECDHKey("")
                                        .build(),
                                this.headers
                        ), String.class
                ).getStatusCode()
        );
        assertEquals(
                HttpStatus.BAD_REQUEST,
                this.restTemplate.exchange(
                        this.targetUrl.toString(), HttpMethod.POST, new HttpEntity<>(
                                RegisterVo.builder().captcha("mycaptcha").captchaId(captchaId).build(), this.headers
                        ), String.class
                ).getStatusCode()
        );
    }

    @Test
    public void testCaptchaFailure() {

        this.body = RegisterVo.builder()
                .captcha("TEST").captchaId("92c9623a7c474c4a92661614cd29d08b-wrong")
                .clientPublicECDHKey(Base64.encode("an12kmdpsd".getBytes()))
                .build();

        this.requestEntity = new HttpEntity<>(this.body, this.headers);

        ResponseEntity<String> response = this.restTemplate.exchange(
                this.targetUrl.toString(), HttpMethod.POST,
                this.requestEntity, String.class
        );

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verify(this.registrationService, times(0)).saveRegistration(ArgumentMatchers.any());
        verifyNoInteractionsWithPushNotifServer();
    }

    @Test
    public void testRegisterFailsWhenRegistrationFails() {

        // Given
        this.body = RegisterVo.builder()
                .captcha("TEST").captchaId("92c9623a7c474c4a92661614cd29d08b-success")
                .clientPublicECDHKey(Base64.encode("an12kmdpsd".getBytes()))
                .build();

        this.requestEntity = new HttpEntity<>(this.body, this.headers);

        // Error creating registration
        when(this.cryptoServerClient.createRegistration(any())).thenReturn(Optional.empty());

        // When
        ResponseEntity<String> response = this.restTemplate.exchange(
                this.targetUrl.toString(), HttpMethod.POST,
                this.requestEntity, String.class
        );

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        verify(this.cryptoServerClient).createRegistration(any());
        verify(this.registrationService, never()).saveRegistration(any());
        verifyNoInteractionsWithPushNotifServer();
    }

    @Test
    public void testRegisterFailsWhenCreateRegistrationFails() {

        // Given
        this.body = RegisterVo.builder()
                .captcha("TEST").captchaId("92c9623a7c474c4a92661614cd29d08b-success")
                .clientPublicECDHKey(Base64.encode("an12kmdpsd".getBytes()))
                .build();

        this.requestEntity = new HttpEntity<>(this.body, this.headers);

        byte[] id = "12345".getBytes();

        CreateRegistrationResponse createRegistrationResponse = CreateRegistrationResponse
                .newBuilder()
                .setIdA(ByteString.copyFrom(id))
                .setTuples(ByteString.copyFrom("EncryptedJSONStringWithTuples".getBytes()))
                .build();

        when(this.cryptoServerClient.createRegistration(any())).thenReturn(Optional.of(createRegistrationResponse));

        when(this.registrationService.saveRegistration(any())).thenReturn(Optional.empty());
        // When
        ResponseEntity<String> response = this.restTemplate.exchange(
                this.targetUrl.toString(), HttpMethod.POST,
                this.requestEntity, String.class
        );

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(this.cryptoServerClient).createRegistration(any());
        verify(this.registrationService).saveRegistration(any());
        verifyNoInteractionsWithPushNotifServer();
    }

    @Test
    public void testSuccessV2() {
        testRegisterSucceeds(
                UriComponentsBuilder.fromUriString(this.pathPrefixV2).path(UriConstants.REGISTER).build().toUri()
                        .toString()
        );
    }

    @Test
    public void testSuccessV3() {
        testRegisterSucceeds(
                UriComponentsBuilder.fromUriString(this.pathPrefixV3).path(UriConstants.REGISTER).build().toUri()
                        .toString()
        );
    }

    @Test
    public void testSuccessV4() {
        testRegisterSucceeds(
                UriComponentsBuilder.fromUriString(this.pathPrefixV4).path(UriConstants.REGISTER).build().toUri()
                        .toString()
        );
    }

    @Test
    public void testSuccessV5() {
        testRegisterSucceeds(
                UriComponentsBuilder.fromUriString(this.pathPrefixV5).path(UriConstants.REGISTER).build().toUri()
                        .toString()
        );
    }

    @Test
    public void testSuccess() {
        testRegisterSucceeds(this.targetUrl.toString());
    }

    @Test
    public void testShouldCallPushServerWhenPushInfoIsProvidedAndRegistrationCreated() {

        // Given
        PushInfoVo pushInfo = PushInfoVo.builder()
                .token("token")
                .locale("en-US")
                .timezone("Europe/Paris")
                .build();

        this.body = RegisterVo.builder()
                .captcha("TEST")
                .captchaId("92c9623a7c474c4a92661614cd29d08b-success")
                .clientPublicECDHKey(Base64.encode("an12kmdpsd".getBytes()))
                .pushInfo(pushInfo)
                .build();

        this.requestEntity = new HttpEntity<>(this.body, this.headers);

        byte[] id = "12345".getBytes();

        Registration reg = Registration.builder()
                .permanentIdentifier(id)
                .isNotified(false)
                .atRisk(false)
                .build();

        CreateRegistrationResponse createRegistrationResponse = CreateRegistrationResponse
                .newBuilder()
                .setIdA(ByteString.copyFrom(id))
                .setTuples(ByteString.copyFrom("EncryptedJSONStringWithTuples".getBytes()))
                .build();

        when(this.cryptoServerClient.createRegistration(any())).thenReturn(Optional.of(createRegistrationResponse));

        when(this.registrationService.saveRegistration(any())).thenReturn(Optional.of(reg));

        // When
        ResponseEntity<RegisterResponseDto> response = this.restTemplate.exchange(
                this.targetUrl.toString(),
                HttpMethod.POST, this.requestEntity, RegisterResponseDto.class
        );

        // Then
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody().getConfig());
        assertNotNull(response.getBody().getTuples());
        assertTrue(StringUtils.isNotEmpty(response.getBody().getTuples()));
        verify(this.cryptoServerClient).createRegistration(any());
        verify(this.registrationService).saveRegistration(any());
        verifyPushNotifServerReceivedRegisterForToken(pushInfo);
    }

    private void testRegisterSucceeds(String url) {

        // Given
        this.body = RegisterVo.builder()
                .captcha("TEST")
                .captchaId("92c9623a7c474c4a92661614cd29d08b-success")
                .clientPublicECDHKey(Base64.encode("an12kmdpsd".getBytes()))
                .build();

        this.requestEntity = new HttpEntity<>(this.body, this.headers);

        byte[] id = "12345".getBytes();

        Registration reg = Registration.builder()
                .permanentIdentifier(id)
                .isNotified(false)
                .atRisk(false)
                .build();

        CreateRegistrationResponse createRegistrationResponse = CreateRegistrationResponse
                .newBuilder()
                .setIdA(ByteString.copyFrom(id))
                .setTuples(ByteString.copyFrom("EncryptedJSONStringWithTuples".getBytes()))
                .build();

        when(this.cryptoServerClient.createRegistration(any())).thenReturn(Optional.of(createRegistrationResponse));

        when(this.registrationService.saveRegistration(any())).thenReturn(Optional.of(reg));

        // When
        ResponseEntity<RegisterResponseDto> response = this.restTemplate.exchange(
                url,
                HttpMethod.POST, this.requestEntity, RegisterResponseDto.class
        );

        // Then
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody().getConfig());
        assertNotNull(response.getBody().getTuples());
        assertTrue(StringUtils.isNotEmpty(response.getBody().getTuples()));
        verify(this.cryptoServerClient).createRegistration(any());
        verify(this.registrationService).saveRegistration(any());
        verifyNoInteractionsWithPushNotifServer();
    }

    private int getCurrentEpoch() {
        long tpStartInSecondsNTP = this.serverConfigurationService.getServiceTimeStart();
        return TimeUtils.getCurrentEpochFrom(tpStartInSecondsNTP);
    }
}
