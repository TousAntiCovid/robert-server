package fr.gouv.stopc.e2e.appmobile;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.gouv.stopc.e2e.appmobile.model.ClientIdentifierBundle;
import fr.gouv.stopc.e2e.config.ApplicationProperties;
import fr.gouv.stopc.e2e.external.common.utils.TimeUtils;
import fr.gouv.stopc.e2e.external.crypto.CryptoAESGCM;
import fr.gouv.stopc.e2e.external.crypto.CryptoHMACSHA256;
import fr.gouv.stopc.e2e.external.crypto.model.EphemeralTupleJson;
import fr.gouv.stopc.e2e.robert.model.*;
import io.restassured.specification.RequestSpecification;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;

import java.security.KeyPair;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static fr.gouv.stopc.e2e.appmobile.model.EcdhUtils.deriveKeysFromBackendPublicKey;
import static fr.gouv.stopc.e2e.appmobile.model.EcdhUtils.generateKeyPair;
import static fr.gouv.stopc.e2e.external.common.enums.DigestSaltEnum.HELLO;
import static fr.gouv.stopc.e2e.external.common.utils.ByteUtils.*;
import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static java.util.Arrays.copyOfRange;
import static java.util.Base64.getEncoder;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@Slf4j
public class AppMobile {

    private static final String CAPTCHA_BYPASS_SOLUTION = "IEDX";

    private final ApplicationProperties applicationProperties;

    private final KeyPair keyPair;

    private long timeStartInNtpSeconds;

    List<Contact> contacts = new ArrayList<>();

    private List<EphemeralTupleJson> decodedTuples;

    private ClientIdentifierBundle clientIdentifierBundleWithPublicKey;

    public AppMobile(ApplicationProperties applicationProperties) {
        this.applicationProperties = applicationProperties;
        this.keyPair = generateKeyPair();
        generateKeyForTuples();
        requestCaptchaChallenge();
        resolveCaptchaAndRegister();
    }

    private RequestSpecification givenRobertBaseUri() {
        return given()
                .baseUri(applicationProperties.getWsRestBaseUrl())
                .contentType(JSON);
    }

    private void requestCaptchaChallenge() {
        // The mobile application ask a captcha id challenge from the captcha server
        var captchaChallengeId = givenRobertBaseUri()
                .body(
                        CaptchaGenerationRequest.builder()
                                .locale("fr")
                                .type("IMAGE")
                                .build()
                )
                .when()
                .post("/api/v6/captcha")
                .then()
                .statusCode(200)
                .contentType(JSON)
                .body("captchaId", notNullValue())
                .extract()
                .path("captchaId");

        // The mobile application ask for an image captcha with the received captcha id
        // challenge
        givenRobertBaseUri()
                .when()
                .get("/api/v6/captcha/{captchaId}/image", captchaChallengeId)
                .then()
                .statusCode(200)
                .contentType("image/png");
    }

    private void resolveCaptchaAndRegister() {
        // We generate a fake Captcha id which we will use to differentiate mobile
        // applications
        final var captchaId = RandomStringUtils.random(7, true, false);

        // We simulate the user visual captcha resolution and we call the robert
        // endpoint
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
                .contentType(JSON)
                .extract()
                .as(RegisterSuccessResponse.class);

        decryptRegisterResponse(registerSuccessResponse);
    }

    public void generateContactsWithOtherApps(final AppMobile otherMobileApp,
            final Instant startDate,
            final int durationOfExchangeInMin) {
        var numberOfContacts = contacts.size();
        generateHelloMessageDuring(otherMobileApp, startDate, durationOfExchangeInMin);
        assertThat(contacts.size()).as("There is more contact(s) in the contact list.")
                .isGreaterThan(numberOfContacts);
    }

    public void reportContacts() {
        given()
                .contentType(JSON)
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
                .contentType(JSON)
                .body("success", equalTo(true))
                .body("message", equalTo("Successful operation"));
    }

    private void decryptRegisterResponse(final RegisterSuccessResponse registerData) {
        timeStartInNtpSeconds = registerData.getTimeStart();
        updateTuples(registerData.getTuples());
    }

