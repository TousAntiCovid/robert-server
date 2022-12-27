package fr.gouv.stopc.e2e.mobileapplication;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.gouv.stopc.e2e.config.ApplicationProperties;
import fr.gouv.stopc.e2e.external.common.utils.ByteUtils;
import fr.gouv.stopc.e2e.external.crypto.CryptoAESGCM;
import fr.gouv.stopc.e2e.external.crypto.exception.RobertServerCryptoException;
import fr.gouv.stopc.e2e.external.crypto.model.EphemeralTupleJson;
import fr.gouv.stopc.e2e.mobileapplication.model.*;
import fr.gouv.stopc.e2e.mobileapplication.repository.ApplicationIdentityRepository;
import fr.gouv.stopc.e2e.mobileapplication.repository.CaptchaRepository;
import fr.gouv.stopc.e2e.mobileapplication.repository.RegistrationRepository;
import fr.gouv.stopc.e2e.mobileapplication.repository.model.Registration;
import fr.gouv.stopc.e2e.steps.PlatformTimeSteps;
import fr.gouv.stopc.robert.client.api.CaptchaApi;
import fr.gouv.stopc.robert.client.api.RobertApi;
import fr.gouv.stopc.robert.client.model.*;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import static fr.gouv.stopc.e2e.external.common.enums.DigestSaltEnum.HELLO;
import static java.util.Base64.getEncoder;
import static java.util.stream.Collectors.toMap;
import static org.apache.http.HttpHeaders.CONTENT_TYPE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@Slf4j
public class MobileApplication {

    private final String username;

    private final ClientKeys clientKeys;

    private final Map<Integer, ContactTuple> contactTupleByEpochId = new HashMap<>();

    private final Map<ContactTuple, Contact> receivedHelloMessages = new HashMap<>();

    private final EpochClock clock;

    private final CaptchaApi captchaApi;

    private final RobertApi robertApi;

    private final String applicationId;

    private final RegistrationRepository registrationRepository;

    private final PlatformTimeSteps platformTime;

    public MobileApplication(
            final String username,
            final ApplicationProperties applicationProperties,
            final CaptchaApi captchaApi,
            final RobertApi robertApi,
            final CaptchaRepository captchaRepository,
            final ApplicationIdentityRepository applicationIdentityRepository,
            final RegistrationRepository registrationRepository,
            final PlatformTimeSteps platformTime) {
        this.platformTime = platformTime;
        this.username = username;
        this.registrationRepository = registrationRepository;
        this.captchaApi = captchaApi;
        this.robertApi = robertApi;
        this.clientKeys = ClientKeys.builder(applicationProperties.getCryptoPublicKey())
                .build();
        resolveCaptchaChallenge();
        final var captcha = captchaRepository.saveRandomCaptcha();
        final var registerResponse = register(captcha.getId(), captcha.getAnswer());
        this.applicationId = applicationIdentityRepository.findLastInsertedIdA();
        this.clock = new EpochClock(registerResponse.getTimeStart());
    }

    private void resolveCaptchaChallenge() {
        // We don't really need it, but for the demonstration...
        // The mobile application asks a captcha challenge
        final var captchaChallenge = captchaApi.captcha(
                CaptchaGenerationRequest.builder()
                        .locale("fr")
                        .type("IMAGE")
                        .build()
        );
        // Then fetch the associated image
        final var response = captchaApi.captchaChallengeImageWithHttpInfo(captchaChallenge.getId());
        assertThat("Content-type header", response.getHeaders().get(CONTENT_TYPE), contains("image/png"));
        assertThat("image content", response.getBody(), notNullValue());
    }

    private RegisterSuccessResponse register(final String captchaId, final String captchaAnswer) {
        final var publicKey = getEncoder()
                .encodeToString(clientKeys.getKeyPair().getPublic().getEncoded());

        final var registerResponse = robertApi.register(
                RegisterRequest.builder()
                        .captcha(captchaAnswer)
                        .captchaId(captchaId)
                        .clientPublicECDHKey(publicKey)
                        .pushInfo(
                                PushInfo.builder()
                                        .token("valid-device-" + username)
                                        .locale("fr-FR")
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

    void receiveHelloMessage(final HelloMessage helloMessage) {
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

    public void reportContacts(final String reportCode) {
        final var reportResponse = robertApi.reportBatch(
                ReportBatchRequest.builder()
                        .token(reportCode)
                        .contacts(new ArrayList<>(receivedHelloMessages.values()))
                        .build()
        );
        assertThat("response attribute 'success'", reportResponse.getSuccess(), equalTo(true));
        assertThat(
                "response attribute 'message'", reportResponse.getMessage(), equalTo("Successful operation")
        );
    }

    HelloMessage produceHelloMessage(final Instant helloMessageTime) {
        final var epochId = clock.at(helloMessageTime).asEpochId();
        final var tuple = contactTupleByEpochId.get(epochId);
        return HelloMessage.builder(HELLO, clientKeys.getKeyForMac())
                .ebid(tuple.getEbid())
                .ecc(tuple.getEcc())
                .time(helloMessageTime)
                .build();
    }

    public ExposureStatus requestStatus() {
        final var now = clock.at(platformTime.getPlatformTime());
        final var currentEpochTuple = contactTupleByEpochId.get(now.asEpochId());
        final var exposureStatusResponse = robertApi.eSR(
                RobertRequestBuilder.withMacKey(clientKeys.getKeyForMac())
                        .exposureStatusRequest(currentEpochTuple.getEbid(), now)
                        .build()
        );
        updateTuples(exposureStatusResponse.getTuples());

        return new ExposureStatus(exposureStatusResponse);
    }

    public void deleteExposureHistory() {
        final var now = clock.now();
        final var currentEpochTuple = contactTupleByEpochId.get(now.asEpochId());
        final var deleteResponse = robertApi.deleteExposureHistory(
                RobertRequestBuilder.withMacKey(clientKeys.getKeyForMac())
                        .deleteExposureHistory(currentEpochTuple.getEbid(), now)
                        .build()
        );
        assertThat("response attribute 'success'", deleteResponse.getSuccess(), equalTo(true));
    }

    public void unregister() {
        final var now = clock.now();
        final var currentEpochTuple = contactTupleByEpochId.get(now.asEpochId());
        final var deleteResponse = robertApi.unregister(
                RobertRequestBuilder.withMacKey(clientKeys.getKeyForMac())
                        .unregisterRequest(currentEpochTuple.getEbid(), now)
                        .build()
        );
        assertThat("response attribute 'success'", deleteResponse.getSuccess(), equalTo(true));
    }

    public Registration getRegistration() {
        return this.registrationRepository.findById(Base64.getDecoder().decode(applicationId))
                .orElseThrow();
    }
}
