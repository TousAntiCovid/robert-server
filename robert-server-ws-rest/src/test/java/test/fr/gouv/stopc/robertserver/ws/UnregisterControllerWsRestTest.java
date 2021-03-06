package test.fr.gouv.stopc.robertserver.ws;

import com.google.protobuf.ByteString;
import fr.gouv.stopc.robert.crypto.grpc.server.client.service.ICryptoServerGrpcClient;
import fr.gouv.stopc.robert.crypto.grpc.server.messaging.DeleteIdResponse;
import fr.gouv.stopc.robert.crypto.grpc.server.messaging.ErrorMessage;
import fr.gouv.stopc.robert.crypto.grpc.server.messaging.GetIdFromAuthResponse;
import fr.gouv.stopc.robert.server.common.DigestSaltEnum;
import fr.gouv.stopc.robert.server.common.service.IServerConfigurationService;
import fr.gouv.stopc.robert.server.common.utils.ByteUtils;
import fr.gouv.stopc.robert.server.common.utils.TimeUtils;
import fr.gouv.stopc.robert.server.crypto.service.CryptoService;
import fr.gouv.stopc.robert.server.crypto.structure.impl.CryptoHMACSHA256;
import fr.gouv.stopc.robert.server.crypto.structure.impl.CryptoSkinny64;
import fr.gouv.stopc.robertserver.database.model.Registration;
import fr.gouv.stopc.robertserver.database.service.impl.RegistrationService;
import fr.gouv.stopc.robertserver.ws.dto.UnregisterResponseDto;
import fr.gouv.stopc.robertserver.ws.utils.PropertyLoader;
import fr.gouv.stopc.robertserver.ws.utils.UriConstants;
import fr.gouv.stopc.robertserver.ws.vo.UnregisterRequestVo;
import org.bson.internal.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.web.util.UriComponentsBuilder;

import javax.crypto.KeyGenerator;

import java.net.URI;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Optional;

import static fr.gouv.stopc.robertserver.ws.test.MockServerManager.verifyNoInteractionsWithPushNotifServer;
import static fr.gouv.stopc.robertserver.ws.test.MockServerManager.verifyPushNotifServerReceivedUnregisterForToken;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@LegacyIntegrationTest
public class UnregisterControllerWsRestTest {

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

    HttpEntity<UnregisterRequestVo> requestEntity;

    private URI targetUrl;

    private UnregisterRequestVo requestBody;

    private HttpHeaders headers;

    @MockBean
    private RegistrationService registrationService;

    @Autowired
    private CryptoService cryptoService;

    @MockBean
    private ICryptoServerGrpcClient cryptoServerClient;

    @Autowired
    private PropertyLoader propertyLoader;

    @Autowired
    private IServerConfigurationService serverConfigurationService;

    private int currentEpoch;

    private byte[] serverKey;

    @BeforeEach
    public void before() {
        MockitoAnnotations.initMocks(this);
        assert (this.restTemplate != null);
        this.headers = new HttpHeaders();
        this.headers.setContentType(MediaType.APPLICATION_JSON);
        this.targetUrl = UriComponentsBuilder.fromUriString(this.pathPrefix).path(UriConstants.UNREGISTER).build()
                .encode().toUri();

        this.currentEpoch = this.getCurrentEpoch();
        this.serverKey = generateKey(24);
    }

