package fr.gouv.stopc.robertserver.utils;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.VisibilityChecker;
import fr.gouv.stopc.robert.server.common.DigestSaltEnum;
import fr.gouv.stopc.robert.server.common.utils.ByteUtils;
import fr.gouv.stopc.robert.server.common.utils.TimeUtils;
import fr.gouv.stopc.robert.server.crypto.exception.RobertServerCryptoException;
import fr.gouv.stopc.robert.server.crypto.structure.impl.CryptoAESGCM;
import fr.gouv.stopc.robert.server.crypto.structure.impl.CryptoHMACSHA256;
import fr.gouv.tac.robert.model.*;
import io.restassured.RestAssured;
import io.restassured.config.ObjectMapperConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.path.json.mapper.factory.Jackson2ObjectMapperFactory;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.bson.internal.Base64;

import java.io.IOException;
import java.lang.reflect.Type;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static fr.gouv.stopc.robertserver.utils.EcdhUtils.deriveKeysFromBackendPublicKey;
import static fr.gouv.stopc.robertserver.utils.EcdhUtils.generateKeyPair;
import static io.restassured.RestAssured.given;

@Slf4j
public class AppMobileClient {


    /**
     * SEE TestHSM.class from hsm-tools project
     */
    final static String ROBERT_CRYPTO_SERVER_PUBLIC_KEY = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEwpDgJdE0aRop5uibRYqCOLK7CnZ+DAyQhGrVD6XYXC/5LeNtLwqSxPAVcswqtiZyPs68h2Y3KEQn2y2bRxRzQg==";

    KeyPair keyPair;

    byte countryCode = 0x21;

    @Getter
    List<Contact> contacts;

    @Getter
    List<EphemeralTupleJson> decodedTuples;

    @Getter
    Optional<ClientIdentifierBundle> clientIdentifierBundleWithPublicKey;

    public AppMobileClient() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, RobertServerCryptoException {
        this.keyPair = generateKeyPair();
        this.contacts = new ArrayList<>();
        generateKeyForTuples();
        RestAssured.config = RestAssuredConfig.config().objectMapperConfig(new ObjectMapperConfig().jackson2ObjectMapperFactory(
                new Jackson2ObjectMapperFactory() {
                    @Override
                    public ObjectMapper create(Type type, String s) {
                        ObjectMapper objectMapper = new ObjectMapper();
                        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
                        objectMapper.setVisibility(VisibilityChecker.Std.defaultInstance().withFieldVisibility(JsonAutoDetect.Visibility.ANY));
                        return objectMapper;
                    }
                }
        ));
    }

    public void addNewContact(Contact contactVo) {
        this.getContacts().add(contactVo);
    }

