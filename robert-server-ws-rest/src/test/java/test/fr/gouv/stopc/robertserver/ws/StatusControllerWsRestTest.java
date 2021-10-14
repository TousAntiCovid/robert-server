package test.fr.gouv.stopc.robertserver.ws;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.net.URI;
import java.security.Key;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.crypto.KeyGenerator;
import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.util.UriComponentsBuilder;

import com.google.protobuf.ByteString;
import fr.gouv.stopc.robert.crypto.grpc.server.client.service.ICryptoServerGrpcClient;
import fr.gouv.stopc.robert.crypto.grpc.server.messaging.GetIdFromStatusResponse;
import fr.gouv.stopc.robert.server.common.DigestSaltEnum;
import fr.gouv.stopc.robert.server.common.service.IServerConfigurationService;
import fr.gouv.stopc.robert.server.common.utils.ByteUtils;
import fr.gouv.stopc.robert.server.common.utils.TimeUtils;
import fr.gouv.stopc.robert.server.crypto.service.CryptoService;
import fr.gouv.stopc.robert.server.crypto.structure.impl.CryptoHMACSHA256;
import fr.gouv.stopc.robert.server.crypto.structure.impl.CryptoSkinny64;
import fr.gouv.stopc.robertserver.database.model.EpochExposition;
import fr.gouv.stopc.robertserver.database.model.Registration;
import fr.gouv.stopc.robertserver.database.service.impl.RegistrationService;
import fr.gouv.stopc.robertserver.ws.RobertServerWsRestApplication;
import fr.gouv.stopc.robertserver.ws.config.RobertServerWsConfiguration;
import fr.gouv.stopc.robertserver.ws.config.WsServerConfiguration;
import fr.gouv.stopc.robertserver.ws.dto.RiskLevel;
import fr.gouv.stopc.robertserver.ws.dto.StatusResponseDto;
import fr.gouv.stopc.robertserver.ws.dto.StatusResponseDtoV1ToV4;
import fr.gouv.stopc.robertserver.ws.dto.StatusResponseDtoV5;
import fr.gouv.stopc.robertserver.ws.service.IRestApiService;
import fr.gouv.stopc.robertserver.ws.utils.PropertyLoader;
import fr.gouv.stopc.robertserver.ws.utils.UriConstants;
import fr.gouv.stopc.robertserver.ws.vo.PushInfoVo;
import fr.gouv.stopc.robertserver.ws.vo.StatusVo;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Encoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.bson.internal.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

