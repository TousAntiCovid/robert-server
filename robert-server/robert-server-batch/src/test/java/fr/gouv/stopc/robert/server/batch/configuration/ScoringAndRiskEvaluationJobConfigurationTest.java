package fr.gouv.stopc.robert.server.batch.configuration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.security.Key;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import javax.crypto.spec.SecretKeySpec;
import javax.inject.Inject;

import org.springframework.batch.core.Job;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.util.CollectionUtils;

import com.google.protobuf.ByteString;
import fr.gouv.stopc.robert.crypto.grpc.server.client.service.ICryptoServerGrpcClient;
import fr.gouv.stopc.robert.crypto.grpc.server.messaging.GetInfoFromHelloMessageResponse;
import fr.gouv.stopc.robert.server.batch.RobertServerBatchApplication;
import fr.gouv.stopc.robert.server.batch.utils.ProcessorTestUtils;
import fr.gouv.stopc.robert.server.batch.utils.PropertyLoader;
import fr.gouv.stopc.robert.server.common.service.IServerConfigurationService;
import fr.gouv.stopc.robert.server.common.utils.ByteUtils;
import fr.gouv.stopc.robert.server.common.utils.TimeUtils;
import fr.gouv.stopc.robert.server.crypto.exception.RobertServerCryptoException;
import fr.gouv.stopc.robert.server.crypto.service.CryptoService;
import fr.gouv.stopc.robert.server.crypto.structure.CryptoAES;
import fr.gouv.stopc.robert.server.crypto.structure.impl.CryptoAESECB;
import fr.gouv.stopc.robert.server.crypto.structure.impl.CryptoHMACSHA256;
import fr.gouv.stopc.robert.server.crypto.structure.impl.CryptoSkinny64;
import fr.gouv.stopc.robertserver.database.model.Contact;
import fr.gouv.stopc.robertserver.database.model.EpochExposition;
import fr.gouv.stopc.robertserver.database.model.HelloMessageDetail;
import fr.gouv.stopc.robertserver.database.model.Registration;
import fr.gouv.stopc.robertserver.database.service.ContactService;
import fr.gouv.stopc.robertserver.database.service.IRegistrationService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@Slf4j
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {  ScoringAndRiskEvaluationJobConfigurationTest.BatchTestConfig.class, RobertServerBatchApplication.class })
@TestPropertySource(locations="classpath:application.properties", properties = {"robert.scoring.algo-version=2",
"robert.scoring.batch-mode=SCORE_CONTACTS_AND_COMPUTE_RISK"})
public class ScoringAndRiskEvaluationJobConfigurationTest {

    private final static String SHOULD_NOT_FAIL = "It should not fail";

    @Inject
    JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private ContactService contactService;

    @Autowired
    private CryptoService cryptoService;

    @Autowired
    private IServerConfigurationService serverConfigurationService;

    @Autowired
    private PropertyLoader propertyLoader;

    @Autowired
    private IRegistrationService registrationService;

    @MockBean
    private ICryptoServerGrpcClient cryptoServerClient;

    @MockBean
    private RobertServerBatchConfiguration config;


    private Optional<Registration> registration;

    private byte[] serverKey;
    private byte countryCode;
    private Key federationKey;

    private long epochDuration;
    private long serviceTimeStart;

    @BeforeEach
    public void before() {

        this.serverKey = this.generateKey(24);
        this.federationKey = new SecretKeySpec(this.generateKey(32), CryptoAES.AES_ENCRYPTION_KEY_SCHEME);
        this.countryCode = this.serverConfigurationService.getServerCountryCode();
    }