    public void exchangeEbIdWith(AppMobileClient otherAppMobile) {

//        List<HelloMessageDetailVo> myHelloMessageDetailVos = new ArrayList<>();
//
//        myHelloMessageDetailVos.add(generateHelloMessage(
//                decodedTuples.get(0).getKey().getEbid(),
//                decodedTuples.get(0).getKey().getEcc(),
//                TimeUtils.convertUnixMillistoNtpSeconds(System.currentTimeMillis())));
//
//        ContactVo myContactVo = ContactVo.builder()
//                .ebid(Base64.encode(decodedTuples.get(0).getKey().getEbid()))
//                .ecc(Base64.encode(decodedTuples.get(0).getKey().getEcc()))
//                .ids(myHelloMessageDetailVos)
//                .build();
//
//        otherAppMobile.addNewContact(myContactVo);

        List<HelloMessageDetail> otherHelloMessageDetailVos = new ArrayList<>();


        //TODO REPLACE THIS VALUE WITH CONFIGURATION
        otherHelloMessageDetailVos.add(generateHelloMessage(otherAppMobile,
                TimeUtils.convertUnixMillistoNtpSeconds(System.currentTimeMillis()), -7));

        otherHelloMessageDetailVos.add(generateHelloMessage(otherAppMobile,
                TimeUtils.convertUnixMillistoNtpSeconds(System.currentTimeMillis() + 100000), -5));

        otherHelloMessageDetailVos.add(generateHelloMessage(otherAppMobile,
                TimeUtils.convertUnixMillistoNtpSeconds(System.currentTimeMillis() + 102000), -5));

        otherHelloMessageDetailVos.add(generateHelloMessage(otherAppMobile,
                TimeUtils.convertUnixMillistoNtpSeconds(System.currentTimeMillis() + 150000), -5));

        otherHelloMessageDetailVos.add(generateHelloMessage(otherAppMobile,
                TimeUtils.convertUnixMillistoNtpSeconds(System.currentTimeMillis() + 180000), -5));

        otherHelloMessageDetailVos.add(generateHelloMessage(otherAppMobile,
                TimeUtils.convertUnixMillistoNtpSeconds(System.currentTimeMillis() + 200000), -5));

        otherHelloMessageDetailVos.add(generateHelloMessage(otherAppMobile,
                TimeUtils.convertUnixMillistoNtpSeconds(System.currentTimeMillis() + 210000), -5));

        otherHelloMessageDetailVos.add(generateHelloMessage(otherAppMobile,
                TimeUtils.convertUnixMillistoNtpSeconds(System.currentTimeMillis() + 215000), -67));

        otherHelloMessageDetailVos.add(generateHelloMessage(otherAppMobile,
                TimeUtils.convertUnixMillistoNtpSeconds(System.currentTimeMillis() + 310000), -5));

        otherHelloMessageDetailVos.add(generateHelloMessage(otherAppMobile,
                TimeUtils.convertUnixMillistoNtpSeconds(System.currentTimeMillis() + 410000), -5));

        otherHelloMessageDetailVos.add(generateHelloMessage(otherAppMobile,
                TimeUtils.convertUnixMillistoNtpSeconds(System.currentTimeMillis() + 450000), -7));

        otherHelloMessageDetailVos.add(generateHelloMessage(otherAppMobile,
                TimeUtils.convertUnixMillistoNtpSeconds(System.currentTimeMillis() + 510000), -7));

        otherHelloMessageDetailVos.add(generateHelloMessage(otherAppMobile,
                TimeUtils.convertUnixMillistoNtpSeconds(System.currentTimeMillis() + 550000), -7));

        otherHelloMessageDetailVos.add(generateHelloMessage(otherAppMobile,
                TimeUtils.convertUnixMillistoNtpSeconds(System.currentTimeMillis() + 750000), -7));

        otherHelloMessageDetailVos.add(generateHelloMessage(otherAppMobile,
                TimeUtils.convertUnixMillistoNtpSeconds(System.currentTimeMillis() + 710000), -7));


        Contact otherContact = new Contact();
        otherContact.setEcc(otherAppMobile.getDecodedTuples().get(0).getKey().getEcc());
        otherContact.setEbid(otherAppMobile.getDecodedTuples().get(0).getKey().getEbid());
        otherContact.setIds(otherHelloMessageDetailVos);

        addNewContact(otherContact);

    }

//    private HelloMessageDetailVo generateHelloMessageV2(AppMobileClient appMobileClient, long timeAsNtpSeconds, int timeHello, int rssiCalibrated) {
//        byte[] ebid = appMobileClient.getDecodedTuples().get(0).getKey().getEbid();
//        byte[] ecc = appMobileClient.getDecodedTuples().get(0).getKey().getEcc();
//
//        byte[] timeHelloB = new byte[4];
//        ByteUtils.intToBytes(timeHello);
//        // Clear out the first two bytes
//        timeHelloB[0] = (byte) (timeHelloB[0] & 0x00);
//        timeHelloB[1] = (byte) (timeHelloB[1] & 0x00);
//
//        return HelloMessageDetailVo.builder()
//                .timeCollectedOnDevice(timeAsNtpSeconds)
//                .timeFromHelloMessage(timeHello)
//                .mac(Base64.encode(appMobileClient.generateMACforHelloMessage(ebid,ecc, timeHelloB)))
//                .rssiCalibrated(rssiCalibrated)
//                .build();
//    }


    private HelloMessageDetail generateHelloMessage(AppMobileClient appMobileClient, long timeAsNtpSeconds, int rssiCalibrated) {

        byte[] ebid = appMobileClient.getDecodedTuples().get(0).getKey().getEbid();
        byte[] ecc = appMobileClient.getDecodedTuples().get(0).getKey().getEcc();

//        byte[] time = new byte[2];
//
//        // Get timestamp on sixteen bits
//        System.arraycopy(ByteUtils.longToBytes(timeAsNtpSeconds), 6, time, 0, 2);

        byte[] timeOfDevice = new byte[4];
        System.arraycopy(ByteUtils.longToBytes(timeAsNtpSeconds), 4, timeOfDevice, 0, 4);

        byte[] timeHelloB = new byte[4];
        System.arraycopy(ByteUtils.longToBytes(timeAsNtpSeconds), 4, timeHelloB, 0, 4);

        // Clear out the first two bytes
        timeHelloB[0] = (byte) (timeHelloB[0] & 0x00);
        timeHelloB[1] = (byte) (timeHelloB[1] & 0x00);

        int timeReceived = ByteUtils.bytesToInt(timeOfDevice);
        int timeHello = ByteUtils.bytesToInt(timeHelloB);

        final HelloMessageDetail helloMessageDetail = new HelloMessageDetail();

        helloMessageDetail.setTimeCollectedOnDevice(timeReceived);
        helloMessageDetail.setTimeFromHelloMessage(timeHello);
        helloMessageDetail.setMac(appMobileClient.generateMACforHelloMessage(ebid, ecc, timeHelloB));
        helloMessageDetail.setRssiCalibrated(rssiCalibrated);

        return helloMessageDetail;
    }