    @Test
    public void testBadHttpVerbFails() {
        this.requestBody = UnregisterRequestVo.builder().ebid(Base64.encode(new byte[4])).build();

        this.requestEntity = new HttpEntity<>(this.requestBody, this.headers);

        ResponseEntity<String> response = this.restTemplate.exchange(
                this.targetUrl.toString(), HttpMethod.GET,
                this.requestEntity, String.class
        );

        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, response.getStatusCode());
        verify(this.registrationService, never()).delete(ArgumentMatchers.any());
    }

    @Test
    public void testBadEBIDSizeFails() {
        byte[] idA = this.generateKey(5);
        byte[] kA = this.generateKA();
        byte[][] reqContent = createEBIDTimeMACFor(idA, kA, currentEpoch);

        requestBody = UnregisterRequestVo.builder()
                .ebid(Base64.encode("ABC".getBytes()))
                .epochId(currentEpoch)
                .time(Base64.encode(reqContent[1]))
                .mac(Base64.encode(reqContent[2]))
                .build();

        this.requestEntity = new HttpEntity<>(this.requestBody, this.headers);

        ResponseEntity<UnregisterResponseDto> response = this.restTemplate.exchange(
                this.targetUrl.toString(),
                HttpMethod.POST, this.requestEntity, UnregisterResponseDto.class
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(this.registrationService, never()).findById(ArgumentMatchers.any());
        verify(this.registrationService, never()).delete(ArgumentMatchers.any());
    }

    /** Test the access for API V2, should not be used since API V3 */
    @Test
    public void testAccessV2() {
        acceptOldEBIDValueEpochSucceeds(
                UriComponentsBuilder.fromUriString(this.pathPrefixV2).path(UriConstants.UNREGISTER).build().encode()
                        .toUri()
        );
    }

    /** Test the access for API V3, should not be used since API V4 */
    @Test
    public void testAccessV3() {
        acceptOldEBIDValueEpochSucceeds(
                UriComponentsBuilder.fromUriString(this.pathPrefixV3).path(UriConstants.UNREGISTER).build().encode()
                        .toUri()
        );
    }

    /** Test the access for API V4, should not be used since API V5 */
    @Test
    public void testAccessV4() {
        acceptOldEBIDValueEpochSucceeds(
                UriComponentsBuilder.fromUriString(this.pathPrefixV4).path(UriConstants.UNREGISTER).build().encode()
                        .toUri()
        );
    }

    /**
     * Test the access for API V5, should not be used since API V6
     */
    @Test
    public void testAccessV5() {
        acceptOldEBIDValueEpochSucceeds(
                UriComponentsBuilder.fromUriString(this.pathPrefixV5).path(UriConstants.UNREGISTER).build().encode()
                        .toUri()
        );
    }

    /**
     * {@link #acceptOldEBIDValueEpochSucceeds(URI)} and shortcut to test for API V4
     * exposure
     */
    @Test
    public void testAcceptOldEBIDValueEpochSucceeds() {
        acceptOldEBIDValueEpochSucceeds(this.targetUrl);
    }

    /**
     * Business requirement: app can use an old EBID to perform its request
     */
    protected void acceptOldEBIDValueEpochSucceeds(URI targetUrl) {

        // Given
        byte[] idA = this.generateKey(5);
        byte[] kA = this.generateKA();
        // Mess up with the epoch used to create the EBID
        byte[][] reqContent = createEBIDTimeMACFor(idA, kA, currentEpoch - 10);
        Registration reg = Registration.builder()
                .permanentIdentifier(idA)
                .atRisk(true)
                .isNotified(false)
                .lastStatusRequestEpoch(currentEpoch - 3).build();

        byte[] decryptedEbid = new byte[8];
        System.arraycopy(idA, 0, decryptedEbid, 3, idA.length);
        System.arraycopy(
                ByteUtils.intToBytes(currentEpoch - 10), 1, decryptedEbid, 0, decryptedEbid.length - idA.length
        );

        doReturn(Optional.of(reg)).when(this.registrationService).findById(idA);

        doReturn(
                Optional.of(
                        GetIdFromAuthResponse.newBuilder()
                                .setIdA(ByteString.copyFrom(idA))
                                .build()
                )
        )
                .when(this.cryptoServerClient).getIdFromAuth(any());

        doReturn(
                Optional.of(
                        DeleteIdResponse.newBuilder()
                                .setIdA(ByteString.copyFrom(idA))
                                .build()
                )
        )
                .when(this.cryptoServerClient).deleteId(any());

        this.requestBody = UnregisterRequestVo.builder()
                .ebid(Base64.encode(reqContent[0]))
                .epochId(currentEpoch)
                .time(Base64.encode(reqContent[1]))
                .mac(Base64.encode(reqContent[2]))
                .build();

        this.requestEntity = new HttpEntity<>(this.requestBody, this.headers);

        // When
        ResponseEntity<UnregisterResponseDto> response = this.restTemplate.exchange(
                targetUrl.toString(),
                HttpMethod.POST, this.requestEntity, UnregisterResponseDto.class
        );

        // Given
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(this.cryptoServerClient, times(1)).deleteId(ArgumentMatchers.any());
        verify(this.registrationService, times(3)).findById(ArgumentMatchers.any());
        verify(this.registrationService, times(1)).delete(ArgumentMatchers.any());
    }

    @Test
    public void testBadTimeFutureFails() {
        byte[] idA = this.generateKey(5);
        byte[] kA = this.generateKA();

        byte[][] reqContent = createEBIDTimeMACFor(
                idA,
                kA,
                currentEpoch,
                0 - (this.propertyLoader.getRequestTimeDeltaTolerance() + 1)
        );

        requestBody = UnregisterRequestVo.builder()
                .ebid(Base64.encode(reqContent[0]))
                .epochId(currentEpoch)
                .time(Base64.encode(reqContent[1]))
                .mac(Base64.encode(reqContent[2]))
                .build();

        this.requestEntity = new HttpEntity<>(this.requestBody, this.headers);

        ResponseEntity<UnregisterResponseDto> response = this.restTemplate.exchange(
                this.targetUrl.toString(),
                HttpMethod.POST, this.requestEntity, UnregisterResponseDto.class
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(this.registrationService, never()).findById(ArgumentMatchers.any());
        verify(this.registrationService, never()).delete(ArgumentMatchers.any());
    }

    @Test
    public void testBadTimePastFails() {
        byte[] idA = this.generateKey(5);
        byte[] kA = this.generateKA();

        byte[][] reqContent = createEBIDTimeMACFor(
                idA,
                kA,
                currentEpoch,
                0 - (this.propertyLoader.getRequestTimeDeltaTolerance() + 1)
        );

        requestBody = UnregisterRequestVo.builder()
                .ebid(Base64.encode(reqContent[0]))
                .epochId(currentEpoch)
                .time(Base64.encode(reqContent[1]))
                .mac(Base64.encode(reqContent[2])).build();

        this.requestEntity = new HttpEntity<>(this.requestBody, this.headers);

        ResponseEntity<UnregisterResponseDto> response = this.restTemplate.exchange(
                this.targetUrl.toString(),
                HttpMethod.POST, this.requestEntity, UnregisterResponseDto.class
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(this.registrationService, never()).findById(ArgumentMatchers.any());
        verify(this.registrationService, never()).delete(ArgumentMatchers.any());
    }

    @Test
    public void testBadTimeSizeFails() {
        byte[] idA = this.generateKey(5);
        byte[] kA = this.generateKA();

        byte[][] reqContent = createEBIDTimeMACFor(idA, kA, currentEpoch);

        requestBody = UnregisterRequestVo.builder()
                .ebid(Base64.encode(reqContent[0]))
                .epochId(currentEpoch)
                .time(Base64.encode("AB".getBytes()))
                .mac(Base64.encode(reqContent[2])).build();

        this.requestEntity = new HttpEntity<>(this.requestBody, this.headers);

        ResponseEntity<UnregisterResponseDto> response = this.restTemplate.exchange(
                this.targetUrl.toString(),
                HttpMethod.POST, this.requestEntity, UnregisterResponseDto.class
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(this.registrationService, never()).findById(ArgumentMatchers.any());
        verify(this.registrationService, never()).delete(ArgumentMatchers.any());
    }

    @Test
    public void testBadMACSizeFails() {
        byte[] idA = this.generateKey(5);
        byte[] kA = this.generateKA();

        byte[][] reqContent = createEBIDTimeMACFor(idA, kA, currentEpoch);

        requestBody = UnregisterRequestVo.builder()
                .ebid(Base64.encode(reqContent[0]))
                .epochId(currentEpoch)
                .time(Base64.encode(reqContent[1]))
                .mac(Base64.encode("ABC".getBytes())).build();

        this.requestEntity = new HttpEntity<>(this.requestBody, this.headers);

        ResponseEntity<UnregisterResponseDto> response = this.restTemplate.exchange(
                this.targetUrl.toString(),
                HttpMethod.POST, this.requestEntity, UnregisterResponseDto.class
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(this.registrationService, never()).findById(ArgumentMatchers.any());
        verify(this.registrationService, never()).delete(ArgumentMatchers.any());
    }

    @Test
    public void testBadMACFails() {

        // Given
        byte[] idA = this.generateKey(5);
        byte[] kA = this.generateKA();
        Registration reg = Registration.builder()
                .permanentIdentifier(idA)
                .atRisk(true)
                .isNotified(false)
                .lastStatusRequestEpoch(currentEpoch - 3).build();

        byte[][] reqContent = createEBIDTimeMACFor(idA, kA, currentEpoch);

        byte[] decryptedEbid = new byte[8];
        System.arraycopy(idA, 0, decryptedEbid, 3, idA.length);
        System.arraycopy(ByteUtils.intToBytes(currentEpoch), 1, decryptedEbid, 0, decryptedEbid.length - idA.length);

        doReturn(Optional.of(reg)).when(this.registrationService).findById(idA);

        doReturn(Optional.empty())
                .when(this.cryptoServerClient).getIdFromAuth(any());

        requestBody = UnregisterRequestVo.builder()
                .ebid(Base64.encode(reqContent[0]))
                .epochId(currentEpoch)
                .time(Base64.encode(reqContent[1]))
                .mac(Base64.encode(reqContent[2])).build();

        this.requestEntity = new HttpEntity<>(this.requestBody, this.headers);

        // When
        ResponseEntity<UnregisterResponseDto> response = this.restTemplate.exchange(
                this.targetUrl.toString(),
                HttpMethod.POST, this.requestEntity, UnregisterResponseDto.class
        );

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(this.cryptoServerClient, times(1)).getIdFromAuth(ArgumentMatchers.any());
        verify(this.cryptoServerClient, never()).deleteId(ArgumentMatchers.any());
        verify(this.registrationService, never()).findById(ArgumentMatchers.any());
        verify(this.registrationService, never()).delete(ArgumentMatchers.any());
    }

    public byte[] generateKA() {
        byte[] ka = null;

        try {
            KeyGenerator keyGen = KeyGenerator.getInstance("HmacSHA256");

            SecureRandom secRandom = new SecureRandom();
            keyGen.init(secRandom);
            Key key = keyGen.generateKey();
            ka = key.getEncoded();

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Problem generating KA", e);
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

        System.arraycopy(tsInSecondsB, 4, time, 0, time.length);

        return time;
    }

    private byte[] generateHMAC(final CryptoHMACSHA256 cryptoHMACSHA256S, final byte[] argument,
            final DigestSaltEnum salt)
            throws Exception {

        final byte[] prefix = new byte[] { salt.getValue() };

        // HMAC-SHA256 processing
        byte[] generatedSHA256 = cryptoHMACSHA256S.encrypt(ByteUtils.addAll(prefix, argument));

        return generatedSHA256;
    }

    private byte[] generateMACFor(byte[] ebid, byte[] time, byte[] ka) {
        byte[] agg = new byte[ebid.length + time.length];
        System.arraycopy(ebid, 0, agg, 0, ebid.length);
        System.arraycopy(time, 0, agg, ebid.length, time.length);

        byte[] mac = new byte[32];
        try {
            mac = this.generateHMAC(new CryptoHMACSHA256(ka), agg, DigestSaltEnum.UNREGISTER);
        } catch (Exception e) {
            throw new RuntimeException("Problem generating SHA256", e);
        }
        return mac;
    }

    private byte[][] createEBIDTimeMACFor(byte[] id, byte[] ka, int currentEpoch) {
        return this.createEBIDTimeMACFor(id, ka, currentEpoch, 0);
    }

    private byte[][] createEBIDTimeMACFor(byte[] id, byte[] ka, int currentEpoch, int adjustTimeBySeconds) {
        byte[][] res = new byte[3][];
        try {
            res[0] = this.cryptoService.generateEBID(
                    new CryptoSkinny64(this.serverKey),
                    currentEpoch, id
            );
            res[1] = this.generateTime32(adjustTimeBySeconds);
            res[2] = this.generateMACFor(res[0], res[1], ka);
        } catch (Exception e) {
            throw new RuntimeException("Problem creating EBID, Time and MAC for test", e);
        }
        return res;
    }

    @Test
    public void testUnregisterRequestSucceeds() {
        byte[] idA = this.generateKey(5);
        byte[] kA = this.generateKA();
        Registration reg = Registration.builder()
                .permanentIdentifier(idA)
                .atRisk(true)
                .isNotified(false)
                .lastStatusRequestEpoch(currentEpoch - 3).build();

        byte[][] reqContent = createEBIDTimeMACFor(idA, kA, currentEpoch);

        byte[] decryptedEbid = new byte[8];
        System.arraycopy(idA, 0, decryptedEbid, 3, idA.length);
        System.arraycopy(ByteUtils.intToBytes(currentEpoch), 1, decryptedEbid, 0, decryptedEbid.length - idA.length);

        doReturn(Optional.of(reg)).when(this.registrationService).findById(idA);

        doReturn(
                Optional.of(
                        GetIdFromAuthResponse.newBuilder()
                                .setIdA(ByteString.copyFrom(idA))
                                .build()
                )
        )
                .when(this.cryptoServerClient).getIdFromAuth(any());

        doReturn(
                Optional.of(
                        DeleteIdResponse.newBuilder()
                                .setIdA(ByteString.copyFrom(idA))
                                .build()
                )
        )
                .when(this.cryptoServerClient).deleteId(any());

        requestBody = UnregisterRequestVo.builder()
                .ebid(Base64.encode(reqContent[0]))
                .epochId(currentEpoch)
                .time(Base64.encode(reqContent[1]))
                .mac(Base64.encode(reqContent[2])).build();

        this.requestEntity = new HttpEntity<>(this.requestBody, this.headers);

        ResponseEntity<UnregisterResponseDto> response = this.restTemplate.exchange(
                this.targetUrl.toString(),
                HttpMethod.POST, this.requestEntity, UnregisterResponseDto.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().getSuccess());
        verify(this.cryptoServerClient, times(1)).deleteId(ArgumentMatchers.any());
        verify(this.registrationService, times(3)).findById(idA);
        verify(this.registrationService, times(1)).delete(ArgumentMatchers.any());

    }

    @Test
    public void testUnregisterRequestFailsCauseNoServerKey() {
        byte[] idA = this.generateKey(5);
        byte[] kA = this.generateKA();
        Registration reg = Registration.builder()
                .permanentIdentifier(idA)
                .atRisk(true)
                .isNotified(false)
                .lastStatusRequestEpoch(currentEpoch - 3).build();

        byte[][] reqContent = createEBIDTimeMACFor(idA, kA, currentEpoch);

        byte[] decryptedEbid = new byte[8];
        System.arraycopy(idA, 0, decryptedEbid, 3, idA.length);
        System.arraycopy(ByteUtils.intToBytes(currentEpoch), 1, decryptedEbid, 0, decryptedEbid.length - idA.length);

        doReturn(Optional.of(reg)).when(this.registrationService).findById(idA);

        doReturn(
                Optional.of(
                        DeleteIdResponse.newBuilder()
                                .setIdA(ByteString.copyFrom(idA))
                                .build()
                )
        )
                .when(this.cryptoServerClient).deleteId(any());

        requestBody = UnregisterRequestVo.builder()
                .ebid(Base64.encode(reqContent[0]))
                .epochId(currentEpoch)
                .time(Base64.encode(reqContent[1]))
                .mac(Base64.encode(reqContent[2])).build();

        this.requestEntity = new HttpEntity<>(this.requestBody, this.headers);

        doReturn(
                Optional.of(
                        GetIdFromAuthResponse.newBuilder()
                                .setError(
                                        ErrorMessage.newBuilder()
                                                .setCode(430)
                                                .setDescription("error")
                                                .build()
                                )
                                .build()
                )
        )
                .when(this.cryptoServerClient).getIdFromAuth(any());

        ResponseEntity<UnregisterResponseDto> response = this.restTemplate.exchange(
                this.targetUrl.toString(),
                HttpMethod.POST, this.requestEntity, UnregisterResponseDto.class
        );

        assertEquals(430, response.getStatusCodeValue());

    }

    @Test
    public void testUnregisterRequestNoSuchIdFails() {

        // Given
        byte[] idA = this.generateKey(5);
        byte[] kA = this.generateKA();

        byte[][] reqContent = createEBIDTimeMACFor(idA, kA, currentEpoch);
        byte[] decryptedEbid = new byte[8];
        System.arraycopy(idA, 0, decryptedEbid, 3, idA.length);
        System.arraycopy(ByteUtils.intToBytes(currentEpoch), 1, decryptedEbid, 0, decryptedEbid.length - idA.length);

        doReturn(
                Optional.of(
                        GetIdFromAuthResponse.newBuilder()
                                .setIdA(ByteString.copyFrom(idA))
                                .build()
                )
        )
                .when(this.cryptoServerClient).getIdFromAuth(any());

        doReturn(
                Optional.of(
                        DeleteIdResponse.newBuilder()
                                .setIdA(ByteString.copyFrom(idA))
                                .build()
                )
        )
                .when(this.cryptoServerClient).deleteId(any());

        doReturn(Optional.empty()).when(this.registrationService).findById(idA);

        requestBody = UnregisterRequestVo.builder()
                .ebid(Base64.encode(reqContent[0]))
                .epochId(currentEpoch)
                .time(Base64.encode(reqContent[1]))
                .mac(Base64.encode(reqContent[2])).build();

        this.requestEntity = new HttpEntity<>(this.requestBody, this.headers);

        // When
        ResponseEntity<UnregisterResponseDto> response = this.restTemplate.exchange(
                this.targetUrl.toString(),
                HttpMethod.POST, this.requestEntity, UnregisterResponseDto.class
        );

        // Then
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(this.cryptoServerClient, times(1)).getIdFromAuth(ArgumentMatchers.any());
        verify(this.cryptoServerClient, never()).deleteId(ArgumentMatchers.any());
        verify(this.registrationService, times(2)).findById(ArgumentMatchers.any());
        verify(this.registrationService, never()).delete(ArgumentMatchers.any());
    }

    @Test
    public void testUnregisterShouldCallPushServerWhenItSucceedsAndPushTokenIsProvidedAndNotEmpty() {

        // Given
        byte[] idA = this.generateKey(5);
        String pushToken = "pushToken";
        generateSuccessPayload(idA, pushToken);

        doReturn(
                Optional.of(
                        GetIdFromAuthResponse.newBuilder()
                                .setIdA(ByteString.copyFrom(idA))
                                .build()
                )
        )
                .when(this.cryptoServerClient).getIdFromAuth(any());

        // When
        ResponseEntity<UnregisterResponseDto> response = this.restTemplate.exchange(
                this.targetUrl.toString(),
                HttpMethod.POST, this.requestEntity, UnregisterResponseDto.class
        );

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().getSuccess());
        verify(this.cryptoServerClient, times(1)).deleteId(ArgumentMatchers.any());
        verify(this.registrationService, times(3)).findById(idA);
        verify(this.registrationService, times(1)).delete(ArgumentMatchers.any());
        verifyPushNotifServerReceivedUnregisterForToken(pushToken);
    }

    @Test
    public void testUnregisterShouldNotCallPushServerWhenItSucceedsAndPushTokenIsProvidedAndEmpty() {

        // Given
        byte[] idA = this.generateKey(5);
        String pushToken = "";
        generateSuccessPayload(idA, pushToken);

        doReturn(
                Optional.of(
                        GetIdFromAuthResponse.newBuilder()
                                .setIdA(ByteString.copyFrom(idA))
                                .build()
                )
        )
                .when(this.cryptoServerClient).getIdFromAuth(any());

        // When
        ResponseEntity<UnregisterResponseDto> response = this.restTemplate.exchange(
                this.targetUrl.toString(),
                HttpMethod.POST, this.requestEntity, UnregisterResponseDto.class
        );

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().getSuccess());
        verify(this.cryptoServerClient, times(1)).deleteId(ArgumentMatchers.any());
        verify(this.registrationService, times(3)).findById(idA);
        verify(this.registrationService, times(1)).delete(ArgumentMatchers.any());
        verifyNoInteractionsWithPushNotifServer();
    }

    private void generateSuccessPayload(byte[] idA, String pushToken) {
        byte[] kA = this.generateKA();
        Registration reg = Registration.builder()
                .permanentIdentifier(idA)
                .atRisk(true)
                .isNotified(false)
                .lastStatusRequestEpoch(currentEpoch - 3).build();

        byte[][] reqContent = createEBIDTimeMACFor(idA, kA, currentEpoch);

        byte[] decryptedEbid = new byte[8];
        System.arraycopy(idA, 0, decryptedEbid, 3, idA.length);
        System.arraycopy(ByteUtils.intToBytes(currentEpoch), 1, decryptedEbid, 0, decryptedEbid.length - idA.length);

        doReturn(Optional.of(reg)).when(this.registrationService).findById(idA);

        doReturn(
                Optional.of(
                        DeleteIdResponse.newBuilder()
                                .setIdA(ByteString.copyFrom(idA))
                                .build()
                )
        )
                .when(this.cryptoServerClient).deleteId(any());

        requestBody = UnregisterRequestVo.builder()
                .ebid(Base64.encode(reqContent[0]))
                .epochId(currentEpoch)
                .time(Base64.encode(reqContent[1]))
                .mac(Base64.encode(reqContent[2]))
                .pushToken(pushToken)
                .build();

        this.requestEntity = new HttpEntity<>(this.requestBody, this.headers);
    }
}