    @Test
    public void testScoreAndProcessRisksWithABadEncryptedCountryCodeShouldNotUpdateRegistration() throws Exception {

        //  Given
        this.registration = this.registrationService.createRegistration(ProcessorTestUtils.generateIdA());
        assertTrue(this.registration.isPresent());

        final long tpstStart = this.serverConfigurationService.getServiceTimeStart();
        final long currentTime = TimeUtils.convertUnixMillistoNtpSeconds(new Date().getTime());
        final int currentEpochId = TimeUtils.getNumberOfEpochsBetween(tpstStart, currentTime);
        byte[] ebid = this.cryptoService.generateEBID(new CryptoSkinny64(serverKey), currentEpochId, ProcessorTestUtils.generateIdA());

        // Create a fake Encrypted Country Code (ECC)
        byte[] encryptedCountryCode = new byte[] { (byte) 0xff };
        Contact contact = Contact.builder()
                .ebid(new byte[8])
                .ecc(encryptedCountryCode)
                .messageDetails(this.generateHelloMessagesFor(ebid, encryptedCountryCode, currentEpochId))
                .build();

        this.contactService.saveContacts(Arrays.asList(contact));

        assertFalse(CollectionUtils.isEmpty(this.contactService.findAll()));
        assertEquals(1, this.contactService.findAll().size());

        // When
        this.jobLauncherTestUtils.launchJob();

        // Then
        assertTrue(CollectionUtils.isEmpty(this.contactService.findAll()));

        Optional<Registration> expectedRegistration = this.registrationService
                .findById(this.registration.get().getPermanentIdentifier());

        assertTrue(expectedRegistration.isPresent());

        assertTrue(CollectionUtils.isEmpty(expectedRegistration.get().getExposedEpochs()));
        assertFalse(expectedRegistration.get().isAtRisk());

    }


    @Test
    public void testScoreAndProcessRisksWhenRegistrationDoesNotExist() throws Exception {

        // Given

        this.registration = this.registrationService.createRegistration(ProcessorTestUtils.generateIdA());
        assertTrue(this.registration.isPresent());

        final long tpstStart = this.serverConfigurationService.getServiceTimeStart();
        final long currentTime = TimeUtils.convertUnixMillistoNtpSeconds(new Date().getTime());

        final int currentEpochId = TimeUtils.getNumberOfEpochsBetween(tpstStart, currentTime);

        byte[] ebid;
        ebid = this.cryptoService.generateEBID(new CryptoSkinny64(serverKey), currentEpochId, ProcessorTestUtils.generateIdA());

        byte[] encryptedCountryCode = this.cryptoService.encryptCountryCode(new CryptoAESECB(federationKey), ebid, countryCode);

        byte[] time = new byte[2];

        byte[] timeOfDevice = new byte[4];
        System.arraycopy(ByteUtils.longToBytes(currentTime + 1), 4, timeOfDevice, 0, 4);

        byte[] timeHelloB = new byte[4];
        System.arraycopy(ByteUtils.longToBytes(currentTime), 4, timeHelloB, 0, 4);

        byte[] helloMessage = new byte[16];
        System.arraycopy(encryptedCountryCode, 0, helloMessage, 0, encryptedCountryCode.length);
        System.arraycopy(ebid, 0, helloMessage, encryptedCountryCode.length, ebid.length);
        System.arraycopy(time, 0, helloMessage, encryptedCountryCode.length + ebid.length, time.length);

        when(this.cryptoServerClient.getInfoFromHelloMessage(any())).thenReturn(Optional.empty());

        Contact contact = Contact.builder()
                .ebid(ebid)
                .ecc(encryptedCountryCode)
                .messageDetails(this.generateHelloMessagesFor(ebid, encryptedCountryCode, currentEpochId))
                .build();

        this.contactService.saveContacts(Arrays.asList(contact));

        assertFalse(CollectionUtils.isEmpty(this.contactService.findAll()));
        assertEquals(1, this.contactService.findAll().size());

        // When
        this.jobLauncherTestUtils.launchJob();

        // Then
        assertTrue(CollectionUtils.isEmpty(this.contactService.findAll()));
        verify(this.cryptoServerClient, atLeast(1)).getInfoFromHelloMessage(any());

    }


