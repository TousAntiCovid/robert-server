package fr.gouv.stopc.e2e.appmobile;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.gouv.stopc.e2e.appmobile.model.*;
import fr.gouv.stopc.e2e.config.ApplicationProperties;
import fr.gouv.stopc.e2e.external.common.enums.DigestSaltEnum;
import fr.gouv.stopc.e2e.external.common.utils.ByteUtils;
import fr.gouv.stopc.e2e.external.crypto.CryptoAESGCM;
import fr.gouv.stopc.e2e.external.crypto.exception.RobertServerCryptoException;
import fr.gouv.stopc.e2e.external.crypto.model.EphemeralTupleJson;
import fr.gouv.stopc.e2e.robert.model.*;
import io.restassured.specification.RequestSpecification;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

import static fr.gouv.stopc.e2e.external.common.enums.DigestSaltEnum.HELLO;
import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static java.util.stream.Collectors.toMap;
import static org.hamcrest.Matchers.equalTo;

@Slf4j
public class AppMobile {

    private final ApplicationProperties applicationProperties;

    private final ClientKeys clientKeys;

    private final Map<Integer, ContactTuple> contactTupleByEpochId = new HashMap<>();

    private final Map<ContactTuple, Contact> receivedHelloMessages = new HashMap<>();

    private final EpochClock clock;

    private ExposureStatusResponse lastExposureStatusResponse;

    public AppMobile(ApplicationProperties applicationProperties) {
        this.applicationProperties = applicationProperties;
        this.clientKeys = ClientKeys.builder(applicationProperties.getCryptoPublicKey())
                .build();

        final var captchaSolution = resolveMockedCaptchaChallenge();
        final var registerResponse = register(captchaSolution.getId(), captchaSolution.getAnswer());
        this.clock = new EpochClock(registerResponse.getTimeStart());
    }

    private RequestSpecification givenRobertBaseUri() {
        return given()
                .baseUri(applicationProperties.getWsRestBaseUrl())
                .contentType(JSON);
    }

    private CaptchaSolution resolveMockedCaptchaChallenge() {
        // We generate a fake Captcha id which we will be used to differentiate mobile
        // applications
        final var captchaId = RandomStringUtils.random(7, true, false);

        // We simulate the user visual captcha resolution and we call the robert
        // endpoint

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

        // The user reads the image content
        return new CaptchaSolution(captchaId, "ABCD");
    }

    private RegisterSuccessResponse register(String captchaId, String captchaSolution) {
        final var publicKey = Base64.getEncoder()
                .encodeToString(clientKeys.getKeyPair().getPublic().getEncoded());

        final var registerResponse = givenRobertBaseUri()
                .body(
                        RegisterRequest.builder()
                                .captcha(captchaSolution)
                                .captchaId(captchaId)
                                .clientPublicECDHKey(publicKey)
                                .pushInfo(
                                        PushInfo.builder()
                                                .token("string")
                                                .locale("fr")
                                                .timezone("Europe/Paris")
                                                .build()
                                )
                                .build()
                )
                .when()
                .post("/api/v6/register")
                .then()
                .extract()
                .as(RegisterSuccessResponse.class);

        updateTuples(registerResponse.getTuples());

        return registerResponse;
    }

    public void updateTuples(final byte[] encryptedTuples) {
        final var aesGcm = new CryptoAESGCM(clientKeys.getKeyForTuples());
        try {
            final var tuples = new ObjectMapper()
                    .readValue(aesGcm.decrypt(encryptedTuples), EphemeralTupleJson[].class);
            final var tuplesByEpochId = Arrays.stream(tuples)
                    .collect(
                            toMap(
                                    EphemeralTupleJson::getEpochId,
                                    tuple -> new ContactTuple(tuple.getKey().getEbid(), tuple.getKey().getEcc())
                            )
                    );
            contactTupleByEpochId.putAll(tuplesByEpochId);
        } catch (RobertServerCryptoException | IOException e) {
            throw new RuntimeException("Error during /register procedure", e);
        }
    }

    public void exchangeHelloMessagesWith(final AppMobile otherMobileApp, final Instant startInstant,
            final Duration exchangeDuration) {
        final var endDate = startInstant.plus(exchangeDuration);

        Stream.iterate(startInstant, d -> d.isBefore(endDate), d -> d.plusSeconds(10))
                .map(e -> produceHelloMessage(e, HELLO))
                .forEach(otherMobileApp::receiveHelloMessage);

        Stream.iterate(startInstant, d -> d.isBefore(endDate), d -> d.plusSeconds(10))
                .map(e -> otherMobileApp.produceHelloMessage(e, HELLO))
                .forEach(this::receiveHelloMessage);
    }

    private void receiveHelloMessage(HelloMessage helloMessage) {
        final var randomRssiCalibrated = ThreadLocalRandom.current().nextInt(-10, 3);
        final var time = clock.at(helloMessage.getTime());
        final var contact = receivedHelloMessages.computeIfAbsent(
                helloMessage.getContactTuple(), tuple -> Contact.builder()
                        .ebid(tuple.getEbid())
                        .ecc(tuple.getEcc())
                        .ids(new ArrayList<>())
                        .build()
        );
        contact.addIdsItem(
                HelloMessageDetail.builder()
                        .rssiCalibrated(randomRssiCalibrated)
                        .timeCollectedOnDevice(time.asNtpTimestamp())
                        .timeFromHelloMessage(ByteUtils.bytesToInt(helloMessage.getEncodedTime()))
                        .mac(helloMessage.getMac())
                        .build()
        );
    }

    public void reportContacts() {
        given()
                .contentType(JSON)
                .body(
                        ReportBatchRequest.builder()
                                .token("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")
                                .contacts(new ArrayList<>(receivedHelloMessages.values()))
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

    private HelloMessage produceHelloMessage(Instant helloMessageTime, final DigestSaltEnum saltEnum) {
        final var epochId = clock.at(helloMessageTime).getEpochId();
        final var tuple = contactTupleByEpochId.get(epochId);
        return HelloMessage.builder(saltEnum, clientKeys.getKeyForMac())
                .ebid(tuple.getEbid())
                .ecc(tuple.getEcc())
                .time(helloMessageTime)
                .build();
    }

    public void requestStatus() {
        var inst = Instant.now();
        final var epochId = clock.at(inst).getEpochId();
        final var tuple = contactTupleByEpochId.get(epochId);
        StatusRequest newStatusRequest = StatusRequest
                .builder(inst, DigestSaltEnum.STATUS, clientKeys.getKeyForMac(), epochId)
                .ebid(tuple.getEbid())
                .build();

        lastExposureStatusResponse = given()
                .contentType(JSON)
                .body(newStatusRequest)
                .when()
                .post(this.applicationProperties.getWsRestBaseUrl().concat("/api/v6/status"))
                .then()
                .statusCode(200)
                .contentType(JSON)
                .extract()
                .as(ExposureStatusResponse.class);

        updateTuples(lastExposureStatusResponse.getTuples());
    }

    public int getRiskLevel() {
        var status = 0;
        if (null != lastExposureStatusResponse && null != lastExposureStatusResponse.getRiskLevel()) {
            status = lastExposureStatusResponse.getRiskLevel();
        }
        return status;
    }

}