@SpringBootTest(classes = {
        RobertServerWsRestApplication.class }, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Slf4j
public class StatusControllerWsRestTest {

    @Value("${controller.path.prefix}" + UriConstants.API_V1)
    private String pathPrefixV1;

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

    @Value("${robert.server.status-request-minimum-epoch-gap}")
    private Integer statusRequestMinimumEpochGap;

    @Inject
    private TestRestTemplate restTemplate;

    HttpEntity<StatusVo> requestEntity;

    private URI targetUrl;

    private StatusVo statusBody;

    private HttpHeaders headers;

    @MockBean
    private RegistrationService registrationService;

    @Autowired
    private CryptoService cryptoService;

    @MockBean
    private ICryptoServerGrpcClient cryptoServerClient;

    @Autowired
    private IServerConfigurationService serverConfigurationService;

    @MockBean
    private PropertyLoader propertyLoader;

    @MockBean
    private WsServerConfiguration wsServerConfiguration;

    @MockBean
    private RobertServerWsConfiguration config;

    @MockBean
    private IRestApiService restApiService;

    private int currentEpoch;

    private byte[] serverKey;

    @BeforeEach
    public void before() {
        assert (this.restTemplate != null);
        this.headers = new HttpHeaders();
        this.headers.setContentType(MediaType.APPLICATION_JSON);
        this.targetUrl = UriComponentsBuilder.fromUriString(this.pathPrefix).path(UriConstants.STATUS).build().encode().toUri();

        this.currentEpoch = this.getCurrentEpoch();

        when(this.propertyLoader.getEsrLimit()).thenReturn(-1);
        when(this.wsServerConfiguration.getStatusRequestMinimumEpochGap()).thenReturn(this.statusRequestMinimumEpochGap);

        this.serverKey = this.generateKey(24);
    }

    @Test
    public void testBadHttpVerbFails() {
        this.statusBody = StatusVo.builder().ebid(Base64.encode(new byte[4])).build();

        this.requestEntity = new HttpEntity<>(this.statusBody, this.headers);

        ResponseEntity<String> response = this.restTemplate.exchange(this.targetUrl.toString(), HttpMethod.GET,
                this.requestEntity, String.class);

        log.info("******* Bad HTTP Verb Payload: {}", response.getBody());

        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, response.getStatusCode());
        verify(this.registrationService, times(0)).findById(ArgumentMatchers.any());
        verify(this.registrationService, times(0)).saveRegistration(ArgumentMatchers.any());
    }

    @Test
    public void testBadEBIDSizeFails() {
        byte[] idA = this.generateKey(5);
        byte[] kA = this.generateKA();
        Registration reg = Registration.builder()
                .permanentIdentifier(idA)
                .atRisk(true)
                .isNotified(false)
                .lastStatusRequestEpoch(currentEpoch - 3).build();

        doReturn(Optional.of(reg)).when(this.registrationService).findById(ArgumentMatchers.any());

        byte[][] reqContent = createEBIDTimeMACFor(idA, kA, currentEpoch);

        statusBody = StatusVo.builder()
                .ebid(Base64.encode("ABC".getBytes()))
                .epochId(currentEpoch)
                .time(Base64.encode(reqContent[1]))
                .mac(Base64.encode(reqContent[2])).build();

        this.requestEntity = new HttpEntity<>(this.statusBody, this.headers);

        ResponseEntity<StatusResponseDto> response = this.restTemplate.exchange(this.targetUrl.toString(),
                HttpMethod.POST, this.requestEntity, StatusResponseDto.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(this.registrationService, times(0)).findById(ArgumentMatchers.any());
        verify(this.registrationService, times(0)).saveRegistration(reg);
    }

    @Test
    public void testAcceptsOldEBIDValueEpochSucceeds() {

        int oldEpoch = currentEpoch - 10;

        // Given
        byte[] idA = this.generateKey(5);
        byte[] kA = this.generateKA();
        // Mess up with the epoch used to create the EBID
        byte[][] reqContent = createEBIDTimeMACFor(idA, kA, oldEpoch);
        Registration reg = Registration.builder()
                .permanentIdentifier(idA)
                .atRisk(true)
                .isNotified(false)
                .lastStatusRequestEpoch(currentEpoch - 3).build();

        byte[] decryptedEbid = new byte[8];
        System.arraycopy(idA, 0, decryptedEbid, 3, 5);
        System.arraycopy(ByteUtils.intToBytes(oldEpoch), 1, decryptedEbid, 0, 3);

        when(this.wsServerConfiguration.getJwtUseTransientKey()).thenReturn(true);

        doReturn(Optional.of(reg)).when(this.registrationService).findById(idA);

        doReturn(Optional.of(GetIdFromStatusResponse.newBuilder()
                .setEpochId(oldEpoch)
                .setIdA(ByteString.copyFrom(idA))
                .setTuples(ByteString.copyFrom("EncryptedJSONStringWithTuples".getBytes()))
                .build()))
        .when(this.cryptoServerClient).getIdFromStatus(any());

        statusBody = StatusVo.builder()
                .ebid(Base64.encode(reqContent[0]))
                .epochId(currentEpoch)
                .time(Base64.encode(reqContent[1]))
                .mac(Base64.encode(reqContent[2])).build();

        this.requestEntity = new HttpEntity<>(this.statusBody, this.headers);

        ResponseEntity<StatusResponseDto> response = this.restTemplate.exchange(this.targetUrl.toString(),
                HttpMethod.POST, this.requestEntity, StatusResponseDto.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(this.registrationService, times(2)).findById(ArgumentMatchers.any());
        verify(this.registrationService, times(2)).saveRegistration(reg);
    }

    @Test
    public void testBadEBIDValueIdFails() {

        // Given
        byte[] idA = this.generateKey(5);
        byte[] kA = this.generateKA();

        // Use unknown Id when creating the EBID
        byte[] modifiedIdA = new byte[5];
        System.arraycopy(idA, 0, modifiedIdA, 0, 5);
        modifiedIdA[4] = (byte)(modifiedIdA[4] ^ 0x4);

        byte[][] reqContent = createEBIDTimeMACFor(modifiedIdA, kA, currentEpoch);

        doReturn(Optional.of(GetIdFromStatusResponse.newBuilder()
                .setEpochId(currentEpoch)
                .setIdA(ByteString.copyFrom(modifiedIdA))
                .setTuples(ByteString.copyFrom("Base64encodedEncryptedJSONStringWithTuples".getBytes()))
                .build()))
        .when(this.cryptoServerClient).getIdFromStatus(any());
        when(this.registrationService.findById(modifiedIdA)).thenReturn(Optional.empty());

        statusBody = StatusVo.builder()
                .ebid(Base64.encode(reqContent[0]))
                .epochId(currentEpoch)
                .time(Base64.encode(reqContent[1]))
                .mac(Base64.encode(reqContent[2]))
                .build();

        this.requestEntity = new HttpEntity<>(this.statusBody, this.headers);

        // When
        ResponseEntity<StatusResponseDto> response = this.restTemplate.exchange(this.targetUrl.toString(),
                HttpMethod.POST, this.requestEntity, StatusResponseDto.class);

        // Then
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(this.registrationService, times(2)).findById(ArgumentMatchers.any());
        verify(this.registrationService, times(0)).saveRegistration(ArgumentMatchers.any());
    }

    @Test
    public void testBadTimeFutureFails() {

        byte[] idA = this.generateKey(5);
        byte[] kA = this.generateKA();
        Registration reg = Registration.builder()
                .permanentIdentifier(idA)
                .atRisk(true)
                .isNotified(false)
                .lastStatusRequestEpoch(currentEpoch - 3).build();

        doReturn(Optional.of(reg)).when(this.registrationService).findById(ArgumentMatchers.any());

        byte[][] reqContent = createEBIDTimeMACFor(idA, kA, currentEpoch, 0 - (this.propertyLoader.getRequestTimeDeltaTolerance() + 1));

        statusBody = StatusVo.builder()
                .ebid(Base64.encode(reqContent[0]))
                .epochId(currentEpoch)
                .time(Base64.encode(reqContent[1]))
                .mac(Base64.encode(reqContent[2]))
                .build();

        this.requestEntity = new HttpEntity<>(this.statusBody, this.headers);

        ResponseEntity<StatusResponseDto> response = this.restTemplate.exchange(this.targetUrl.toString(),
                HttpMethod.POST, this.requestEntity, StatusResponseDto.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(this.registrationService, times(0)).findById(ArgumentMatchers.any());
        verify(this.registrationService, times(0)).saveRegistration(reg);
    }

    @Test
    public void testBadTimeFutureFailsEvenWithPuhsInfoShouldNotCallPushRegister() {

        byte[] idA = this.generateKey(5);
        byte[] kA = this.generateKA();
        Registration reg = Registration.builder()
                .permanentIdentifier(idA)
                .atRisk(true)
                .isNotified(false)
                .lastStatusRequestEpoch(currentEpoch - 3).build();

        doReturn(Optional.of(reg)).when(this.registrationService).findById(ArgumentMatchers.any());

        byte[][] reqContent = createEBIDTimeMACFor(idA, kA, currentEpoch, 0 - (this.propertyLoader.getRequestTimeDeltaTolerance() + 1));

        statusBody = StatusVo.builder()
                .ebid(Base64.encode(reqContent[0]))
                .epochId(currentEpoch)
                .time(Base64.encode(reqContent[1]))
                .mac(Base64.encode(reqContent[2]))
                .pushInfo(PushInfoVo.builder()
                        .token("token")
                        .locale("en-US")
                        .timezone("Europe/Paris")
                        .build())
                .build();

        this.requestEntity = new HttpEntity<>(this.statusBody, this.headers);

        ResponseEntity<StatusResponseDto> response = this.restTemplate.exchange(this.targetUrl.toString(),
                HttpMethod.POST, this.requestEntity, StatusResponseDto.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(this.registrationService, times(0)).findById(ArgumentMatchers.any());
        verify(this.registrationService, times(0)).saveRegistration(reg);
        verify(this.restApiService, never()).registerPushNotif(any(PushInfoVo.class));
    }

    @Test
    public void testBadTimePastFails() {
        byte[] idA = this.generateKey(5);
        byte[] kA = this.generateKA();
        Registration reg = Registration.builder()
                .permanentIdentifier(idA)
                .atRisk(true)
                .isNotified(false)
                .lastStatusRequestEpoch(currentEpoch - 3).build();

        doReturn(Optional.of(reg)).when(this.registrationService).findById(ArgumentMatchers.any());

        byte[][] reqContent = createEBIDTimeMACFor(idA, kA, currentEpoch, 0 - (this.propertyLoader.getRequestTimeDeltaTolerance() + 1));

        statusBody = StatusVo.builder()
                .ebid(Base64.encode(reqContent[0]))
                .epochId(currentEpoch)
                .time(Base64.encode(reqContent[1]))
                .mac(Base64.encode(reqContent[2]))
                .build();

        this.requestEntity = new HttpEntity<>(this.statusBody, this.headers);

        ResponseEntity<StatusResponseDto> response = this.restTemplate.exchange(this.targetUrl.toString(),
                HttpMethod.POST, this.requestEntity, StatusResponseDto.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(this.registrationService, times(0)).findById(ArgumentMatchers.any());
        verify(this.registrationService, times(0)).saveRegistration(reg);
    }

    @Test
    public void testBadTimePastFailsEvenWithPushInfoShouldNotCallPushRegister() {

        // Given
        byte[] idA = this.generateKey(5);
        byte[] kA = this.generateKA();
        Registration reg = Registration.builder()
                .permanentIdentifier(idA)
                .atRisk(true)
                .isNotified(false)
                .lastStatusRequestEpoch(currentEpoch - 3).build();

        doReturn(Optional.of(reg)).when(this.registrationService).findById(ArgumentMatchers.any());

        byte[][] reqContent = createEBIDTimeMACFor(idA, kA, currentEpoch, 0 - (this.propertyLoader.getRequestTimeDeltaTolerance() + 1));

        PushInfoVo pushInfo = PushInfoVo.builder()
                .token("token")
                .locale("en-US")
                .timezone("Europe/Paris")
                .build();

        statusBody = StatusVo.builder()
                .ebid(Base64.encode(reqContent[0]))
                .epochId(currentEpoch)
                .time(Base64.encode(reqContent[1]))
                .mac(Base64.encode(reqContent[2]))
                .pushInfo(pushInfo)
                .build();

        this.requestEntity = new HttpEntity<>(this.statusBody, this.headers);

        // When
        ResponseEntity<StatusResponseDto> response = this.restTemplate.exchange(this.targetUrl.toString(),
                HttpMethod.POST, this.requestEntity, StatusResponseDto.class);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(this.registrationService, times(0)).findById(ArgumentMatchers.any());
        verify(this.registrationService, times(0)).saveRegistration(reg);
        verify(this.restApiService, never()).registerPushNotif(pushInfo);
    }

    @Test
    public void testBadTimeSizeFails() {
        byte[] idA = this.generateKey(5);
        byte[] kA = this.generateKA();
        Registration reg = Registration.builder()
                .permanentIdentifier(idA)
                .atRisk(true)
                .isNotified(false)
                .lastStatusRequestEpoch(currentEpoch - 3).build();

        doReturn(Optional.of(reg)).when(this.registrationService).findById(ArgumentMatchers.any());

        byte[][] reqContent = createEBIDTimeMACFor(idA, kA, currentEpoch);

        statusBody = StatusVo.builder()
                .ebid(Base64.encode(reqContent[0]))
                .epochId(currentEpoch)
                .time(Base64.encode("AB".getBytes()))
                .mac(Base64.encode(reqContent[2]))
                .build();

        this.requestEntity = new HttpEntity<>(this.statusBody, this.headers);

        ResponseEntity<StatusResponseDto> response = this.restTemplate.exchange(this.targetUrl.toString(),
                HttpMethod.POST, this.requestEntity, StatusResponseDto.class);


        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(this.registrationService, times(0)).findById(ArgumentMatchers.any());
        verify(this.registrationService, times(0)).saveRegistration(reg);
    }

    @Test
    public void testBadMACSizeFails() {
        byte[] idA = this.generateKey(5);
        byte[] kA = this.generateKA();
        Registration reg = Registration.builder()
                .permanentIdentifier(idA)
                .atRisk(true)
                .isNotified(false)
                .lastStatusRequestEpoch(currentEpoch - 3).build();

        doReturn(Optional.of(reg)).when(this.registrationService).findById(ArgumentMatchers.any());

        byte[][] reqContent = createEBIDTimeMACFor(idA, kA, currentEpoch);

        statusBody = StatusVo.builder()
                .ebid(Base64.encode(reqContent[0]))
                .epochId(currentEpoch)
                .time(Base64.encode(reqContent[1]))
                .mac(Base64.encode("ABC".getBytes()))
                .build();

        this.requestEntity = new HttpEntity<>(this.statusBody, this.headers);

        ResponseEntity<StatusResponseDto> response = this.restTemplate.exchange(this.targetUrl.toString(),
                HttpMethod.POST, this.requestEntity, StatusResponseDto.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(this.registrationService, times(0)).findById(ArgumentMatchers.any());
        verify(this.registrationService, times(0)).saveRegistration(reg);
    }

    @Test
    public void testBadMACValueFails() {

        // Given
        byte[] idA = this.generateKey(5);
        byte[] kA = this.generateKA();
        Registration reg = Registration.builder()
                .permanentIdentifier(idA)
                .atRisk(true)
                .isNotified(false)
                .lastStatusRequestEpoch(currentEpoch - 3).build();

        doReturn(Optional.of(reg)).when(this.registrationService).findById(ArgumentMatchers.any());

        byte[][] reqContent = createEBIDTimeMACFor(idA, kA, currentEpoch);

        doReturn(Optional.empty())
        .when(this.cryptoServerClient).getIdFromStatus(any());

        // Mess up with MAC
        reqContent[2][3] = 0x00;

        statusBody = StatusVo.builder()
                .ebid(Base64.encode(reqContent[0]))
                .epochId(currentEpoch)
                .time(Base64.encode(reqContent[1]))
                .mac(Base64.encode(reqContent[2]))
                .build();

        this.requestEntity = new HttpEntity<>(this.statusBody, this.headers);

        // When
        ResponseEntity<StatusResponseDto> response = this.restTemplate.exchange(this.targetUrl.toString(),
                HttpMethod.POST, this.requestEntity, StatusResponseDto.class);

        // Given
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(this.registrationService, times(0)).findById(ArgumentMatchers.any());
        verify(this.registrationService, times(0)).saveRegistration(reg);
    }

    public byte[] generateKA() {
        byte[] ka = null;

        try {
            KeyGenerator keyGen = KeyGenerator.getInstance("HmacSHA256");

            // Creating a SecureRandom object
            SecureRandom secRandom = new SecureRandom();

            // Initializing the KeyGenerator
            keyGen.init(secRandom);

            // Creating/Generating a key
            Key key = keyGen.generateKey();
            ka = key.getEncoded();

        } catch (NoSuchAlgorithmException e) {
            log.info("Problem generating KA");
        }
        return ka;
    }

    public byte[] generateKey(final int nbOfbytes) {
        byte[] rndBytes = new byte[nbOfbytes];
        SecureRandom sr = new SecureRandom();
        sr.nextBytes(rndBytes);
        return rndBytes;
    }

    private int getCurrentEpoch() {
        long tpStartInSecondsNTP = this.serverConfigurationService.getServiceTimeStart();
        return TimeUtils.getCurrentEpochFrom(tpStartInSecondsNTP);
    }

    private byte[] generateTime32(int adjustTimeInSeconds) {
        long tsInSeconds = TimeUtils.convertUnixMillistoNtpSeconds(System.currentTimeMillis());
        tsInSeconds += adjustTimeInSeconds;
        byte[] tsInSecondsB = ByteUtils.longToBytes(tsInSeconds);
        byte[] time = new byte[4];

        System.arraycopy(tsInSecondsB, 4, time, 0, 4);

        return time;
    }

    private byte[] generateHMAC(final CryptoHMACSHA256 cryptoHMACSHA256S, final byte[] argument, final DigestSaltEnum salt)
            throws Exception {

        final byte[] prefix = new byte[] { salt.getValue() };

        // HMAC-SHA256 processing
        byte[] generatedSHA256 = cryptoHMACSHA256S.encrypt(ByteUtils.addAll(prefix, argument));

        return generatedSHA256;
    }

    private byte[] generateMACforESR(byte[] ebid, byte[] time, byte[] ka) {
        // Merge arrays
        // HMAC-256
        // return hash
        byte[] agg = new byte[8 + 4];
        System.arraycopy(ebid, 0, agg, 0, ebid.length);
        System.arraycopy(time, 0, agg, ebid.length, time.length);

        byte[] mac = new byte[32];
        try {
            mac = this.generateHMAC(new CryptoHMACSHA256(ka), agg, DigestSaltEnum.STATUS);
        } catch (Exception e) {
            log.info("Problem generating SHA256");
        }
        return mac;
    }

    private byte[][] createEBIDTimeMACFor(byte[] id, byte[] ka, int currentEpoch) {
        return this.createEBIDTimeMACFor(id, ka, currentEpoch, 0);
    }

    private byte[][] createEBIDTimeMACFor(byte[] id, byte[] ka, int currentEpoch, int adjustTimeBySeconds) {
        byte[][] res = new byte[3][];
        try {
            res[0] = this.cryptoService.generateEBID(new CryptoSkinny64(this.serverKey),
                    currentEpoch, id);
            res[1] = this.generateTime32(adjustTimeBySeconds);
            res[2] = this.generateMACforESR(res[0], res[1], ka);
        } catch (Exception e) {
            log.info("Problem creating EBID, Time and MAC for test");
        }
        return res;
    }

    /** Test the access for API V1, should not be used since API V2 */
    @Test
    public void testAccessV1() {
        statusRequestAtRiskSucceedsV1ToV4(UriComponentsBuilder.fromUriString(this.pathPrefixV1).path(UriConstants.STATUS).build().encode().toUri());
    }

    /** Test the access for API V2, should not be used since API V3 */
    @Test
    public void testAccessV2() {
        statusRequestAtRiskSucceedsV1ToV4(UriComponentsBuilder.fromUriString(this.pathPrefixV2).path(UriConstants.STATUS).build().encode().toUri());
    }
    
    /** Test the access for API V3, should not be used since API V4 */
    @Test
    public void testAccessV3() {
        statusRequestAtRiskSucceedsV1ToV4(UriComponentsBuilder.fromUriString(this.pathPrefixV3).path(UriConstants.STATUS).build().encode().toUri());
    }

    /** Test the access for API V4, should not be used since API V5 */
    @Test
    public void testAccessV4() {
        statusRequestAtRiskSucceedsV1ToV4(UriComponentsBuilder.fromUriString(this.pathPrefixV4).path(UriConstants.STATUS).build().encode().toUri());
    }

    /** Test the access for API V5, should not be used since API V6 */
    @Test
    public void testAccessV5() {
        statusRequestAtRiskSucceedsV5(UriComponentsBuilder.fromUriString(this.pathPrefixV5).path(UriConstants.STATUS).build().encode().toUri());
    }

    /** {@link #statusRequestAtRiskSucceeds(URI)} and shortcut to test for API V6 exposure */
    @Test
    public void testStatusRequestAtRiskSucceedsV6() {
        statusRequestAtRiskSucceeds(this.targetUrl);
    }

    protected Registration statusRequestAtRiskSucceedsSetUp(URI targetUrl, byte[] idA) {
        byte[] kA = this.generateKA();
        long lastContactTimestamp = TimeUtils.getNtpSeconds(currentEpoch - 96, serverConfigurationService.getServiceTimeStart());
        
        Registration reg = Registration.builder()
                .permanentIdentifier(idA)
                .atRisk(true)
                .isNotified(false)
                .latestRiskEpoch(currentEpoch - 10)
                .lastContactTimestamp(TimeUtils.dayTruncatedTimestamp(lastContactTimestamp))
                .lastStatusRequestEpoch(currentEpoch - 3).build();

        byte[][] reqContent = createEBIDTimeMACFor(idA, kA, currentEpoch);

        statusBody = StatusVo.builder()
                .ebid(Base64.encode(reqContent[0]))
                .epochId(currentEpoch)
                .time(Base64.encode(reqContent[1]))
                .mac(Base64.encode(reqContent[2]))
                .build();

        byte[] decryptedEbid = new byte[8];
        System.arraycopy(idA, 0, decryptedEbid, 3, 5);
        System.arraycopy(ByteUtils.intToBytes(currentEpoch), 1, decryptedEbid, 0, 3);

        doReturn(Optional.of(reg)).when(this.registrationService).findById(idA);

        doReturn(Optional.of(GetIdFromStatusResponse.newBuilder()
                .setEpochId(currentEpoch)
                .setIdA(ByteString.copyFrom(idA))
                .setTuples(ByteString.copyFrom("Base64encodedEncryptedJSONStringWithTuples".getBytes()))
                .build()))
        .when(this.cryptoServerClient).getIdFromStatus(any());

        this.requestEntity = new HttpEntity<>(this.statusBody, this.headers);
        return reg;
    }
    
    protected void statusRequestAtRiskSucceedsV1ToV4(URI targetUrl) {
        // Given
        byte[] idA = this.generateKey(5);
        Registration reg = this.statusRequestAtRiskSucceedsSetUp(targetUrl, idA);

        when(this.wsServerConfiguration.getDeclareTokenKid()).thenReturn("kid");
        when(this.wsServerConfiguration.getJwtUseTransientKey()).thenReturn(true);
        
        // When
        ResponseEntity<StatusResponseDtoV1ToV4> response = this.restTemplate.exchange(targetUrl.toString(),
                HttpMethod.POST, this.requestEntity, StatusResponseDtoV1ToV4.class);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isAtRisk());
        assertNotNull(response.getBody().getTuples());
        assertTrue(reg.isNotified());
        assertTrue(currentEpoch - 3 < reg.getLastStatusRequestEpoch());
        verify(this.registrationService, times(2)).findById(idA);
        verify(this.registrationService, times(2)).saveRegistration(reg);
        verify(this.restApiService, never()).registerPushNotif(any(PushInfoVo.class));
    }

    protected void statusRequestAtRiskSucceedsV5(URI targetUrl) {
        // Given
        byte[] idA = this.generateKey(5);
        Registration reg = this.statusRequestAtRiskSucceedsSetUp(targetUrl, idA);

        when(this.wsServerConfiguration.getDeclareTokenKid()).thenReturn("kid");
        when(this.wsServerConfiguration.getJwtUseTransientKey()).thenReturn(true);

        // When
        ResponseEntity<StatusResponseDtoV5> response = this.restTemplate.exchange(targetUrl.toString(),
                HttpMethod.POST, this.requestEntity, StatusResponseDtoV5.class);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(RiskLevel.HIGH, response.getBody().getRiskLevel());
        assertNotNull(response.getBody().getTuples());
        assertEquals(Long.toString(reg.getLastContactTimestamp()), response.getBody().getLastContactDate());
        assertNotNull(response.getBody().getLastRiskScoringDate());
        assertTrue(Long.parseLong(response.getBody().getLastRiskScoringDate()) > 0);
        assertTrue(reg.isNotified());
        assertTrue(currentEpoch - 3 < reg.getLastStatusRequestEpoch());
        verify(this.registrationService, times(2)).findById(idA);
        verify(this.registrationService, times(2)).saveRegistration(reg);
        verify(this.restApiService, never()).registerPushNotif(any(PushInfoVo.class));
    }


    protected void statusRequestAtRiskSucceeds(URI targetUrl) {
        // Given
        byte[] idA = this.generateKey(5);
        Registration reg = this.statusRequestAtRiskSucceedsSetUp(targetUrl, idA);

        when(this.wsServerConfiguration.getDeclareTokenKid()).thenReturn("kid");
        when(this.wsServerConfiguration.getJwtUseTransientKey()).thenReturn(true);

        // When
        ResponseEntity<StatusResponseDto> response = this.restTemplate.exchange(targetUrl.toString(),
                HttpMethod.POST, this.requestEntity, StatusResponseDto.class);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(RiskLevel.HIGH, response.getBody().getRiskLevel());
        assertNotNull(response.getBody().getTuples());
        assertEquals(Long.toString(reg.getLastContactTimestamp()), response.getBody().getLastContactDate());
        assertNotNull(response.getBody().getLastRiskScoringDate());
        assertTrue(Long.parseLong(response.getBody().getLastRiskScoringDate()) > 0);
        assertTrue(reg.isNotified());
        assertTrue(currentEpoch - 3 < reg.getLastStatusRequestEpoch());
        verify(this.registrationService, times(2)).findById(idA);
        verify(this.registrationService, times(2)).saveRegistration(reg);
        verify(this.restApiService, never()).registerPushNotif(any(PushInfoVo.class));
        assertNotNull(response.getBody().getAnalyticsToken());
    }

    @Test
    public void testStatusRequestNotAtRiskSucceeds() {

        // Given
        byte[] idA = this.generateKey(5);
        byte[] kA = this.generateKA();

        Registration reg = Registration.builder()
                .permanentIdentifier(idA)
                .atRisk(false)
                .isNotified(false)
                .lastStatusRequestEpoch(currentEpoch - 3).build();

        byte[][] reqContent = createEBIDTimeMACFor(idA, kA, currentEpoch);

        statusBody = StatusVo.builder()
                .ebid(Base64.encode(reqContent[0]))
                .epochId(currentEpoch)
                .time(Base64.encode(reqContent[1]))
                .mac(Base64.encode(reqContent[2])).build();

        this.requestEntity = new HttpEntity<>(this.statusBody, this.headers);

        byte[] decryptedEbid = new byte[8];
        System.arraycopy(idA, 0, decryptedEbid, 3, 5);
        System.arraycopy(ByteUtils.intToBytes(currentEpoch), 1, decryptedEbid, 0, 3);

        when(this.wsServerConfiguration.getJwtUseTransientKey()).thenReturn(true);

        doReturn(Optional.of(reg)).when(this.registrationService).findById(idA);

        doReturn(Optional.of(GetIdFromStatusResponse.newBuilder()
                .setEpochId(currentEpoch)
                .setIdA(ByteString.copyFrom(idA))
                .setTuples(ByteString.copyFrom("Base64encodedEncryptedJSONStringWithTuples".getBytes()))
                .build()))
        .when(this.cryptoServerClient).getIdFromStatus(any());

        this.requestEntity = new HttpEntity<>(this.statusBody, this.headers);

        // When
        ResponseEntity<StatusResponseDto> response = this.restTemplate.exchange(this.targetUrl.toString(),
                HttpMethod.POST, this.requestEntity, StatusResponseDto.class);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(RiskLevel.NONE, response.getBody().getRiskLevel());
        assertNotNull(response.getBody().getTuples());
        assertTrue(currentEpoch - 3 < reg.getLastStatusRequestEpoch());
        assertThat(response.getBody().getLastContactDate()).isNull();
        assertThat(response.getBody().getLastRiskScoringDate()).isNull();
        assertEquals(0, reg.getLastContactTimestamp());
        verify(this.registrationService, times(2)).findById(idA);
        verify(this.registrationService, times(2)).saveRegistration(reg);
        verify(this.restApiService, never()).registerPushNotif(any(PushInfoVo.class));
    }

    @Test
    public void testStatusRequestNoNewRiskSinceLastNotifSucceeds() {
        byte[] idA = this.generateKey(5);
        byte[] kA = this.generateKA();

        when(this.wsServerConfiguration.getJwtUseTransientKey()).thenReturn(true);

        List<EpochExposition> epochExpositions = new ArrayList<>();

        // Before latest notification
        epochExpositions.add(EpochExposition.builder()
                .epochId(currentEpoch - 9)
                .expositionScores(Arrays.asList(3.00, 4.30))
                .build());

        epochExpositions.add(EpochExposition.builder()
                .epochId(currentEpoch - 4)
                .expositionScores(Arrays.asList(0.076, 0.15))
                .build());

        epochExpositions.add(EpochExposition.builder()
                .epochId(currentEpoch - 3)
                .expositionScores(Arrays.asList(0.052, 0.16))
                .build());
        long lastContactTimestamp = TimeUtils.getNtpSeconds(currentEpoch - 24, serverConfigurationService.getServiceTimeStart());

        Registration reg = Registration.builder()
                .permanentIdentifier(idA)
                .atRisk(false)
                .isNotified(true)
                .lastStatusRequestEpoch(currentEpoch - 3)
                .latestRiskEpoch(currentEpoch - 8)
                .lastContactTimestamp(TimeUtils.dayTruncatedTimestamp(lastContactTimestamp))
                .exposedEpochs(epochExpositions)
                .build();


        byte[][] reqContent = createEBIDTimeMACFor(idA, kA, currentEpoch);

        byte[] decryptedEbid = new byte[8];
        System.arraycopy(idA, 0, decryptedEbid, 3, 5);
        System.arraycopy(ByteUtils.intToBytes(currentEpoch), 1, decryptedEbid, 0, 3);

        doReturn(Optional.of(reg)).when(this.registrationService).findById(idA);

        doReturn(Optional.of(GetIdFromStatusResponse.newBuilder()
                .setEpochId(currentEpoch)
                .setIdA(ByteString.copyFrom(idA))
                .setTuples(ByteString.copyFrom("Base64encodedEncryptedJSONStringWithTuples".getBytes()))
                .build()))
        .when(this.cryptoServerClient).getIdFromStatus(any());

        doReturn(Optional.of(reg)).when(this.registrationService).saveRegistration(reg);

        statusBody = StatusVo.builder()
                .ebid(Base64.encode(reqContent[0]))
                .epochId(currentEpoch)
                .time(Base64.encode(reqContent[1]))
                .mac(Base64.encode(reqContent[2]))
                .build();

        this.requestEntity = new HttpEntity<>(this.statusBody, this.headers);

        ResponseEntity<StatusResponseDto> response = this.restTemplate.exchange(this.targetUrl.toString(),
                HttpMethod.POST, this.requestEntity, StatusResponseDto.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(currentEpoch, reg.getLastStatusRequestEpoch());
        assertEquals(RiskLevel.NONE, response.getBody().getRiskLevel());
        assertNull(response.getBody().getLastRiskScoringDate());
        assertNull(response.getBody().getLastContactDate());
        assertNotNull(response.getBody().getTuples());
        assertFalse(reg.isAtRisk());
        assertTrue(reg.isNotified());
        verify(this.registrationService, times(2)).findById(idA);
        verify(this.registrationService, times(2)).saveRegistration(reg);
        verify(this.restApiService, never()).registerPushNotif(any(PushInfoVo.class));
    }

    @Test
    public void testStatusRequestNewRiskSinceLastNotifSucceeds() {
        byte[] idA = this.generateKey(5);
        byte[] kA = this.generateKA();

        List<EpochExposition> epochExpositions = new ArrayList<>();

        // Before latest notification
        epochExpositions.add(EpochExposition.builder()
                .epochId(currentEpoch - 7)
                .expositionScores(Arrays.asList(3.00, 4.30))
                .build());

        epochExpositions.add(EpochExposition.builder()
                .epochId(currentEpoch - 4)
                .expositionScores(Arrays.asList(0.076, 0.15))
                .build());

        epochExpositions.add(EpochExposition.builder()
                .epochId(currentEpoch - 3)
                .expositionScores(Arrays.asList(0.052, 0.16))
                .build());

        Registration reg = Registration.builder()
                .permanentIdentifier(idA)
                .atRisk(true)
                .isNotified(true)
                .lastStatusRequestEpoch(currentEpoch - 3)
                .latestRiskEpoch(currentEpoch - 8)
                .exposedEpochs(epochExpositions)
                .build();

        byte[][] reqContent = createEBIDTimeMACFor(idA, kA, currentEpoch);

        byte[] decryptedEbid = new byte[8];
        System.arraycopy(idA, 0, decryptedEbid, 3, 5);
        System.arraycopy(ByteUtils.intToBytes(currentEpoch), 1, decryptedEbid, 0, 3);

        when(this.wsServerConfiguration.getJwtUseTransientKey()).thenReturn(true);

        doReturn(Optional.of(reg)).when(this.registrationService).findById(idA);

        doReturn(Optional.of(GetIdFromStatusResponse.newBuilder()
                .setEpochId(currentEpoch)
                .setIdA(ByteString.copyFrom(idA))
                .setTuples(ByteString.copyFrom("Base64encodedEncryptedJSONStringWithTuples".getBytes()))
                .build()))
        .when(this.cryptoServerClient).getIdFromStatus(any());

        statusBody = StatusVo.builder()
                .ebid(Base64.encode(reqContent[0]))
                .epochId(currentEpoch)
                .time(Base64.encode(reqContent[1]))
                .mac(Base64.encode(reqContent[2]))
                .build();

        this.requestEntity = new HttpEntity<>(this.statusBody, this.headers);

        ResponseEntity<StatusResponseDto> response = this.restTemplate.exchange(this.targetUrl.toString(),
                HttpMethod.POST, this.requestEntity, StatusResponseDto.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(currentEpoch, reg.getLastStatusRequestEpoch());
        assertEquals(RiskLevel.HIGH, response.getBody().getRiskLevel());
        assertNotNull(response.getBody().getTuples());
        assertTrue(reg.isAtRisk());
        assertTrue(reg.isNotified());
        verify(this.registrationService, times(2)).findById(idA);
        verify(this.registrationService, times(2)).saveRegistration(reg);
        verify(this.restApiService, never()).registerPushNotif(any(PushInfoVo.class));
    }

    @Test
    public void testStatusRequestESRThrottleFails() {

        // Given
        String message = "Discarding ESR request because epochs are too close:";
        String errorMessage = String.format("%s"
                + " last ESR request epoch %d vs current epoch %d => %d < %d (tolerance)",
                message,
                currentEpoch,
                currentEpoch,
                0,
                this.wsServerConfiguration.getStatusRequestMinimumEpochGap());

        byte[] idA = this.generateKey(5);
        byte[] kA = this.generateKA();
        Registration reg = Registration.builder()
                .permanentIdentifier(idA)
                .atRisk(false)
                .isNotified(false)
                .lastStatusRequestEpoch(currentEpoch).build();

        byte[][] reqContent = createEBIDTimeMACFor(idA, kA, currentEpoch);

        statusBody = StatusVo.builder()
                .ebid(Base64.encode(reqContent[0]))
                .epochId(currentEpoch)
                .time(Base64.encode(reqContent[1]))
                .mac(Base64.encode(reqContent[2])).build();

        byte[] decryptedEbid = new byte[8];
        System.arraycopy(idA, 0, decryptedEbid, 3, 5);
        System.arraycopy(ByteUtils.intToBytes(currentEpoch), 1, decryptedEbid, 0, 3);

        doReturn(Optional.of(reg)).when(this.registrationService).findById(idA);

        doReturn(Optional.of(GetIdFromStatusResponse.newBuilder()
                .setEpochId(currentEpoch)
                .setIdA(ByteString.copyFrom(idA))
                .setTuples(ByteString.copyFrom("Base64encodedEncryptedJSONStringWithTuples".getBytes()))
                .build()))
        .when(this.cryptoServerClient).getIdFromStatus(any());

        this.requestEntity = new HttpEntity<>(this.statusBody, this.headers);


        // When
        ResponseEntity<StatusResponseDto> response = this.restTemplate.exchange(this.targetUrl.toString(),
                HttpMethod.POST, this.requestEntity, StatusResponseDto.class);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(currentEpoch, reg.getLastStatusRequestEpoch());
        assertEquals(currentEpoch, reg.getLastFailedStatusRequestEpoch());
        assertEquals(errorMessage, reg.getLastFailedStatusRequestMessage());
        verify(this.registrationService, times(2)).findById(idA);
        verify(this.registrationService, times(2)).saveRegistration(reg);
        verify(this.restApiService, never()).registerPushNotif(any(PushInfoVo.class));
    }

    @Test
    public void testStatusRequestESRThrottleFailsEvenWithPushInfo() {

        // Given
        String message = "Discarding ESR request because epochs are too close:";
        String errorMessage = String.format("%s"
                + " last ESR request epoch %d vs current epoch %d => %d < %d (tolerance)",
                message,
                currentEpoch,
                currentEpoch,
                0,
                this.wsServerConfiguration.getStatusRequestMinimumEpochGap());

        byte[] idA = this.generateKey(5);
        byte[] kA = this.generateKA();
        Registration reg = Registration.builder()
                .permanentIdentifier(idA)
                .atRisk(false)
                .isNotified(false)
                .lastStatusRequestEpoch(currentEpoch).build();

        byte[][] reqContent = createEBIDTimeMACFor(idA, kA, currentEpoch);

        PushInfoVo pushInfo = PushInfoVo.builder()
                .token("token")
                .locale("en-US")
                .timezone("Europe/Paris")
                .build();

        statusBody = StatusVo.builder()
                .ebid(Base64.encode(reqContent[0]))
                .epochId(currentEpoch)
                .time(Base64.encode(reqContent[1]))
                .mac(Base64.encode(reqContent[2]))
                .pushInfo(pushInfo)
                .build();

        byte[] decryptedEbid = new byte[8];
        System.arraycopy(idA, 0, decryptedEbid, 3, 5);
        System.arraycopy(ByteUtils.intToBytes(currentEpoch), 1, decryptedEbid, 0, 3);

        doReturn(Optional.of(reg)).when(this.registrationService).findById(idA);

        doReturn(Optional.of(GetIdFromStatusResponse.newBuilder()
                .setEpochId(currentEpoch)
                .setIdA(ByteString.copyFrom(idA))
                .setTuples(ByteString.copyFrom("Base64encodedEncryptedJSONStringWithTuples".getBytes()))
                .build()))
        .when(this.cryptoServerClient).getIdFromStatus(any());

        this.requestEntity = new HttpEntity<>(this.statusBody, this.headers);


        // When
        ResponseEntity<StatusResponseDto> response = this.restTemplate.exchange(this.targetUrl.toString(),
                HttpMethod.POST, this.requestEntity, StatusResponseDto.class);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(currentEpoch, reg.getLastStatusRequestEpoch());
        assertEquals(currentEpoch, reg.getLastFailedStatusRequestEpoch());
        assertEquals(errorMessage, reg.getLastFailedStatusRequestMessage());
        verify(this.registrationService, times(2)).findById(idA);
        verify(this.registrationService, times(2)).saveRegistration(reg);
        verify(this.restApiService, never()).registerPushNotif(any(PushInfoVo.class));
    }

    @Test
    public void testStatusRequestESRThrottleShouldSucceedsWhenLimitIsZero() {

        // Given
        byte[] idA = this.generateKey(5);
        byte[] kA = this.generateKA();
        Registration reg = Registration.builder()
                .permanentIdentifier(idA)
                .atRisk(true)
                .isNotified(false)
                .lastStatusRequestEpoch(currentEpoch - 3).build();

        byte[][] reqContent = createEBIDTimeMACFor(idA, kA, currentEpoch);

        statusBody = StatusVo.builder()
                .ebid(Base64.encode(reqContent[0]))
                .epochId(currentEpoch)
                .time(Base64.encode(reqContent[1]))
                .mac(Base64.encode(reqContent[2]))
                .build();

        byte[] decryptedEbid = new byte[8];
        System.arraycopy(idA, 0, decryptedEbid, 3, 5);
        System.arraycopy(ByteUtils.intToBytes(currentEpoch), 1, decryptedEbid, 0, 3);

        when(this.wsServerConfiguration.getJwtUseTransientKey()).thenReturn(true);

        doReturn(Optional.of(reg)).when(this.registrationService).findById(idA);

        doReturn(Optional.of(GetIdFromStatusResponse.newBuilder()
                .setEpochId(currentEpoch)
                .setIdA(ByteString.copyFrom(idA))
                .setTuples(ByteString.copyFrom("Base64encodedEncryptedJSONStringWithTuples".getBytes()))
                .build()))
        .when(this.cryptoServerClient).getIdFromStatus(any());

        when(this.propertyLoader.getEsrLimit()).thenReturn(0);

        this.requestEntity = new HttpEntity<>(this.statusBody, this.headers);

        // When
        ResponseEntity<StatusResponseDto> response = this.restTemplate.exchange(this.targetUrl.toString(),
                HttpMethod.POST, this.requestEntity, StatusResponseDto.class);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(currentEpoch, reg.getLastStatusRequestEpoch());
        assertEquals(RiskLevel.HIGH, response.getBody().getRiskLevel());
        assertNotNull(response.getBody().getTuples());
        assertTrue(reg.isAtRisk());
        assertTrue(reg.isNotified());
        verify(this.registrationService, times(2)).findById(idA);
        verify(this.registrationService, times(2)).saveRegistration(reg);
        verify(this.restApiService, never()).registerPushNotif(any(PushInfoVo.class));
    }

    @Test
    public void testStatusRequestCallRegisterPushWhenESRSucceedsAndPushInfoIsProvided() {

        // Given
        byte[] idA = this.generateKey(5);
        byte[] kA = this.generateKA();
        Registration reg = Registration.builder()
                .permanentIdentifier(idA)
                .atRisk(true)
                .isNotified(false)
                .lastStatusRequestEpoch(currentEpoch - 3).build();

        byte[][] reqContent = createEBIDTimeMACFor(idA, kA, currentEpoch);

        PushInfoVo pushInfo = PushInfoVo.builder()
                .token("token")
                .locale("en-US")
                .timezone("Europe/Paris")
                .build();

        statusBody = StatusVo.builder()
                .ebid(Base64.encode(reqContent[0]))
                .epochId(currentEpoch)
                .time(Base64.encode(reqContent[1]))
                .mac(Base64.encode(reqContent[2]))
                .pushInfo(pushInfo)
                .build();

        byte[] decryptedEbid = new byte[8];
        System.arraycopy(idA, 0, decryptedEbid, 3, 5);
        System.arraycopy(ByteUtils.intToBytes(currentEpoch), 1, decryptedEbid, 0, 3);

        when(this.wsServerConfiguration.getJwtUseTransientKey()).thenReturn(true);

        doReturn(Optional.of(reg)).when(this.registrationService).findById(idA);

        doReturn(Optional.of(GetIdFromStatusResponse.newBuilder()
                .setEpochId(currentEpoch)
                .setIdA(ByteString.copyFrom(idA))
                .setTuples(ByteString.copyFrom("Base64encodedEncryptedJSONStringWithTuples".getBytes()))
                .build()))
        .when(this.cryptoServerClient).getIdFromStatus(any());

        when(this.propertyLoader.getEsrLimit()).thenReturn(0);

        this.requestEntity = new HttpEntity<>(this.statusBody, this.headers);

        // When
        ResponseEntity<StatusResponseDto> response = this.restTemplate.exchange(this.targetUrl.toString(),
                HttpMethod.POST, this.requestEntity, StatusResponseDto.class);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(currentEpoch, reg.getLastStatusRequestEpoch());
        assertEquals(RiskLevel.HIGH, response.getBody().getRiskLevel());
        assertNotNull(response.getBody().getTuples());
        assertTrue(reg.isAtRisk());
        assertTrue(reg.isNotified());
        verify(this.registrationService, times(2)).findById(idA);
        verify(this.registrationService, times(2)).saveRegistration(reg);
        verify(this.restApiService).registerPushNotif(pushInfo);
    }

    @Test
    public void testStatusStoreDriftWhenRequestOKSucceeds() {

        // Given
        byte[] idA = this.generateKey(5);
        byte[] kA = this.generateKA();

        List<EpochExposition> epochExpositions = new ArrayList<>();

        // Before latest notification
        epochExpositions.add(EpochExposition.builder()
                .epochId(currentEpoch - 7)
                .expositionScores(Arrays.asList(3.00, 4.30))
                .build());

        epochExpositions.add(EpochExposition.builder()
                .epochId(currentEpoch - 4)
                .expositionScores(Arrays.asList(0.076, 0.15))
                .build());

        epochExpositions.add(EpochExposition.builder()
                .epochId(currentEpoch - 3)
                .expositionScores(Arrays.asList(0.052, 0.16))
                .build());

        Registration reg = Registration.builder()
                .permanentIdentifier(idA)
                .atRisk(true)
                .isNotified(true)
                .lastStatusRequestEpoch(currentEpoch - 3)
                .latestRiskEpoch(currentEpoch - 8)
                .exposedEpochs(epochExpositions)
                .build();

        int timestampDelta = -20;

        byte[][] reqContent = createEBIDTimeMACFor(idA, kA, currentEpoch, timestampDelta);

        byte[] decryptedEbid = new byte[8];
        System.arraycopy(idA, 0, decryptedEbid, 3, 5);
        System.arraycopy(ByteUtils.intToBytes(currentEpoch), 1, decryptedEbid, 0, 3);

        when(this.wsServerConfiguration.getJwtUseTransientKey()).thenReturn(true);

        doReturn(Optional.of(reg)).when(this.registrationService).findById(idA);

        doReturn(Optional.of(GetIdFromStatusResponse.newBuilder()
                .setEpochId(currentEpoch)
                .setIdA(ByteString.copyFrom(idA))
                .setTuples(ByteString.copyFrom("Base64encodedEncryptedJSONStringWithTuples".getBytes()))
                .build()))
        .when(this.cryptoServerClient).getIdFromStatus(any());

        statusBody = StatusVo.builder()
                .ebid(Base64.encode(reqContent[0]))
                .epochId(currentEpoch)
                .time(Base64.encode(reqContent[1]))
                .mac(Base64.encode(reqContent[2]))
                .build();

        this.requestEntity = new HttpEntity<>(this.statusBody, this.headers);

        // When
        ResponseEntity<StatusResponseDto> response = this.restTemplate.exchange(this.targetUrl.toString(),
                HttpMethod.POST, this.requestEntity, StatusResponseDto.class);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(currentEpoch, reg.getLastStatusRequestEpoch());
        assertEquals(RiskLevel.HIGH, response.getBody().getRiskLevel());
        assertNotNull(response.getBody().getTuples());
        assertTrue(reg.isAtRisk());
        assertTrue(reg.isNotified());
        assertTrue(reg.getLastTimestampDrift() == Math.abs(timestampDelta) + 1 || reg.getLastTimestampDrift() == Math.abs(timestampDelta));
        verify(this.registrationService, times(2)).findById(idA);
        verify(this.registrationService, times(2)).saveRegistration(reg);
        verify(this.restApiService, never()).registerPushNotif(any(PushInfoVo.class));
    }

    @Test
    public void testStatusStoreDriftWhenRequestKOSucceeds() {
        // Given
        byte[] idA = this.generateKey(5);
        byte[] kA = this.generateKA();
        Registration reg = Registration.builder()
                .permanentIdentifier(idA)
                .atRisk(false)
                .isNotified(false)
                .lastStatusRequestEpoch(currentEpoch).build();

        int timestampDelta = -23;

        byte[][] reqContent = createEBIDTimeMACFor(idA, kA, currentEpoch, timestampDelta);

        statusBody = StatusVo.builder()
                .ebid(Base64.encode(reqContent[0]))
                .epochId(currentEpoch)
                .time(Base64.encode(reqContent[1]))
                .mac(Base64.encode(reqContent[2])).build();

        byte[] decryptedEbid = new byte[8];
        System.arraycopy(idA, 0, decryptedEbid, 3, 5);
        System.arraycopy(ByteUtils.intToBytes(currentEpoch), 1, decryptedEbid, 0, 3);

        doReturn(Optional.of(reg)).when(this.registrationService).findById(idA);

        doReturn(Optional.of(GetIdFromStatusResponse.newBuilder()
                .setEpochId(currentEpoch)
                .setIdA(ByteString.copyFrom(idA))
                .setTuples(ByteString.copyFrom("Base64encodedEncryptedJSONStringWithTuples".getBytes()))
                .build()))
        .when(this.cryptoServerClient).getIdFromStatus(any());

        this.requestEntity = new HttpEntity<>(this.statusBody, this.headers);


        // When
        ResponseEntity<StatusResponseDto> response = this.restTemplate.exchange(this.targetUrl.toString(),
                HttpMethod.POST, this.requestEntity, StatusResponseDto.class);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(currentEpoch, reg.getLastStatusRequestEpoch());
        assertTrue(reg.getLastTimestampDrift() == Math.abs(timestampDelta) + 1 || reg.getLastTimestampDrift() == Math.abs(timestampDelta));
        verify(this.registrationService, times(2)).findById(idA);
        verify(this.registrationService, times(2)).saveRegistration(reg);
        verify(this.restApiService, never()).registerPushNotif(any(PushInfoVo.class));
    }

    @Test
    public void when_not_at_risk_then_status_does_not_have_a_declaration_token() {

        // Given
        byte[] idA = this.generateKey(5);
        byte[] kA = this.generateKA();

        Registration reg = Registration.builder()
                .permanentIdentifier(idA)
                .atRisk(false)
                .isNotified(false)
                .lastStatusRequestEpoch(currentEpoch - 3).build();

        byte[][] reqContent = createEBIDTimeMACFor(idA, kA, currentEpoch);

        statusBody = StatusVo.builder()
                .ebid(Base64.encode(reqContent[0]))
                .epochId(currentEpoch)
                .time(Base64.encode(reqContent[1]))
                .mac(Base64.encode(reqContent[2])).build();

        this.requestEntity = new HttpEntity<>(this.statusBody, this.headers);

        byte[] decryptedEbid = new byte[8];
        System.arraycopy(idA, 0, decryptedEbid, 3, 5);
        System.arraycopy(ByteUtils.intToBytes(currentEpoch), 1, decryptedEbid, 0, 3);

        when(this.wsServerConfiguration.getJwtUseTransientKey()).thenReturn(true);

        doReturn(Optional.of(reg)).when(this.registrationService).findById(idA);

        doReturn(Optional.of(GetIdFromStatusResponse.newBuilder()
                .setEpochId(currentEpoch)
                .setIdA(ByteString.copyFrom(idA))
                .setTuples(ByteString.copyFrom("Base64encodedEncryptedJSONStringWithTuples".getBytes()))
                .build()))
                .when(this.cryptoServerClient).getIdFromStatus(any());

        this.requestEntity = new HttpEntity<>(this.statusBody, this.headers);

        // When
        ResponseEntity<StatusResponseDto> response = this.restTemplate.exchange(this.targetUrl.toString(),
                HttpMethod.POST, this.requestEntity, StatusResponseDto.class);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNull(Objects.requireNonNull(response.getBody()).getDeclarationToken());
        assertEquals(currentEpoch, reg.getLastStatusRequestEpoch());
        assertEquals(RiskLevel.NONE, response.getBody().getRiskLevel());
        assertFalse(reg.isAtRisk());

    }

    @Test
    public void when_at_risk_then_status_got_a_declaration_token_with_last_contact_date_and_last_status_request_both_in_timestamp() {

        // Given
        KeyPair keyPair = Keys.keyPairFor(SignatureAlgorithm.RS256);

        byte[] idA = this.generateKey(5);
        Registration reg = this.statusRequestAtRiskSucceedsSetUp(targetUrl, idA);

        when(this.wsServerConfiguration.getDeclareTokenKid()).thenReturn("kid");
        when(this.wsServerConfiguration.getDeclareTokenPrivateKey()).thenReturn(Encoders.BASE64.encode(keyPair.getPrivate().getEncoded()));
        when(this.wsServerConfiguration.getJwtUseTransientKey()).thenReturn(false);

        when(this.wsServerConfiguration.getAnalyticsTokenPrivateKey()).thenReturn(Encoders.BASE64.encode(keyPair.getPrivate().getEncoded()));

        // When
        ResponseEntity<StatusResponseDto> response = this.restTemplate.exchange(targetUrl.toString(),
                HttpMethod.POST, this.requestEntity, StatusResponseDto.class);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(RiskLevel.HIGH, Objects.requireNonNull(response.getBody()).getRiskLevel());
        assertNotNull(response.getBody().getTuples());
        assertNotNull(response.getBody().getDeclarationToken());
        assertEquals(Long.toString(reg.getLastContactTimestamp()), response.getBody().getLastContactDate());
        assertTrue(reg.isNotified());
        assertTrue(currentEpoch - 3 < reg.getLastStatusRequestEpoch());
        verify(this.registrationService, times(2)).findById(idA);
        verify(this.registrationService, times(2)).saveRegistration(reg);
        verify(this.restApiService, never()).registerPushNotif(any(PushInfoVo.class));

        Claims claims = Jwts.parserBuilder()
                .setSigningKey(keyPair.getPublic())
                .build()
                .parseClaimsJws(response.getBody().getDeclarationToken())
                .getBody();

        assertNotNull(claims.get("notificationDateTimestamp"));
        assertEquals(claims.get("notificationDateTimestamp"), TimeUtils.getNtpSeconds(
                reg.getLastStatusRequestEpoch(),
                serverConfigurationService.getServiceTimeStart()));
        assertNotNull(claims.get("lastContactDateTimestamp"));
        assertEquals(claims.get("lastContactDateTimestamp"), reg.getLastContactTimestamp());
    }

    //Implement me
    @Test
    public void when_at_risk_during_risk_retention_period_then_two_status_have_declarations_token_with_different_last_status_request_timestamp() {


    }

    //Implement me
    @Test
    public void when_at_risk_during_risk_retention_period_then_two_status_have_declarations_token_with_same_last_contact_date_timestamp() {


    }

    //Implement me
    @Test
    public void when_at_risk_after_risk_retention_period_then_two_status_have_declarations_token_with_different_identifier_and_last_contact_date() {


    }

    @Test
    public void when_calling_status_should_return_an_analytics_token() {

        // Given
        byte[] idA = this.generateKey(5);
        byte[] kA = this.generateKA();

        Registration reg = Registration.builder()
                .permanentIdentifier(idA)
                .atRisk(false)
                .isNotified(false)
                .lastStatusRequestEpoch(currentEpoch - 3).build();

        byte[][] reqContent = createEBIDTimeMACFor(idA, kA, currentEpoch);

        statusBody = StatusVo.builder()
                .ebid(Base64.encode(reqContent[0]))
                .epochId(currentEpoch)
                .time(Base64.encode(reqContent[1]))
                .mac(Base64.encode(reqContent[2])).build();

        this.requestEntity = new HttpEntity<>(this.statusBody, this.headers);

        byte[] decryptedEbid = new byte[8];
        System.arraycopy(idA, 0, decryptedEbid, 3, 5);
        System.arraycopy(ByteUtils.intToBytes(currentEpoch), 1, decryptedEbid, 0, 3);

        when(this.wsServerConfiguration.getJwtUseTransientKey()).thenReturn(true);

        doReturn(Optional.of(reg)).when(this.registrationService).findById(idA);

        doReturn(Optional.of(GetIdFromStatusResponse.newBuilder()
                .setEpochId(currentEpoch)
                .setIdA(ByteString.copyFrom(idA))
                .setTuples(ByteString.copyFrom("Base64encodedEncryptedJSONStringWithTuples".getBytes()))
                .build()))
                .when(this.cryptoServerClient).getIdFromStatus(any());

        this.requestEntity = new HttpEntity<>(this.statusBody, this.headers);

        // When
        ResponseEntity<StatusResponseDto> response = this.restTemplate.exchange(this.targetUrl.toString(),
                HttpMethod.POST, this.requestEntity, StatusResponseDto.class);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody().getAnalyticsToken());
    }

}