    @Test
    public void testScoreAndProcessRisksWhenContactIsValid() throws Exception {

        // Given
        this.registration = this.registrationService.createRegistration(ProcessorTestUtils.generateIdA());
        assertTrue(this.registration.isPresent());

        final long tpstStart = this.serverConfigurationService.getServiceTimeStart();
        final long currentTime = TimeUtils.convertUnixMillistoNtpSeconds(new Date().getTime());

        final int currentEpochId = TimeUtils.getNumberOfEpochsBetween(tpstStart, currentTime);

        final int previousEpoch = TimeUtils.getNumberOfEpochsBetween(
                tpstStart,
                currentTime - this.serverConfigurationService.getEpochDurationSecs());

        Registration registrationWithEE = this.registration.get();
        registrationWithEE.setExposedEpochs(Arrays.asList(EpochExposition.builder()
                .epochId(previousEpoch)
                .expositionScores(Arrays.asList(0.0))
                .build(),
                EpochExposition.builder()
                .epochId(currentEpochId)
                .expositionScores(Arrays.asList(0.0))
                .build()));

        int nbOfExposedEpochs = registrationWithEE.getExposedEpochs().size();

        this.registrationService.saveRegistration(registrationWithEE);

        Contact contact = this.generateContact(currentEpochId, currentTime);


        this.contactService.saveContacts(Arrays.asList(contact));

        doReturn(Optional.of(
                GetInfoFromHelloMessageResponse.newBuilder()
                .setIdA(ByteString.copyFrom(this.registration.get().getPermanentIdentifier()))
                .setCountryCode(ByteString.copyFrom(new byte[] { this.serverConfigurationService.getServerCountryCode() }))
                .setEpochId(currentEpochId)
                .build()))
        .when(this.cryptoServerClient)
        .getInfoFromHelloMessage(any());

        assertFalse(CollectionUtils.isEmpty(this.contactService.findAll()));
        assertEquals(1, this.contactService.findAll().size());

        // When
        this.jobLauncherTestUtils.launchJob();

        // Then
        assertTrue(CollectionUtils.isEmpty(this.contactService.findAll()));

        Optional<Registration> expectedRegistration = this.registrationService
                .findById(this.registration.get().getPermanentIdentifier());
        assertTrue(expectedRegistration.isPresent());
        assertFalse(CollectionUtils.isEmpty(expectedRegistration.get().getExposedEpochs()));
        assertEquals(expectedRegistration.get().getExposedEpochs().size(), nbOfExposedEpochs);
        verify(this.cryptoServerClient, atLeast(1)).getInfoFromHelloMessage(any());


    }

    @Test
    public void testScoreAndProcessRisksWhenTheRegistrationHasTooOldExposedEpochsShouldRemoveTooOldExposedEpochs() throws Exception {

        // Given
        this.registration = this.registrationService.createRegistration(ProcessorTestUtils.generateIdA());
        assertTrue(this.registration.isPresent());

        final long tpstStart = this.serverConfigurationService.getServiceTimeStart();
        final long currentTime = TimeUtils.convertUnixMillistoNtpSeconds(new Date().getTime());

        final int currentEpochId = TimeUtils.getNumberOfEpochsBetween(tpstStart, currentTime);
        int val = (this.propertyLoader.getContagiousPeriod() * 24 * 3600)
                / this.serverConfigurationService.getEpochDurationSecs();
        val++;
        int tooOldEpochId = currentEpochId - val;
        Registration registrationWithEE = this.registration.get();

        EpochExposition oldEpochExposition = EpochExposition
                .builder()
                .epochId(tooOldEpochId)
                .expositionScores(Arrays.asList(0.0))
                .build();

        registrationWithEE.setExposedEpochs(Arrays.asList(oldEpochExposition));

        int nbOfExposedEpochsBefore = registrationWithEE.getExposedEpochs().size();

        this.registrationService.saveRegistration(registrationWithEE);

        Contact contact = this.generateContact(currentEpochId, currentTime);

        this.contactService.saveContacts(Arrays.asList(contact));

        assertFalse(CollectionUtils.isEmpty(this.contactService.findAll()));
        assertEquals(1, this.contactService.findAll().size());

        // When
        this.jobLauncherTestUtils.launchJob();

        // Then
        Optional<Registration> expectedRegistration = this.registrationService
                .findById(registrationWithEE.getPermanentIdentifier());

        assertTrue(CollectionUtils.isEmpty(this.contactService.findAll()));
        assertTrue(expectedRegistration.isPresent());
        assertFalse(CollectionUtils.isEmpty(expectedRegistration.get().getExposedEpochs()));
        assertEquals(expectedRegistration.get().getExposedEpochs().size(), nbOfExposedEpochsBefore);

        EpochExposition currentExposedEpoch = expectedRegistration.get().getExposedEpochs().get(nbOfExposedEpochsBefore - 1);
        assertNotEquals(currentExposedEpoch.getEpochId(), oldEpochExposition.getEpochId());
        assertFalse(expectedRegistration.get().isAtRisk());

        verify(this.cryptoServerClient, times(contact.getMessageDetails().size())).getInfoFromHelloMessage(any());

    }