    public void register() {
        final RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setCaptcha("anything");
        registerRequest.setCaptchaId("anything1");
        registerRequest.setClientPublicECDHKey(getPublicKey());

        RegisterSuccessResponse registerResponseDto = submitRequest(registerRequest, RegisterSuccessResponse.class, RobertEndPointEnum.REGISTER);

        decryptRegisterResponse(registerResponseDto);


    }

    public void reports(String qrCode) {

        final ReportBatchRequest reportBatchRequest = new ReportBatchRequest();
        reportBatchRequest.setToken(qrCode);
        reportBatchRequest.setContacts(contacts);

        ReportBatchResponse responseDto = submitRequest(reportBatchRequest, ReportBatchResponse.class, RobertEndPointEnum.REPORT);

        log.info("ReportBatchResponseDto = {} " + responseDto.toString());

    }

    public void status() {

        AuthRequestDto authRequestDto = prepareAuthRequestDto(0, DigestSaltEnum.STATUS, 0);
        final ExposureStatusRequest statusRequest = new ExposureStatusRequest();
        statusRequest.setEbid(authRequestDto.getEbid());
        statusRequest.setEpochId(authRequestDto.getEpochId());
        statusRequest.setTime(authRequestDto.getTime());
        statusRequest.setMac(authRequestDto.getMac());

        ExposureStatusResponse exposureStatusResponse = submitRequest(statusRequest, ExposureStatusResponse.class, RobertEndPointEnum.STATUS);
        log.info("exposureStatusResponse = {} " + exposureStatusResponse.toString());
    }

    public void deleteHistory() {

        AuthRequestDto authRequestDto = prepareAuthRequestDto(0, DigestSaltEnum.DELETE_HISTORY, 0);

        AuthentifiedRequest deleteHistoryRequest = new AuthentifiedRequest();

        deleteHistoryRequest.setEbid(authRequestDto.getEbid());
        deleteHistoryRequest.setEpochId(authRequestDto.getEpochId());
        deleteHistoryRequest.setTime(authRequestDto.getTime());
        deleteHistoryRequest.setMac(authRequestDto.getMac());

        //TODO: UnregisterResponse currently matches deleteHistoryRequest response in openapi definition:
        // -> replace UnregisterResponse by actual reponse type matching request type
        UnregisterResponse deleteHistoryResponseDto = submitRequest(deleteHistoryRequest, UnregisterResponse.class, RobertEndPointEnum.DELETE_EXPOSURE_HISTORY);

        log.info("deleteHistoryResponseDto = {} " + deleteHistoryResponseDto.toString());
    }

    public void unregister() {
        AuthRequestDto authRequestDto = prepareAuthRequestDto(0, DigestSaltEnum.UNREGISTER, 0);

        final UnregisterRequest unregisterRequest = new UnregisterRequest();
        unregisterRequest.setEbid(authRequestDto.getEbid());
        unregisterRequest.setEpochId(authRequestDto.getEpochId());
        unregisterRequest.setTime(authRequestDto.getTime());
        unregisterRequest.setMac(authRequestDto.getMac());

        UnregisterResponse unregisterResponseDto = submitRequest(unregisterRequest, UnregisterResponse.class, RobertEndPointEnum.UNREGISTER);

        log.info("unregisterResponseDto = {} " + unregisterResponseDto.toString());
    }

    private <T, Q> T submitRequest(Q request, Class<T> clazz, RobertEndPointEnum endPointEnum) {

        RestAssured.baseURI = "http://localhost:8086/api/" + endPointEnum.getApiVersion();

        return given()
                .when()
                .header("Content-Type", "application/json")
                .body(request)
                .post(endPointEnum.getEndpoint())
                .then()
                .statusCode(endPointEnum.getOkStatusCode())
                .extract()
                .body().as(clazz);
    }