    @SneakyThrows
    private void updateTuples(final byte[] encryptedTuples) {
        var aesGcm = new CryptoAESGCM(clientIdentifierBundleWithPublicKey.getKeyForTuples());

        decodedTuples = new ObjectMapper().readValue(
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
        return getEncoder().encodeToString(keyPair.getPublic().getEncoded());
    }

    @SneakyThrows
    private byte[] generateHMAC(final CryptoHMACSHA256 cryptoHMACSHA256S, final byte[] argument) {
        return cryptoHMACSHA256S.encrypt(addAll(new byte[] { HELLO.getValue() }, argument));
    }

    private EphemeralTupleJson getTupleByDateInUnixMillis(final long timeInUnixMillis) {
        return decodedTuples.stream()
                .filter(e -> e.getEpochId() == TimeUtils.getCurrentEpochFromTo(timeStartInNtpSeconds, timeInUnixMillis))
                .findFirst()
                .orElseThrow();
    }

    private HelloMessageDetail generateHelloMessage(final AppMobile appMobile,
            final long timeAsUnixMilli,
            final int rssiCalibrated) {

        var ebid = appMobile.getTupleByDateInUnixMillis(timeAsUnixMilli).getKey().getEbid();
        var ecc = appMobile.getTupleByDateInUnixMillis(timeAsUnixMilli).getKey().getEcc();

        var timeAsNtpSeconds = TimeUtils.convertUnixMillistoNtpSeconds(timeAsUnixMilli);

        byte[] timeHello = new byte[4];
        System.arraycopy(longToBytes(timeAsNtpSeconds), 4, timeHello, 0, 4);

        // Clear out the first two bytes
        timeHello[0] = (byte) (timeHello[0] & 0x00);
        timeHello[1] = (byte) (timeHello[1] & 0x00);

        return HelloMessageDetail.builder()
                .timeCollectedOnDevice(timeAsNtpSeconds)
                .timeFromHelloMessage(bytesToInt(timeHello))
                .mac(appMobile.generateMACforHelloMessage(ebid, ecc, timeHello))
                .rssiCalibrated(rssiCalibrated)
                .build();
    }

    @SneakyThrows
    private byte[] generateMACforHelloMessage(final byte[] ebid,
            final byte[] ecc,
            final byte[] timeHelloMessage) {
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

        // Truncate the result from 0 to 40-bits
        return copyOfRange(encryptedMac, 0, 5);
    }

    private void exchangeEbIdWithRand(final AppMobile otherMobileApp, final long timeInUnixMillis) {
        var helloMessageDetail = generateHelloMessage(
                otherMobileApp,
                timeInUnixMillis,
                ThreadLocalRandom.current().nextInt(-10, 3)
        );
        getOrCreateContact(otherMobileApp.getTupleByDateInUnixMillis(timeInUnixMillis).getKey())
                .addIdsItem(helloMessageDetail);
    }

    private Contact getOrCreateContact(final EphemeralTupleJson.EphemeralTupleEbidEccJson tuple) {
        return contacts.stream()
                .filter(c -> c.getEbid() == tuple.getEbid())
                .filter(c -> c.getEcc() == tuple.getEcc())
                .findFirst()
                .orElseGet(() -> createAndAddNewContact(tuple));
    }

    private Contact createAndAddNewContact(final EphemeralTupleJson.EphemeralTupleEbidEccJson tuple) {
        var newContact = Contact.builder()
                .ebid(tuple.getEbid())
                .ecc(tuple.getEcc())
                .ids(new ArrayList<>())
                .build();
        contacts.add(newContact);
        return newContact;
    }

    private void generateHelloMessageDuring(final AppMobile otherMobileApp,
            final Instant startDate,
            final Integer durationOfExchangeInMin) {

        long initialTime = startDate.toEpochMilli();
        var durationOfExchangeInStepsOfTenSec = durationOfExchangeInMin * 6;

        log.info("Start of exchange : {}", DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochMilli(initialTime)));
        for (var i = 0; i < durationOfExchangeInStepsOfTenSec; i++) {
            // NOTE : In theory, we need an exchange of 40 minutes and send 1 message per
            // second
            initialTime += 10000; // steps of 10 seconds
            exchangeEbIdWithRand(otherMobileApp, initialTime);
        }
        log.info("End of exchange : {}", DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochMilli(initialTime)));
    }

}