    @Test
    public void testScoreAndProcessRiskskWhenScoresEqualsZeroldShouldNotBeAtRisk() throws Exception {

        // Given
        this.registration = this.registrationService.createRegistration(ProcessorTestUtils.generateIdA());
        assertTrue(this.registration.isPresent());


        final long tpstStart = this.serverConfigurationService.getServiceTimeStart();
        final long currentTime = TimeUtils.convertUnixMillistoNtpSeconds(new Date().getTime());

        final int currentEpochId = TimeUtils.getCurrentEpochFrom(tpstStart);
        final int previousEpoch = TimeUtils.getNumberOfEpochsBetween(tpstStart, currentTime - 900);

        // Setup id with an existing score below threshold
        Registration registrationWithEE = this.registration.get();
        registrationWithEE.setExposedEpochs(Arrays.asList(EpochExposition.builder()
                .epochId(previousEpoch)
                .expositionScores(Arrays.asList(10.0))
                .build(),
                EpochExposition.builder()
                .epochId(currentEpochId)
                .expositionScores(Arrays.asList(1.0))
                .build()));

        this.registrationService.saveRegistration(registrationWithEE);

        // When
        this.jobLauncherTestUtils.launchJob();

        // Then
        Optional<Registration> expectedRegistration = this.registrationService
                .findById(registrationWithEE.getPermanentIdentifier());

        assertTrue(expectedRegistration.isPresent());
        assertFalse(expectedRegistration.get().isAtRisk());
        verify(this.cryptoServerClient, never()).getInfoFromHelloMessage(any());

    }

    @Test
    public void testScoreAndProcessRisksWhenRecentExposedEpochScoreGreaterThanRiskThresholdShouldBeAtRisk() throws Exception {

        // Given
        this.registration = this.registrationService.createRegistration(ProcessorTestUtils.generateIdA());
        assertTrue(this.registration.isPresent());


        final long tpstStart = this.serverConfigurationService.getServiceTimeStart();
        final long currentTime = TimeUtils.convertUnixMillistoNtpSeconds(new Date().getTime());

        final int currentEpochId = TimeUtils.getCurrentEpochFrom(tpstStart);
        final int previousEpoch = TimeUtils.getNumberOfEpochsBetween(tpstStart, currentTime - 900);

        // Setup id with an existing score below threshold
        Registration registrationWithEE = this.registration.get();
        registrationWithEE.setExposedEpochs(Arrays.asList(EpochExposition.builder()
                .epochId(previousEpoch)
                .expositionScores(Arrays.asList(14.0))
                .build(),
                EpochExposition.builder()
                .epochId(currentEpochId)
                .expositionScores(Arrays.asList(2.0))
                .build()));

        this.registrationService.saveRegistration(registrationWithEE);

        // When
        this.jobLauncherTestUtils.launchJob();

        // Then
        Optional<Registration> expectedRegistration = this.registrationService
                .findById(registrationWithEE.getPermanentIdentifier());

        assertTrue(expectedRegistration.isPresent());
        assertTrue(expectedRegistration.get().isAtRisk());
        verify(this.cryptoServerClient, never()).getInfoFromHelloMessage(any());

    }

    @Test
    public void testScoreAndProcessRisksWhenEpochScoresLessThanRiskThresholdShouldNotBeAtRisk() throws Exception {

        // Given
        this.registration = this.registrationService.createRegistration(ProcessorTestUtils.generateIdA());
        assertTrue(this.registration.isPresent());


        final long tpstStart = this.serverConfigurationService.getServiceTimeStart();
        final long currentTime = TimeUtils.convertUnixMillistoNtpSeconds(new Date().getTime());

        final int currentEpochId = TimeUtils.getCurrentEpochFrom(tpstStart);
        final int previousEpoch = TimeUtils.getNumberOfEpochsBetween(tpstStart, currentTime - 900);

        // Setup id with an existing score below threshold
        Registration registrationWithEE = this.registration.get();
        registrationWithEE.setExposedEpochs(Arrays.asList(EpochExposition.builder()
                .epochId(previousEpoch)
                .expositionScores(Arrays.asList(0.0))
                .build(),
                EpochExposition.builder()
                .epochId(currentEpochId)
                .expositionScores(Arrays.asList(0.0))
                .build()));

        this.registrationService.saveRegistration(registrationWithEE);

        // When
        this.jobLauncherTestUtils.launchJob();

        // Then
        Optional<Registration> expectedRegistration = this.registrationService
                .findById(registrationWithEE.getPermanentIdentifier());

        assertTrue(expectedRegistration.isPresent());
        assertFalse(expectedRegistration.get().isAtRisk());

        verify(this.cryptoServerClient, never()).getInfoFromHelloMessage(any());

    }