    private AuthRequestDto prepareAuthRequestDto(int adjustTimeInSeconds, DigestSaltEnum saltEnum, int tupleIndex) {

        byte[] ebid = getDecodedTuples().get(tupleIndex).getKey().getEbid();
        int epochId = getDecodedTuples().get(tupleIndex).getEpochId();
        byte[] time = generateTime32(System.currentTimeMillis(), adjustTimeInSeconds);
        byte[] mac = generateMAC(ebid, epochId, time, saltEnum);

        return AuthRequestDto.builder()
                .ebid(ebid)
                .epochId(epochId)
                .time(time)
                .mac(mac).build();
    }

    private void generateKeyForTuples() throws RobertServerCryptoException {
        this.clientIdentifierBundleWithPublicKey = deriveKeysFromBackendPublicKey(java.util.Base64.getDecoder().decode(ROBERT_CRYPTO_SERVER_PUBLIC_KEY), keyPair);
    }

    private String getPublicKey() {
        return Base64.encode(keyPair.getPublic().getEncoded());
    }

    private void decryptRegisterResponse(RegisterSuccessResponse registerData) {

        CryptoAESGCM aesGcm = new CryptoAESGCM(clientIdentifierBundleWithPublicKey.get().getKeyForTuples());
        try {
            byte[] decryptedTuples = aesGcm.decrypt(registerData.getTuples());
            ObjectMapper objectMapper = new ObjectMapper();
            this.decodedTuples = objectMapper.readValue(
                    decryptedTuples,
                    new TypeReference<List<EphemeralTupleJson>>() {
                    });

        } catch (IOException | RobertServerCryptoException e) {
            log.error(e.getMessage(), e);
        }

    }

    private byte[] generateTime32(long unixTimeInMillis, int adjustTimeInSeconds) {
        long tsInSeconds = TimeUtils.convertUnixMillistoNtpSeconds(unixTimeInMillis);
        tsInSeconds += adjustTimeInSeconds;
        byte[] tsInSecondsB = ByteUtils.longToBytes(tsInSeconds);
        byte[] time = new byte[4];

        System.arraycopy(tsInSecondsB, 4, time, 0, 4);

        return time;
    }

    private byte[] generateMAC(byte[] ebid, int epochId, byte[] time, DigestSaltEnum saltEnum) {
        // Merge arrays
        // HMAC-256
        // return hash
        byte[] agg = new byte[8 + 4 + 4];
        System.arraycopy(ebid, 0, agg, 0, ebid.length);
        System.arraycopy(ByteUtils.intToBytes(epochId), 0, agg, ebid.length, Integer.BYTES);
        System.arraycopy(time, 0, agg, ebid.length + Integer.BYTES, time.length);

        byte[] mac = new byte[32];
        try {
            mac = this.generateHMAC(new CryptoHMACSHA256(getClientIdentifierBundleWithPublicKey().get().getKeyForMac()), agg, saltEnum);
        } catch (Exception e) {
            log.info("Problem generating SHA256");
        }
        return mac;
    }

    private byte[] generateHMAC(final CryptoHMACSHA256 cryptoHMACSHA256S, final byte[] argument, final DigestSaltEnum salt)
            throws Exception {

        final byte[] prefix = new byte[]{salt.getValue()};

        // HMAC-SHA256 processing
        return cryptoHMACSHA256S.encrypt(ByteUtils.addAll(prefix, argument));
    }


    public byte[] generateMACforHelloMessage(byte[] ebid, byte[] ecc, byte[] timeHelloMessage) {
        // TODO A FINIR
        // Merge arrays
        // HMAC-256
        // return hash
        byte[] mai = new byte[ebid.length + ecc.length + 2];
        System.arraycopy(ecc, 0, mai, 0, ecc.length);
        System.arraycopy(ebid, 0, mai, ecc.length, ebid.length);
        //take into account the 2 last bytes
        System.arraycopy(timeHelloMessage, 2, mai, ecc.length + ebid.length, 2);


        byte[] encryptedMac = new byte[32];
        try {
            encryptedMac = this.generateHMAC(new CryptoHMACSHA256(getClientIdentifierBundleWithPublicKey().get().getKeyForMac()), mai, DigestSaltEnum.HELLO);
        } catch (Exception e) {
            log.info("Problem generating SHA256");
        }

        // truncate the result from 0 to 40-bits
        return Arrays.copyOfRange(encryptedMac, 0, 5);

    }

}
