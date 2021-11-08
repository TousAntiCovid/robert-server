package fr.gouv.stopc.e2e.appmobile;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.gouv.stopc.e2e.appmobile.model.ClientIdentifierBundle;
import fr.gouv.stopc.e2e.config.ApplicationProperties;
import fr.gouv.stopc.e2e.external.common.utils.ByteUtils;
import fr.gouv.stopc.e2e.external.common.utils.TimeUtils;
import fr.gouv.stopc.e2e.external.crypto.CryptoAESGCM;
import fr.gouv.stopc.e2e.external.crypto.CryptoHMACSHA256;
import fr.gouv.stopc.e2e.external.crypto.model.EphemeralTupleJson;
import fr.gouv.stopc.e2e.robert.model.*;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;

import javax.validation.constraints.NotNull;

import java.security.KeyPair;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import static fr.gouv.stopc.e2e.appmobile.model.EcdhUtils.deriveKeysFromBackendPublicKey;
import static fr.gouv.stopc.e2e.appmobile.model.EcdhUtils.generateKeyPair;
import static fr.gouv.stopc.e2e.external.common.enums.DigestSaltEnum.HELLO;
import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.hamcrest.Matchers.equalTo;

@Slf4j
public class AppMobile {

    public static final String CAPTCHA_BYPASS_SOLUTION = "IEDX";

    private final ApplicationProperties applicationProperties;

    private final KeyPair keyPair;

    private String captchaId;

    private long timestart;

    List<Contact> contacts = new ArrayList<>();

    private List<EphemeralTupleJson> decodedTuples;

    private ClientIdentifierBundle clientIdentifierBundleWithPublicKey;

    @SneakyThrows
    public AppMobile(ApplicationProperties applicationProperties) {
        this.applicationProperties = applicationProperties;
        this.keyPair = generateKeyPair();
        generateKeyForTuples();
        resolveCaptcha();
        register();
    }

    private RequestSpecification givenRobertBaseUri() {
        return given()
                .baseUri(applicationProperties.getWsRestBaseUrl())
                .contentType(JSON);
    }

    private void resolveCaptcha() {
        givenRobertBaseUri()
                .body(
                        CaptchaGenerationRequest.builder()
                                .locale("fr")
                                .type("IMAGE")
                                .build()
                )
                .when()
                .post("/api/v6/captcha")
                .then()
                .statusCode(200);

        // We generate a fake Captcha id which we will use to differentiate mobile apps
        captchaId = RandomStringUtils.random(7, true, false);

        // We simulate captcha resolution and we call the robert endpoint
        givenRobertBaseUri()
                .when()
                .get("/api/v6/captcha/{captchaId}/image", captchaId)
                .then()
                .statusCode(200);
    }

    private void register() {
        var register = RegisterRequest.builder()
                .captcha(CAPTCHA_BYPASS_SOLUTION)
                .captchaId(captchaId)
                .clientPublicECDHKey(getPublicKey())
                .pushInfo(
                        PushInfo.builder()
                                .token("string")
                                .locale("fr")
                                .timezone("Europe/Paris")
                                .build()
                )
                .build();

        var registerSuccessResponse = given()
                .contentType(JSON)
                .body(register)
                .when()
                .post(applicationProperties.getWsRestBaseUrl().concat("/api/v6/register"))
                .then()
                .statusCode(201)
                .extract()
                .as(RegisterSuccessResponse.class);

        decryptRegisterResponse(registerSuccessResponse);
    }

    public void generateContactsWithOtherApps(List<AppMobile> otherApps, Instant startDate,
            int durationOfExchangeInMin) {
        int numberOfContacts = contacts.size();
        generateHelloMessageDuring(otherApps, startDate, durationOfExchangeInMin);
        assertThat(contacts.size()).as("There is one more contact in the contact list.")
                .isEqualTo(numberOfContacts + 1);
    }

    public void reportContacts() {
        given()
                .contentType(ContentType.JSON)
                .body(
                        ReportBatchRequest.builder()
                                .token("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")
                                .contacts(contacts)
                                .build()
                )
                .when()
                .post(
                        applicationProperties.getWsRestBaseUrl()
                                .concat("/api/v6/report")
                )
                .then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("message", equalTo("Successful operation"));
    }

    private void decryptRegisterResponse(@NotNull RegisterSuccessResponse registerData) {
        timestart = registerData.getTimeStart();
        updateTuples(registerData.getTuples());
    }

    @SneakyThrows
    private void updateTuples(byte[] encryptedTuples) {
        var aesGcm = new CryptoAESGCM(clientIdentifierBundleWithPublicKey.getKeyForTuples());

        this.decodedTuples = new ObjectMapper().readValue(
                aesGcm.decrypt(encryptedTuples),
                new TypeReference<>() {
                }
        );
    }

