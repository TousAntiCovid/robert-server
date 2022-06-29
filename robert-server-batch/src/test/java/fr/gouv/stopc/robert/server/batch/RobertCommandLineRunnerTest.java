package fr.gouv.stopc.robert.server.batch;

import com.google.protobuf.ByteString;
import fr.gouv.stopc.robert.crypto.grpc.server.client.service.ICryptoServerGrpcClient;
import fr.gouv.stopc.robert.crypto.grpc.server.messaging.GetInfoFromHelloMessageResponse;
import fr.gouv.stopc.robert.server.batch.configuration.PropertyLoader;
import fr.gouv.stopc.robert.server.batch.configuration.RobertServerBatchConfiguration;
import fr.gouv.stopc.robert.server.batch.configuration.job.ScoringAndRiskEvaluationJobConfiguration;
import fr.gouv.stopc.robert.server.batch.utils.ProcessorTestUtils;
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
import fr.gouv.stopc.robertserver.database.model.HelloMessageDetail;
import fr.gouv.stopc.robertserver.database.model.Registration;
import fr.gouv.stopc.robertserver.database.service.ContactService;
import fr.gouv.stopc.robertserver.database.service.IRegistrationService;
import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.batch.core.Job;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.crypto.spec.SecretKeySpec;

import java.security.Key;
import java.security.SecureRandom;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

@ExtendWith(SpringExtension.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ContextConfiguration(classes = { RobertCommandLineRunnerTest.BatchTestConfig.class,
        RobertServerBatchApplication.class })
@TestPropertySource(locations = "classpath:application-legacy.properties", properties = {
        "robert-batch.command-line-runner.reasses-risk.enabled=true"
})
public class RobertCommandLineRunnerTest {

    @Autowired
    ApplicationContext appContext;

    @Autowired
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

    @Autowired
    private ApplicationContext context;

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
        this.serviceTimeStart = this.serverConfigurationService.getServiceTimeStart();
    }

    @Test
    void can_log_how_many_hello_messages_will_be_processed() throws Exception {
        final var currentTime = TimeUtils.convertUnixMillistoNtpSeconds(new Date().getTime());
        final var currentEpochId = TimeUtils.getNumberOfEpochsBetween(this.serviceTimeStart, currentTime);

        this.registration = this.registrationService.createRegistration(ProcessorTestUtils.generateIdA());

        // contacts 1 and 2 have 1 hello message each
        // contact 3 has 2 hello messages
        final var contact1 = this.generateContact(currentEpochId, currentTime);
        final var contact2 = this.generateContact(currentEpochId, currentTime);
        final var contact3 = this.generateContact(currentEpochId, currentTime);
        contact3.setMessageDetails(
                List.of(
                        contact3.getMessageDetails().get(0),
                        contact3.getMessageDetails().get(0)
                )
        );
        this.contactService.saveContacts(Arrays.asList(contact1, contact2, contact3));

        try (final var logCaptor = LogCaptor.forClass(RobertCommandLineRunner.class)) {
            var lineRunner = context.getBean(RobertCommandLineRunner.class);
            lineRunner.run("");

            assertThat(logCaptor.getInfoLogs())
                    .contains("4 hello messages waiting for process", "4 hello messages remaining after process");
        }
    }

    @Configuration
    @Import({ ScoringAndRiskEvaluationJobConfiguration.class })
    public static class BatchTestConfig {

        private Job scoreAndProcessRisks;

        @Bean
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

    private HelloMessageDetail generateHelloMessageFor(byte[] ebid, byte[] encryptedCountryCode, long t, int rssi)
            throws Exception {
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
                .generateMACHello(
                        new CryptoHMACSHA256(getKeyMacFor(this.registration.get().getPermanentIdentifier())),
                        helloMessage
                );

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

    private List<HelloMessageDetail> generateHelloMessagesFor(byte[] ebid, byte[] encryptedCountryCode,
            int currentEpoch) throws Exception {
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

        byte[] ebid = this.cryptoService.generateEBID(
                new CryptoSkinny64(this.serverKey), epochOfMessage,
                this.registration.get().getPermanentIdentifier()
        );
        byte[] encryptedCountryCode = this.cryptoService
                .encryptCountryCode(new CryptoAESECB(this.federationKey), ebid, this.countryCode);
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

        doReturn(
                Optional.of(
                        GetInfoFromHelloMessageResponse.newBuilder()
                                .setIdA(ByteString.copyFrom(this.registration.get().getPermanentIdentifier()))
                                .setCountryCode(
                                        ByteString.copyFrom(
                                                new byte[] { this.countryCode }
                                        )
                                )
                                .setEpochId(epochOfMessage)
                                .build()
                )
        )
                .when(this.cryptoServerClient)
                .getInfoFromHelloMessage(any());

        byte[] mac = this.cryptoService
                .generateMACHello(
                        new CryptoHMACSHA256(getKeyMacFor(this.registration.get().getPermanentIdentifier())),
                        helloMessage
                );

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
