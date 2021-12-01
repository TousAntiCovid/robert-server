package fr.gouv.stopc.e2e.appmobile;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.gouv.stopc.e2e.appmobile.model.*;
import fr.gouv.stopc.e2e.config.ApplicationProperties;
import fr.gouv.stopc.e2e.external.common.utils.ByteUtils;
import fr.gouv.stopc.e2e.external.crypto.CryptoAESGCM;
import fr.gouv.stopc.e2e.external.crypto.exception.RobertServerCryptoException;
import fr.gouv.stopc.e2e.external.crypto.model.EphemeralTupleJson;
import fr.gouv.stopc.robert.client.api.CaptchaApi;
import fr.gouv.stopc.robert.client.api.DefaultApi;
import fr.gouv.stopc.robert.client.model.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.hamcrest.MatcherAssert;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import static fr.gouv.stopc.e2e.external.common.enums.DigestSaltEnum.HELLO;
import static java.util.stream.Collectors.toMap;
import static org.apache.http.HttpHeaders.CONTENT_TYPE;
import static org.hamcrest.Matchers.*;

@Slf4j
public class AppMobile {

    private final String username;

    private final ApplicationProperties applicationProperties;

    private final ClientKeys clientKeys;

    private final Map<Integer, ContactTuple> contactTupleByEpochId = new HashMap<>();

    private final Map<ContactTuple, Contact> receivedHelloMessages = new HashMap<>();

    private final EpochClock clock;

    private final CaptchaApi captchaApi;

    private final DefaultApi robertApi;

    @Getter
    private ExposureStatusResponse lastExposureStatusResponse;

    public AppMobile(String username, ApplicationProperties applicationProperties, CaptchaApi captchaApi,
            DefaultApi robertApi) {
        this.username = username;
        this.applicationProperties = applicationProperties;
        this.captchaApi = captchaApi;
        this.robertApi = robertApi;
        this.clientKeys = ClientKeys.builder(applicationProperties.getCryptoPublicKey())
                .build();

        final var captchaSolution = resolveMockedCaptchaChallenge();
        final var registerResponse = register(captchaSolution.getId(), captchaSolution.getAnswer());
        this.clock = new EpochClock(registerResponse.getTimeStart());
    }

    private CaptchaSolution resolveMockedCaptchaChallenge() {
        // We generate a fake Captcha id which we will be used to differentiate mobile
        // applications
        final var captchaId = RandomStringUtils.random(7, true, false);

        // We simulate the user visual captcha resolution and we call the robert
        // endpoint

        // The mobile application ask a captcha id challenge from the captcha server

        final var captchaChallenge = captchaApi.captcha(
                CaptchaGenerationRequest.builder()
                        .locale("fr")
                        .type("IMAGE")
                        .build()
        );

        // The mobile application ask for an image captcha with the received captcha id
        // challenge
        final var response = captchaApi.captchaChallengeImageWithHttpInfo(captchaChallenge.getId());
        MatcherAssert.assertThat("Content-type header", response.getHeaders().get(CONTENT_TYPE), contains("image/png"));
        MatcherAssert.assertThat("image content", response.getData(), notNullValue());

        // The user reads the image content
        return new CaptchaSolution(captchaId, "ABCD");
    }

    private RegisterSuccessResponse register(final String captchaId, final String captchaSolution) {
        final var publicKey = Base64.getEncoder()
                .encodeToString(clientKeys.getKeyPair().getPublic().getEncoded());

        final var registerResponse = robertApi.register(
                RegisterRequest.builder()
                        .captcha(captchaSolution)
                        .captchaId(captchaId)
                        .clientPublicECDHKey(publicKey)
                        .pushInfo(
                                PushInfo.builder()
                                        .token("device-" + username)
                                        .locale("fr")
                                        .timezone("Europe/Paris")
                                        .build()
                        )
                        .build()
        );

        updateTuples(registerResponse.getTuples());

        return registerResponse;
    }

    private void updateTuples(final byte[] encryptedTuples) {
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

    public void receiveHelloMessage(final HelloMessage helloMessage) {
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
        final var reportResponse = robertApi.reportBatch(
                ReportBatchRequest.builder()
                        .token("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")
                        .contacts(new ArrayList<>(receivedHelloMessages.values()))
                        .build()
        );
        MatcherAssert.assertThat("response attribute 'success'", reportResponse.getSuccess(), equalTo(true));
        MatcherAssert.assertThat(
                "response attribute 'message'", reportResponse.getMessage(), equalTo("Successful operation")
        );
    }

    public HelloMessage produceHelloMessage(final Instant helloMessageTime) {
        final var epochId = clock.at(helloMessageTime).getEpochId();
        final var tuple = contactTupleByEpochId.get(epochId);
        return HelloMessage.builder(HELLO, clientKeys.getKeyForMac())
                .ebid(tuple.getEbid())
                .ecc(tuple.getEcc())
                .time(helloMessageTime)
                .build();
    }

    public ExposureStatusResponse requestStatus() {
        final var now = clock.now();
        final var currentEpochTuple = contactTupleByEpochId.get(now.getEpochId());
        final var exposureStatusResponse = robertApi.eSR(
                RobertRequestBuilder.withMacKey(clientKeys.getKeyForMac())
                        .exposureStatusRequest(currentEpochTuple.getEbid(), now)
                        .build()
        );
        updateTuples(exposureStatusResponse.getTuples());
        lastExposureStatusResponse = exposureStatusResponse;
        return exposureStatusResponse;
    }

    public void deleteExposureHistory() {
        final var now = clock.now();
        final var currentEpochTuple = contactTupleByEpochId.get(now.getEpochId());
        final var deleteResponse = robertApi.deleteExposureHistory(
                RobertRequestBuilder.withMacKey(clientKeys.getKeyForMac())
                        .deleteExposureHistory(currentEpochTuple.getEbid(), now)
                        .build()
        );
        MatcherAssert.assertThat("response attribute 'success'", deleteResponse.getSuccess(), equalTo(true));
    }

    public void unregister() {
        final var now = clock.now();
        final var currentEpochTuple = contactTupleByEpochId.get(now.getEpochId());
        final var deleteResponse = robertApi.unregister(
                RobertRequestBuilder.withMacKey(clientKeys.getKeyForMac())
                        .unregisterRequest(currentEpochTuple.getEbid(), now)
                        .build()
        );
        MatcherAssert.assertThat("response attribute 'success'", deleteResponse.getSuccess(), equalTo(true));
    }

}