    @Test
    public void testResetAtRisk() throws Exception {

        // Given
        final long tpstStart = this.serverConfigurationService.getServiceTimeStart();
        final int currentEpochId = TimeUtils.getCurrentEpochFrom(tpstStart);

        Registration registrationHavingRiskLevelThatMustBeReset = this.registrationService.createRegistration(ProcessorTestUtils.generateIdA()).get();

        registrationHavingRiskLevelThatMustBeReset.setAtRisk(true);
        registrationHavingRiskLevelThatMustBeReset.setLatestRiskEpoch(currentEpochId - (propertyLoader.getRiskLevelRetentionPeriodInDays() * TimeUtils.EPOCHS_PER_DAY) - 1);

        this.registrationService.saveRegistration(registrationHavingRiskLevelThatMustBeReset);

        Registration registrationHavingRiskLevelThatMustNotBeReset = this.registrationService.createRegistration(ProcessorTestUtils.generateIdA()).get();

        registrationHavingRiskLevelThatMustNotBeReset.setAtRisk(true);
        registrationHavingRiskLevelThatMustNotBeReset.setLatestRiskEpoch(currentEpochId - (propertyLoader.getRiskLevelRetentionPeriodInDays() * TimeUtils.EPOCHS_PER_DAY) + 1);

        this.registrationService.saveRegistration(registrationHavingRiskLevelThatMustNotBeReset);

        // When
        this.jobLauncherTestUtils.launchJob();

        // Then
        Optional<Registration> expectedRegistrationHavingRiskLevelThatMustBeReset = this.registrationService
                .findById(registrationHavingRiskLevelThatMustBeReset.getPermanentIdentifier());

        assertTrue(expectedRegistrationHavingRiskLevelThatMustBeReset.isPresent());
        assertFalse(expectedRegistrationHavingRiskLevelThatMustBeReset.get().isAtRisk());

        Optional<Registration> expectedRegistrationHavingRiskLevelThatMustNotBeReset = this.registrationService
                .findById(registrationHavingRiskLevelThatMustNotBeReset.getPermanentIdentifier());

        assertTrue(expectedRegistrationHavingRiskLevelThatMustNotBeReset.isPresent());
        assertTrue(expectedRegistrationHavingRiskLevelThatMustNotBeReset.get().isAtRisk());

    }



    @Configuration
    @Import({ ScoringAndRiskEvaluationJobConfiguration.class })
    public static class BatchTestConfig {

        private Job scoreAndProcessRisks;

        @Bean
        @Inject
        JobLauncherTestUtils jobLauncherTestUtils(Job scoreAndProcessRisks) {

            this.scoreAndProcessRisks = scoreAndProcessRisks;

            JobLauncherTestUtils jobLauncherTestUtils = new JobLauncherTestUtils();
            jobLauncherTestUtils.setJob(this.scoreAndProcessRisks);

            return jobLauncherTestUtils;
        }

    }

    @AfterEach
    public void afterAll() {
        this.contactService.deleteAll();
        this.registrationService.deleteAll();
    }

    private HelloMessageDetail generateHelloMessageFor(byte[] ebid, byte[] encryptedCountryCode, long t, int rssi) throws Exception {
        byte[] time = new byte[2];

        // Get timestamp on sixteen bits
        System.arraycopy(ByteUtils.longToBytes(t), 6, time, 0, 2);

        byte[] timeOfDevice = new byte[4];
        System.arraycopy(ByteUtils.longToBytes(t + 1), 4, timeOfDevice, 0, 4);

        byte[] timeHelloB = new byte[4];
        System.arraycopy(ByteUtils.longToBytes(t), 4, timeHelloB, 0, 4);

        // Clear out the first two bytes
        timeHelloB[0] = (byte) (timeHelloB[0] & 0x00);
        timeHelloB[1] = (byte) (timeHelloB[1] & 0x00);

        int timeReceived = ByteUtils.bytesToInt(timeOfDevice);
        int timeHello = ByteUtils.bytesToInt(timeHelloB);

        byte[] helloMessage = new byte[16];
        System.arraycopy(encryptedCountryCode, 0, helloMessage, 0, encryptedCountryCode.length);
        System.arraycopy(ebid, 0, helloMessage, encryptedCountryCode.length, ebid.length);
        System.arraycopy(time, 0, helloMessage, encryptedCountryCode.length + ebid.length, time.length);

        byte[] mac = this.cryptoService
                .generateMACHello(new CryptoHMACSHA256(getKeyMacFor(this.registration.get().getPermanentIdentifier())), helloMessage);

        return HelloMessageDetail.builder()
                .timeFromHelloMessage(timeHello)
                .timeCollectedOnDevice(Integer.toUnsignedLong(timeReceived))
                .rssiCalibrated(rssi)
                .mac(mac)
                .build();
    }