    @SneakyThrows
    private void generateKeyForTuples() {
        deriveKeysFromBackendPublicKey(
                Base64.getDecoder().decode(applicationProperties.getCryptoPublicKey()), keyPair
        )
                .ifPresent(clientIdentifierBundle -> clientIdentifierBundleWithPublicKey = clientIdentifierBundle);
    }

    private String getPublicKey() {
        return Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
    }

    private EphemeralTupleJson getCurrentTuple() {
        var currentEpoch = TimeUtils.getCurrentEpochFrom(timestart);

        var tuple = decodedTuples.stream()
                .filter(e -> e.getEpochId() == currentEpoch)
                .collect(Collectors.toList())
                .get(0);

        return tuple;
    }

    @SneakyThrows
    private byte[] generateHMAC(final CryptoHMACSHA256 cryptoHMACSHA256S, final byte[] argument) {

        return cryptoHMACSHA256S.encrypt(ByteUtils.addAll(new byte[] { HELLO.getValue() }, argument));
    }

    private HelloMessageDetail generateHelloMessage(AppMobile appMobile, long timeAsNtpSeconds, int rssiCalibrated) {

        byte[] ebid = appMobile.getCurrentTuple().getKey().getEbid();
        byte[] ecc = appMobile.getCurrentTuple().getKey().getEcc();

        byte[] timeOfDevice = new byte[4];
        System.arraycopy(ByteUtils.longToBytes(timeAsNtpSeconds), 4, timeOfDevice, 0, 4);

        byte[] timeHelloB = new byte[4];
        System.arraycopy(ByteUtils.longToBytes(timeAsNtpSeconds), 4, timeHelloB, 0, 4);

        // Clear out the first two bytes
        timeHelloB[0] = (byte) (timeHelloB[0] & 0x00);
        timeHelloB[1] = (byte) (timeHelloB[1] & 0x00);

        return HelloMessageDetail.builder()
                .timeCollectedOnDevice(Long.valueOf(ByteUtils.bytesToInt(timeOfDevice)))
                .timeFromHelloMessage(ByteUtils.bytesToInt(timeHelloB))
                .mac(appMobile.generateMACforHelloMessage(ebid, ecc, timeHelloB))
                .rssiCalibrated(rssiCalibrated)
                .build();
    }

    @SneakyThrows
    private byte[] generateMACforHelloMessage(byte[] ebid, byte[] ecc, byte[] timeHelloMessage) {
        // Merge arrays
        // HMAC-256
        // return hash
        var mai = new byte[ebid.length + ecc.length + 2];
        System.arraycopy(ecc, 0, mai, 0, ecc.length);
        System.arraycopy(ebid, 0, mai, ecc.length, ebid.length);
        // take into account the 2 last bytes
        System.arraycopy(timeHelloMessage, 2, mai, ecc.length + ebid.length, 2);

        var encryptedMac = generateHMAC(
                new CryptoHMACSHA256(clientIdentifierBundleWithPublicKey.getKeyForMac()), mai
        );

        // truncate the result from 0 to 40-bits
        return Arrays.copyOfRange(encryptedMac, 0, 5);
    }

    private void exchangeEbIdWithRand(List<AppMobile> otherAppMobileList, long timeInMillis) {

        for (AppMobile otherAppMobile : otherAppMobileList) {

            var helloMessageDetail = generateHelloMessage(
                    otherAppMobile,
                    TimeUtils.convertUnixMillistoNtpSeconds(timeInMillis),
                    ThreadLocalRandom.current().nextInt(-10, 3)
            );

            getOrCreateContact(otherAppMobile.getCurrentTuple().getKey()).addIdsItem(helloMessageDetail);
        }
    }

    private Contact getOrCreateContact(EphemeralTupleJson.EphemeralTupleEbidEccJson tuple) {
        var contact = contacts.stream()
                .filter(c -> c.getEbid() == tuple.getEbid())
                .filter(c -> c.getEcc() == tuple.getEcc())
                .findFirst();

        if (contact.isPresent()) {
            return contact.get();
        } else {
            var newContact = Contact.builder()
                    .ebid(tuple.getEbid())
                    .ecc(tuple.getEcc())
                    .ids(new ArrayList<>())
                    .build();
            contacts.add(newContact);
            return newContact;
        }
    }

    private void generateHelloMessageDuring(List<AppMobile> otherMobileApps,
            Instant startDate,
            Integer durationOfExchangeInMin) {

        var initialTime = startDate.toEpochMilli();

        durationOfExchangeInMin = durationOfExchangeInMin * 60;
        for (var i = 0; i < durationOfExchangeInMin; i++) {
            // On ajoute une minute à la date d'échange
            // NOTE : En théorie il faut à peu près 40 min de contact et 1 message par
            // seconde
            // Mais en configurant scoringThreshold = "0.0001"
            // cela devrait déclencher le calcul comme "risque" dès le premier échange.
            initialTime += 1000; // 1 seconde

            // On crée les helloMessages
            exchangeEbIdWithRand(otherMobileApps, initialTime);
        }
    }

}