    private byte[] generateKey(int sizeInBytes) {
        byte[] data = new byte[sizeInBytes];
        new SecureRandom().nextBytes(data);
        return data;
    }

    private byte[] getKeyMacFor(byte[] idA) {
        return ProcessorTestUtils.generateRandomByteArrayOfSize(32);
    }

    private List<HelloMessageDetail> generateHelloMessagesFor(byte[] ebid, byte[] encryptedCountryCode, int currentEpoch) throws Exception {
        List<HelloMessageDetail> messages = new ArrayList<>();

        Random random = new Random();
        int nbOfHellos = random.nextInt(5) + 1;
        long t = currentEpoch * this.epochDuration + this.serviceTimeStart + 15L;

        for (int i = 0; i < nbOfHellos; i++) {
            int rssi = -30 - random.nextInt(90);
            t += random.nextInt(30) + 5;
            messages.add(generateHelloMessageFor(ebid, encryptedCountryCode, t, rssi));
        }

        return messages;
    }

    private Contact generateContact(int epochOfMessage, long timeOfMessage) throws RobertServerCryptoException {

        byte[] ebid = this.cryptoService.generateEBID(new CryptoSkinny64(this.serverKey), epochOfMessage,
                this.registration.get().getPermanentIdentifier());
        byte[] encryptedCountryCode = this.cryptoService.encryptCountryCode(new CryptoAESECB(this.federationKey), ebid, this.countryCode);
        byte[] time = new byte[2];

        // Get timestamp on 16 bits
        System.arraycopy(ByteUtils.longToBytes(timeOfMessage), 6, time, 0, 2);

        byte[] timeOfDevice = new byte[4];
        System.arraycopy(ByteUtils.longToBytes(timeOfMessage + 1), 4, timeOfDevice, 0, 4);

        byte[] timeHelloB = new byte[4];
        System.arraycopy(ByteUtils.longToBytes(timeOfMessage), 4, timeHelloB, 0, 4);

        timeHelloB[0] = (byte) (timeHelloB[0] & 0x00);
        timeHelloB[1] = (byte) (timeHelloB[1] & 0x00);

        int timeReceived = ByteUtils.bytesToInt(timeOfDevice);
        int timeHello = ByteUtils.bytesToInt(timeHelloB);

        byte[] helloMessage = new byte[16];
        System.arraycopy(encryptedCountryCode, 0, helloMessage, 0, encryptedCountryCode.length);
        System.arraycopy(ebid, 0, helloMessage, encryptedCountryCode.length, ebid.length);
        System.arraycopy(time, 0, helloMessage, encryptedCountryCode.length + ebid.length, time.length);

        doReturn(Optional.of(
                GetInfoFromHelloMessageResponse.newBuilder()
                .setIdA(ByteString.copyFrom(this.registration.get().getPermanentIdentifier()))
                .setCountryCode(ByteString.copyFrom(new byte[] { this.serverConfigurationService.getServerCountryCode() }))
                .setEpochId(epochOfMessage)
                .build()))
        .when(this.cryptoServerClient)
        .getInfoFromHelloMessage(any());

        byte[] mac = this.cryptoService
                .generateMACHello(new CryptoHMACSHA256(getKeyMacFor(this.registration.get().getPermanentIdentifier())), helloMessage);

        HelloMessageDetail helloMessageDetail = HelloMessageDetail.builder()
                .mac(mac)
                .timeFromHelloMessage(timeHello)
                .timeCollectedOnDevice(Integer.toUnsignedLong(timeReceived))
                .rssiCalibrated(-70)
                .build();

        return Contact.builder()
                .ebid(ebid)
                .ecc(encryptedCountryCode)
                .messageDetails(Arrays.asList(helloMessageDetail))
                .build();
    }

}
