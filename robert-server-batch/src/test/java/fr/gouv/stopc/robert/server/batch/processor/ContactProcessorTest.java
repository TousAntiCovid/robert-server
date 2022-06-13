package fr.gouv.stopc.robert.server.batch.processor;

import com.google.protobuf.ByteString;
import fr.gouv.stopc.robert.crypto.grpc.server.client.service.ICryptoServerGrpcClient;
import fr.gouv.stopc.robert.crypto.grpc.server.messaging.ValidateContactResponse;
import fr.gouv.stopc.robert.server.batch.IntegrationLegacyTest;
import fr.gouv.stopc.robert.server.batch.configuration.PropertyLoader;
import fr.gouv.stopc.robert.server.batch.configuration.RobertServerBatchConfiguration;
import fr.gouv.stopc.robert.server.batch.exception.RobertScoringException;
import fr.gouv.stopc.robert.server.batch.service.ScoringStrategyService;
import fr.gouv.stopc.robert.server.batch.utils.ProcessorTestUtils;
import fr.gouv.stopc.robert.server.batch.writer.ContactItemWriter;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.util.CollectionUtils;

import javax.crypto.spec.SecretKeySpec;

import java.security.Key;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@IntegrationLegacyTest
class ContactProcessorTest {

    @Autowired
    private ContactService contactService;

    @Autowired
    private CryptoService cryptoService;

    @Autowired
    private IServerConfigurationService serverConfigurationService;

    @Autowired
    private IRegistrationService registrationService;

    @MockBean
    private ICryptoServerGrpcClient cryptoServerClient;

    @MockBean
    private RobertServerBatchConfiguration config;

    private ContactProcessor contactProcessor;

    private ContactItemWriter contactItemWriter;

    private Optional<Registration> registration;

    @Autowired
    private ScoringStrategyService scoringStrategyService;

    @Autowired
    private PropertyLoader propertyLoader;

    private byte[] serverKey;

    private Key federationKey;

    private byte countryCode;

    private long epochDuration;

    private long serviceTimeStart;

    private byte[] generateKey(int sizeInBytes) {
        byte[] data = new byte[sizeInBytes];
        new SecureRandom().nextBytes(data);
        return data;
    }

    @BeforeEach
    public void before() {
        this.serverKey = this.generateKey(24);
        this.federationKey = new SecretKeySpec(this.generateKey(32), CryptoAES.AES_ENCRYPTION_KEY_SCHEME);
        this.countryCode = this.serverConfigurationService.getServerCountryCode();

        this.contactProcessor = new ContactProcessor(
                serverConfigurationService,
                registrationService,
                cryptoServerClient,
                scoringStrategyService,
                propertyLoader
        );

        this.contactItemWriter = new ContactItemWriter(this.contactService);

        this.epochDuration = this.serverConfigurationService.getEpochDurationSecs();
        this.serviceTimeStart = this.serverConfigurationService.getServiceTimeStart();
    }

    @Test
    void testProcessContactWithABadEncryptedCountryCodeFails() throws Exception {
        this.registration = this.registrationService.createRegistration(ProcessorTestUtils.generateIdA());
        assertTrue(this.registration.isPresent());

        final long tpstStart = this.serverConfigurationService.getServiceTimeStart();
        final long currentTime = TimeUtils.convertUnixMillistoNtpSeconds(new Date().getTime());
        final int currentEpochId = TimeUtils.getNumberOfEpochsBetween(tpstStart, currentTime);
        byte[] ebid = this.cryptoService
                .generateEBID(new CryptoSkinny64(serverKey), currentEpochId, ProcessorTestUtils.generateIdA());

        // Create a fake Encrypted Country Code (ECC)
        byte[] encryptedCountryCode = new byte[] { (byte) 0xff };
        Contact contact = Contact.builder()
                .ebid(new byte[8])
                .ecc(encryptedCountryCode)
                .messageDetails(generateHelloMessagesFor(ebid, encryptedCountryCode, currentEpochId))
                .build();

        this.contactService.saveContacts(Arrays.asList(contact));

        assertFalse(CollectionUtils.isEmpty(this.contactService.findAll()));
        assertEquals(1, this.contactService.findAll().size());

        when(cryptoServerClient.validateContact(any()))
                .thenReturn(
                        ValidateContactResponse
                                .newBuilder()
                                .setIdA(ByteString.copyFrom(this.registration.get().getPermanentIdentifier()))
                                .setCountryCode(
                                        ByteString.copyFrom(
                                                new byte[] { this.serverConfigurationService.getServerCountryCode() }
                                        )
                                )
                                .setEpochId(currentEpochId)
                                .build()
                );

        // When
        Contact processedContact = this.contactProcessor.process(contact);

        // Then
        assertNotNull(processedContact);
        contactItemWriter.write(Arrays.asList(processedContact));
        assertTrue(CollectionUtils.isEmpty(this.contactService.findAll()));
        verify(this.cryptoServerClient, times(1)).validateContact(any());
    }

    @Test
    void testProcessContactWitNoMessagesFails() throws RobertServerCryptoException, RobertScoringException {
        this.registration = this.registrationService.createRegistration(ProcessorTestUtils.generateIdA());
        assertTrue(this.registration.isPresent());

        final long tpstStart = this.serverConfigurationService.getServiceTimeStart();
        final long currentTime = TimeUtils.convertUnixMillistoNtpSeconds(new Date().getTime());
        final int currentEpochId = TimeUtils.getNumberOfEpochsBetween(tpstStart, currentTime);
        byte[] ebid = this.cryptoService
                .generateEBID(new CryptoSkinny64(serverKey), currentEpochId, ProcessorTestUtils.generateIdA());
        byte[] encryptedCountryCode = this.cryptoService
                .encryptCountryCode(new CryptoAESECB(federationKey), ebid, countryCode);
        Contact contact = Contact.builder()
                .ebid(new byte[8])
                .ecc(encryptedCountryCode)
                .messageDetails(new ArrayList<>())
                .build();

        this.contactService.saveContacts(Arrays.asList(contact));

        assertFalse(CollectionUtils.isEmpty(this.contactService.findAll()));
        assertEquals(1, this.contactService.findAll().size());
        // When
        Contact processedContact = this.contactProcessor.process(contact);

        // Then
        assertNotNull(processedContact);
        contactItemWriter.write(Arrays.asList(processedContact));
        assertTrue(CollectionUtils.isEmpty(this.contactService.findAll()));
        verify(this.cryptoServerClient, never()).validateContact(any());
    }

    @Test
    void testProcessContactWhenRegistrationDoesNotExistFails() throws Exception {
        this.registration = this.registrationService.createRegistration(ProcessorTestUtils.generateIdA());
        assertTrue(this.registration.isPresent());

        // Given
        final long tpstStart = this.serverConfigurationService.getServiceTimeStart();
        final long currentTime = TimeUtils.convertUnixMillistoNtpSeconds(new Date().getTime());

        final int currentEpochId = TimeUtils.getNumberOfEpochsBetween(tpstStart, currentTime);

        byte[] ebid = this.cryptoService
                .generateEBID(new CryptoSkinny64(serverKey), currentEpochId, ProcessorTestUtils.generateIdA());
        byte[] encryptedCountryCode = this.cryptoService
                .encryptCountryCode(new CryptoAESECB(federationKey), ebid, countryCode);

        byte[] time = new byte[2];

        byte[] timeOfDevice = new byte[4];
        System.arraycopy(ByteUtils.longToBytes(currentTime + 1), 4, timeOfDevice, 0, 4);

        byte[] timeHelloB = new byte[4];
        System.arraycopy(ByteUtils.longToBytes(currentTime), 4, timeHelloB, 0, 4);

        byte[] helloMessage = new byte[16];
        System.arraycopy(encryptedCountryCode, 0, helloMessage, 0, encryptedCountryCode.length);
        System.arraycopy(ebid, 0, helloMessage, encryptedCountryCode.length, ebid.length);
        System.arraycopy(time, 0, helloMessage, encryptedCountryCode.length + ebid.length, time.length);

        Contact contact = Contact.builder()
                .ebid(ebid)
                .ecc(encryptedCountryCode)
                .messageDetails(generateHelloMessagesFor(ebid, encryptedCountryCode, currentEpochId))
                .build();

        var helloMessageDetail = contact.getMessageDetails().get(0);
        when(cryptoServerClient.validateContact(any()))
                .thenReturn(
                        ValidateContactResponse
                                .newBuilder()
                                .addInvalidHelloMessageDetails(
                                        ValidateContactResponse
                                                .newBuilder()
                                                .addInvalidHelloMessageDetailsBuilder()
                                                .setMac(ByteString.copyFrom(helloMessageDetail.getMac()))
                                                .setTimeSent(helloMessageDetail.getTimeFromHelloMessage())
                                                .setTimeReceived(helloMessageDetail.getTimeCollectedOnDevice())
                                )
                                .build()
                );

        this.contactService.saveContacts(Arrays.asList(contact));

        assertFalse(CollectionUtils.isEmpty(this.contactService.findAll()));
        assertEquals(1, this.contactService.findAll().size());

        this.registrationService.delete(this.registration.get());

        // When
        Contact processedContact = this.contactProcessor.process(contact);

        // Then
        assertNotNull(processedContact);
        contactItemWriter.write(Arrays.asList(processedContact));
        assertTrue(CollectionUtils.isEmpty(this.contactService.findAll()));
        verify(this.cryptoServerClient, times(1)).validateContact(any());
    }

    @Test
    void testProcessTwoContactsWithAggregatedScoreAboveThresholdYieldsRiskSucceeds() throws Exception {
        // Given
        this.registration = this.registrationService.createRegistration(ProcessorTestUtils.generateIdA());
        assertTrue(this.registration.isPresent());

        final long tpstStart = this.serverConfigurationService.getServiceTimeStart();
        final long currentTime = TimeUtils.convertUnixMillistoNtpSeconds(new Date().getTime());

        final int currentEpochId = TimeUtils.getNumberOfEpochsBetween(tpstStart, currentTime);

        final int previousEpoch = TimeUtils.getNumberOfEpochsBetween(tpstStart, currentTime - 900);

        // Setup id with an existing score below threshold
        Registration registrationWithEE = this.registration.get();
        registrationWithEE.setExposedEpochs(
                Arrays.asList(
                        EpochExposition.builder()
                                .epochId(previousEpoch)
                                .expositionScores(Arrays.asList(3.0))
                                .build(),
                        EpochExposition.builder()
                                .epochId(currentEpochId)
                                .expositionScores(Arrays.asList(4.3))
                                .build()
                )
        );

        // Simulate new exposed epochs
        registrationWithEE.setOutdatedRisk(true);

        this.registrationService.saveRegistration(registrationWithEE);

        byte[] ebid = this.cryptoService.generateEBID(
                new CryptoSkinny64(serverKey), currentEpochId,
                this.registration.get().getPermanentIdentifier()
        );

        byte[] encryptedCountryCode = this.cryptoService
                .encryptCountryCode(new CryptoAESECB(federationKey), ebid, countryCode);

        when(cryptoServerClient.validateContact(any()))
                .thenReturn(
                        ValidateContactResponse
                                .newBuilder()
                                .setIdA(ByteString.copyFrom(this.registration.get().getPermanentIdentifier()))
                                .setCountryCode(
                                        ByteString.copyFrom(
                                                new byte[] { this.serverConfigurationService.getServerCountryCode() }
                                        )
                                )
                                .setEpochId(currentEpochId)
                                .build()
                );

        // Create HELLO message that will make total score exceed threshold
        long t = currentEpochId * this.epochDuration + this.serviceTimeStart + 15L;
        List<HelloMessageDetail> messages = new ArrayList<>();
        messages.add(generateHelloMessageFor(ebid, encryptedCountryCode, t, -78));
        messages.add(generateHelloMessageFor(ebid, encryptedCountryCode, t + 165L, -50));
        messages.add(generateHelloMessageFor(ebid, encryptedCountryCode, t + 300L, -35));

        Contact contact = Contact.builder()
                .ebid(ebid)
                .ecc(encryptedCountryCode)
                .messageDetails(messages)
                .build();

        this.contactService.saveContacts(Arrays.asList(contact));

        assertFalse(CollectionUtils.isEmpty(this.contactService.findAll()));
        assertEquals(1, this.contactService.findAll().size());

        // When
        Contact processedContact = this.contactProcessor.process(contact);

        // Then
        assertNotNull(processedContact);
        contactItemWriter.write(Arrays.asList(processedContact));
        assertTrue(CollectionUtils.isEmpty(this.contactService.findAll()));
        Optional<Registration> expectedRegistration = this.registrationService
                .findById(registrationWithEE.getPermanentIdentifier());
        assertTrue(expectedRegistration.isPresent());
        assertFalse(CollectionUtils.isEmpty(expectedRegistration.get().getExposedEpochs()));
        assertTrue(expectedRegistration.get().getExposedEpochs().size() == 2);
        verify(this.cryptoServerClient, times(1)).validateContact(any());
    }

    @Test
    void testProcessTwoContactsWithAggregatedScoreBelowThresholdDoesNotYieldRiskSucceeds() throws Exception {
        // Given
        this.registration = this.registrationService.createRegistration(ProcessorTestUtils.generateIdA());
        assertTrue(this.registration.isPresent());

        final long tpstStart = this.serverConfigurationService.getServiceTimeStart();
        final long currentTime = TimeUtils.convertUnixMillistoNtpSeconds(new Date().getTime());

        final int currentEpochId = TimeUtils.getNumberOfEpochsBetween(tpstStart, currentTime);

        final int previousEpoch = TimeUtils.getNumberOfEpochsBetween(tpstStart, currentTime - 900);

        // Setup id with an existing score below threshold
        Registration registrationWithEE = this.registration.get();
        registrationWithEE.setExposedEpochs(
                Arrays.asList(
                        EpochExposition.builder()
                                .epochId(previousEpoch)
                                .expositionScores(Arrays.asList(1.0))
                                .build(),
                        EpochExposition.builder()
                                .epochId(currentEpochId)
                                .expositionScores(Arrays.asList(2.3))
                                .build()
                )
        );

        // Simulate new exposed epochs
        registrationWithEE.setOutdatedRisk(true);

        this.registrationService.saveRegistration(registrationWithEE);

        byte[] ebid = this.cryptoService.generateEBID(
                new CryptoSkinny64(serverKey), currentEpochId,
                this.registration.get().getPermanentIdentifier()
        );
        byte[] encryptedCountryCode = this.cryptoService
                .encryptCountryCode(new CryptoAESECB(federationKey), ebid, countryCode);

        when(cryptoServerClient.validateContact(any()))
                .thenReturn(
                        ValidateContactResponse
                                .newBuilder()
                                .setIdA(ByteString.copyFrom(this.registration.get().getPermanentIdentifier()))
                                .setCountryCode(
                                        ByteString.copyFrom(
                                                new byte[] { this.serverConfigurationService.getServerCountryCode() }
                                        )
                                )
                                .setEpochId(currentEpochId)
                                .build()
                );

        // Create HELLO message that will not make total score exceed threshold
        long t = currentEpochId * this.epochDuration + this.serviceTimeStart + 15L;
        List<HelloMessageDetail> messages = new ArrayList<>();
        messages.add(generateHelloMessageFor(ebid, encryptedCountryCode, t, -78));
        messages.add(generateHelloMessageFor(ebid, encryptedCountryCode, t + 165L, -50));

        Contact contact = Contact.builder()
                .ebid(ebid)
                .ecc(encryptedCountryCode)
                .messageDetails(messages)
                .build();

        this.contactService.saveContacts(Arrays.asList(contact));

        assertFalse(CollectionUtils.isEmpty(this.contactService.findAll()));
        assertEquals(1, this.contactService.findAll().size());

        // When
        Contact processedContact = this.contactProcessor.process(contact);

        // Then
        assertNotNull(processedContact);
        contactItemWriter.write(Arrays.asList(processedContact));
        assertTrue(CollectionUtils.isEmpty(this.contactService.findAll()));
        Optional<Registration> expectedRegistration = this.registrationService
                .findById(registrationWithEE.getPermanentIdentifier());
        assertTrue(expectedRegistration.isPresent());
        assertFalse(CollectionUtils.isEmpty(expectedRegistration.get().getExposedEpochs()));
        assertTrue(expectedRegistration.get().getExposedEpochs().size() == 2);

        verify(this.cryptoServerClient, times(1)).validateContact(any());
    }

    @Test
    void testProcessContactWhenHelloMessageTimestampIsExceededFails()
            throws RobertServerCryptoException, RobertScoringException {
        // Given
        this.registration = this.registrationService.createRegistration(ProcessorTestUtils.generateIdA());
        assertTrue(this.registration.isPresent());

        final long tpstStart = this.serverConfigurationService.getServiceTimeStart();
        final long currentTime = TimeUtils.convertUnixMillistoNtpSeconds(new Date().getTime());

        final int currentEpochId = TimeUtils.getNumberOfEpochsBetween(tpstStart, currentTime);

        byte[] ebid = this.cryptoService.generateEBID(
                new CryptoSkinny64(serverKey), currentEpochId,
                this.registration.get().getPermanentIdentifier()
        );

        byte[] encryptedCountryCode = this.cryptoService
                .encryptCountryCode(new CryptoAESECB(federationKey), ebid, countryCode);
        byte[] time = new byte[2];

        byte[] timeOfDevice = new byte[4];
        System.arraycopy(
                ByteUtils.longToBytes(currentTime + this.propertyLoader.getHelloMessageTimeStampTolerance() + 1), 4,
                timeOfDevice, 0, 4
        );

        byte[] timeHelloB = new byte[4];
        System.arraycopy(ByteUtils.longToBytes(currentTime), 4, timeHelloB, 0, 4);

        timeHelloB[0] = (byte) (timeHelloB[0] & 0x00);
        timeHelloB[1] = (byte) (timeHelloB[1] & 0x00);

        int timeReceived = ByteUtils.bytesToInt(timeOfDevice);
        int timeHello = ByteUtils.bytesToInt(timeHelloB);

        byte[] helloMessage = new byte[16];
        System.arraycopy(encryptedCountryCode, 0, helloMessage, 0, encryptedCountryCode.length);
        System.arraycopy(ebid, 0, helloMessage, encryptedCountryCode.length, ebid.length);
        System.arraycopy(time, 0, helloMessage, encryptedCountryCode.length + ebid.length, time.length);

        when(cryptoServerClient.validateContact(any()))
                .thenReturn(
                        ValidateContactResponse
                                .newBuilder()
                                .setIdA(ByteString.copyFrom(this.registration.get().getPermanentIdentifier()))
                                .setCountryCode(
                                        ByteString.copyFrom(
                                                new byte[] { this.serverConfigurationService.getServerCountryCode() }
                                        )
                                )
                                .setEpochId(currentEpochId)
                                .build()
                );

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

        Contact contact = Contact.builder()
                .ebid(ebid)
                .ecc(encryptedCountryCode)
                .messageDetails(Arrays.asList(helloMessageDetail))
                .build();

        this.contactService.saveContacts(Arrays.asList(contact));

        assertFalse(CollectionUtils.isEmpty(this.contactService.findAll()));
        assertEquals(1, this.contactService.findAll().size());

        // When
        Contact processedContact = this.contactProcessor.process(contact);

        // Then
        assertNotNull(processedContact);
        contactItemWriter.write(Arrays.asList(processedContact));
        assertTrue(CollectionUtils.isEmpty(this.contactService.findAll()));
        assertTrue(CollectionUtils.isEmpty(contact.getMessageDetails()));

        verify(this.cryptoServerClient, times(1)).validateContact(any());

        assertFalse(helloMessageDetail.toString().contains(Arrays.toString(mac)));
        assertFalse(helloMessageDetail.toString().contains(Integer.toString(timeHello)));
        assertFalse(helloMessageDetail.toString().contains(Long.toString(Integer.toUnsignedLong(timeReceived))));
    }

    /*
     * Reject HELLO messages if the epoch embedded in the EBID does not match the
     * one accompanying the HELLO message (timestamp when received on device)
     */
    @Test
    void testProcessContactWhenTheEpochsAreDifferentFails()
            throws RobertServerCryptoException, RobertScoringException {
        // Given
        this.registration = this.registrationService.createRegistration(ProcessorTestUtils.generateIdA());
        assertTrue(this.registration.isPresent());

        final long tpstStart = this.serverConfigurationService.getServiceTimeStart();
        final long currentTime = TimeUtils.convertUnixMillistoNtpSeconds(new Date().getTime());

        final int currentEpochId = TimeUtils.getNumberOfEpochsBetween(tpstStart, currentTime);

        byte[] ebid = this.cryptoService.generateEBID(
                new CryptoSkinny64(serverKey), currentEpochId,
                this.registration.get().getPermanentIdentifier()
        );
        byte[] encryptedCountryCode = this.cryptoService
                .encryptCountryCode(new CryptoAESECB(federationKey), ebid, countryCode);
        byte[] time = new byte[2];

        // Get timestamp on 16 bits
        System.arraycopy(ByteUtils.longToBytes(currentTime), 6, time, 0, 2);

        // The timestamps are coherent between each other but not with the epoch
        // embedded in the EBID
        byte[] timeOfDevice = new byte[4];
        long tsDevice = currentTime + this.serverConfigurationService.getEpochDurationSecs() * 2 + 2;
        System.arraycopy(ByteUtils.longToBytes(tsDevice), 4, timeOfDevice, 0, 4);

        byte[] timeHelloB = new byte[4];
        System.arraycopy(ByteUtils.longToBytes(tsDevice - 1), 4, timeHelloB, 0, 4);

        timeHelloB[0] = (byte) (timeHelloB[0] & 0x00);
        timeHelloB[1] = (byte) (timeHelloB[1] & 0x00);

        int timeReceived = ByteUtils.bytesToInt(timeOfDevice);
        int timeHello = ByteUtils.bytesToInt(timeHelloB);

        byte[] helloMessage = new byte[16];
        System.arraycopy(encryptedCountryCode, 0, helloMessage, 0, encryptedCountryCode.length);
        System.arraycopy(ebid, 0, helloMessage, encryptedCountryCode.length, ebid.length);
        System.arraycopy(time, 0, helloMessage, encryptedCountryCode.length + ebid.length, time.length);

        when(cryptoServerClient.validateContact(any()))
                .thenReturn(
                        ValidateContactResponse
                                .newBuilder()
                                .setIdA(ByteString.copyFrom(this.registration.get().getPermanentIdentifier()))
                                .setCountryCode(
                                        ByteString.copyFrom(
                                                new byte[] { this.serverConfigurationService.getServerCountryCode() }
                                        )
                                )
                                .setEpochId(currentEpochId)
                                .build()
                );

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

        Contact contact = Contact.builder()
                .ebid(ebid)
                .ecc(encryptedCountryCode)
                .messageDetails(Arrays.asList(helloMessageDetail))
                .build();

        this.contactService.saveContacts(Arrays.asList(contact));

        assertFalse(CollectionUtils.isEmpty(this.contactService.findAll()));
        assertEquals(1, this.contactService.findAll().size());

        // When
        Contact processedContact = this.contactProcessor.process(contact);

        // Then
        assertNotNull(processedContact);
        contactItemWriter.write(Arrays.asList(processedContact));
        assertTrue(CollectionUtils.isEmpty(this.contactService.findAll()));

        verify(this.cryptoServerClient).validateContact(any());
    }

    @Test
    void testProcessContactWhenOneHelloMessageHasADifferentEpochShouldBeSuccessfullProcessed()
            throws RobertServerCryptoException, RobertScoringException {
        // Given
        this.registration = this.registrationService.createRegistration(ProcessorTestUtils.generateIdA());
        assertTrue(this.registration.isPresent());

        final long tpstStart = this.serverConfigurationService.getServiceTimeStart();
        final long currentTime = TimeUtils.convertUnixMillistoNtpSeconds(new Date().getTime());

        final int currentEpochId = TimeUtils.getNumberOfEpochsBetween(tpstStart, currentTime);

        byte[] ebid = this.cryptoService.generateEBID(
                new CryptoSkinny64(serverKey), currentEpochId,
                this.registration.get().getPermanentIdentifier()
        );
        byte[] encryptedCountryCode = this.cryptoService
                .encryptCountryCode(new CryptoAESECB(federationKey), ebid, countryCode);
        byte[] time = new byte[2];

        // Get timestamp on 16 bits
        System.arraycopy(ByteUtils.longToBytes(currentTime), 6, time, 0, 2);

        // The timestamps are coherent between each other but not with the epoch
        // embedded in the EBID
        byte[] timeOfDevice = new byte[4];
        long tsDevice = currentTime + this.serverConfigurationService.getEpochDurationSecs() * 2 + 2;
        System.arraycopy(ByteUtils.longToBytes(tsDevice), 4, timeOfDevice, 0, 4);

        byte[] timeHelloB = new byte[4];
        System.arraycopy(ByteUtils.longToBytes(tsDevice - 1), 4, timeHelloB, 0, 4);

        timeHelloB[0] = (byte) (timeHelloB[0] & 0x00);
        timeHelloB[1] = (byte) (timeHelloB[1] & 0x00);

        int timeReceived = ByteUtils.bytesToInt(timeOfDevice);
        int timeHello = ByteUtils.bytesToInt(timeHelloB);

        // Accurate time
        tsDevice = currentTime + this.serverConfigurationService.getEpochDurationSecs() + 2;
        byte[] accurateTimeOfDevice = new byte[4];
        System.arraycopy(ByteUtils.longToBytes(tsDevice), 4, accurateTimeOfDevice, 0, 4);
        int accurateTimeReceived = ByteUtils.bytesToInt(accurateTimeOfDevice);

        byte[] timeHelloC = new byte[4];
        System.arraycopy(ByteUtils.longToBytes(tsDevice - 1), 4, timeHelloC, 0, 4);
        int accurateTimeHello = ByteUtils.bytesToInt(timeHelloC);

        byte[] helloMessage = new byte[16];
        System.arraycopy(encryptedCountryCode, 0, helloMessage, 0, encryptedCountryCode.length);
        System.arraycopy(ebid, 0, helloMessage, encryptedCountryCode.length, ebid.length);
        System.arraycopy(time, 0, helloMessage, encryptedCountryCode.length + ebid.length, time.length);

        when(cryptoServerClient.validateContact(any()))
                .thenReturn(
                        ValidateContactResponse
                                .newBuilder()
                                .setIdA(ByteString.copyFrom(this.registration.get().getPermanentIdentifier()))
                                .setCountryCode(
                                        ByteString.copyFrom(
                                                new byte[] { this.serverConfigurationService.getServerCountryCode() }
                                        )
                                )
                                .setEpochId(currentEpochId)
                                .build()
                );

        byte[] mac = this.cryptoService
                .generateMACHello(
                        new CryptoHMACSHA256(getKeyMacFor(this.registration.get().getPermanentIdentifier())),
                        helloMessage
                );

        HelloMessageDetail discardedHelloMessage = HelloMessageDetail.builder()
                .mac(mac)
                .timeFromHelloMessage(timeHello)
                .timeCollectedOnDevice(Integer.toUnsignedLong(timeReceived))
                .rssiCalibrated(-70)
                .build();

        HelloMessageDetail helloMessageDetail = HelloMessageDetail.builder()
                .mac(mac)
                .timeFromHelloMessage(accurateTimeHello)
                .timeCollectedOnDevice(Integer.toUnsignedLong(accurateTimeReceived))
                .rssiCalibrated(-70)
                .build();

        Contact contact = Contact.builder()
                .ebid(ebid)
                .ecc(encryptedCountryCode)
                .messageDetails(Arrays.asList(discardedHelloMessage, helloMessageDetail))
                .build();

        this.contactService.saveContacts(Arrays.asList(contact));

        assertFalse(CollectionUtils.isEmpty(this.contactService.findAll()));
        assertEquals(1, this.contactService.findAll().size());

        // When
        Contact processedContact = this.contactProcessor.process(contact);

        // Then
        Optional<Registration> expectedRegistration = this.registrationService
                .findById(this.registration.get().getPermanentIdentifier());

        assertNotNull(processedContact);
        contactItemWriter.write(Arrays.asList(processedContact));
        assertThat(this.contactService.findAll())
                .as("contacts to process")
                .isEmpty();
        assertThat(expectedRegistration).isPresent();
        assertThat(expectedRegistration.get().getExposedEpochs()).isNotEmpty();
        assertThat(expectedRegistration.get().getExposedEpochs().size()).isEqualTo(1);

        verify(this.cryptoServerClient, times(1)).validateContact(any());
    }

    @Test
    void testProcessContactWhenTheMacIsInvalidFails()
            throws RobertServerCryptoException, RobertScoringException {
        // Given
        this.registration = this.registrationService.createRegistration(ProcessorTestUtils.generateIdA());
        assertTrue(this.registration.isPresent());

        final long tpstStart = this.serverConfigurationService.getServiceTimeStart();
        final long currentTime = TimeUtils.convertUnixMillistoNtpSeconds(new Date().getTime());

        final int currentEpochId = TimeUtils.getNumberOfEpochsBetween(tpstStart, currentTime);

        byte[] ebid = this.cryptoService.generateEBID(
                new CryptoSkinny64(serverKey),
                currentEpochId,
                this.registration.get().getPermanentIdentifier()
        );
        byte[] encryptedCountryCode = this.cryptoService.encryptCountryCode(
                new CryptoAESECB(federationKey),
                ebid,
                countryCode
        );
        byte[] time = new byte[2];

        // Get timestamp on sixteen bits
        System.arraycopy(ByteUtils.longToBytes(currentTime + 902), 6, time, 0, 2);

        byte[] timeOfDevice = new byte[4];
        System.arraycopy(ByteUtils.longToBytes(currentTime), 4, timeOfDevice, 0, 4);

        byte[] timeHelloB = new byte[4];
        System.arraycopy(ByteUtils.longToBytes(currentTime), 4, timeHelloB, 0, 4);

        timeHelloB[0] = (byte) (timeHelloB[0] & 0x00);
        timeHelloB[1] = (byte) (timeHelloB[1] & 0x00);

        int timeReceived = ByteUtils.bytesToInt(timeOfDevice);
        int timeHello = ByteUtils.bytesToInt(timeHelloB);

        byte[] helloMessage = new byte[16];
        System.arraycopy(encryptedCountryCode, 0, helloMessage, 0, encryptedCountryCode.length);
        System.arraycopy(ebid, 0, helloMessage, encryptedCountryCode.length, ebid.length);
        System.arraycopy(time, 0, helloMessage, encryptedCountryCode.length + ebid.length, time.length);

        when(cryptoServerClient.validateContact(any()))
                .thenReturn(
                        ValidateContactResponse
                                .newBuilder()
                                .setIdA(ByteString.copyFrom(this.registration.get().getPermanentIdentifier()))
                                .setCountryCode(
                                        ByteString.copyFrom(
                                                new byte[] { this.serverConfigurationService.getServerCountryCode() }
                                        )
                                )
                                .setEpochId(currentEpochId)
                                .build()
                );

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

        Contact contact = Contact.builder()
                .ebid(ebid)
                .ecc(encryptedCountryCode)
                .messageDetails(Arrays.asList(helloMessageDetail))
                .build();

        this.contactService.saveContacts(Arrays.asList(contact));

        assertFalse(CollectionUtils.isEmpty(this.contactService.findAll()));
        assertEquals(1, this.contactService.findAll().size());

        // When
        Contact processedContact = this.contactProcessor.process(contact);

        // Then
        assertNotNull(processedContact);
        contactItemWriter.write(Arrays.asList(processedContact));
        assertTrue(CollectionUtils.isEmpty(this.contactService.findAll()));

        verify(this.cryptoServerClient).validateContact(any());
    }

    @Test
    void testProcessContactWhenTheContactIsValidSucceeds()
            throws RobertServerCryptoException, RobertScoringException {
        // Given
        this.registration = this.registrationService.createRegistration(ProcessorTestUtils.generateIdA());
        assertTrue(this.registration.isPresent());

        final long tpstStart = this.serverConfigurationService.getServiceTimeStart();
        final long currentTime = TimeUtils.convertUnixMillistoNtpSeconds(new Date().getTime());

        final int currentEpochId = TimeUtils.getNumberOfEpochsBetween(tpstStart, currentTime);

        final int previousEpoch = TimeUtils.getNumberOfEpochsBetween(
                tpstStart,
                currentTime - this.serverConfigurationService.getEpochDurationSecs()
        );

        Registration registrationWithEE = this.registration.get();
        registrationWithEE.setExposedEpochs(
                Arrays.asList(
                        EpochExposition.builder()
                                .epochId(previousEpoch)
                                .expositionScores(Arrays.asList(0.0))
                                .build(),
                        EpochExposition.builder()
                                .epochId(currentEpochId)
                                .expositionScores(Arrays.asList(0.0))
                                .build()
                )
        );

        // Simulate new exposed epochs
        registrationWithEE.setOutdatedRisk(true);

        int nbOfExposedEpochs = registrationWithEE.getExposedEpochs().size();

        this.registrationService.saveRegistration(registrationWithEE);

        byte[] ebid = this.cryptoService.generateEBID(
                new CryptoSkinny64(serverKey),
                currentEpochId,
                this.registration.get().getPermanentIdentifier()
        );
        byte[] encryptedCountryCode = this.cryptoService.encryptCountryCode(
                new CryptoAESECB(federationKey),
                ebid,
                countryCode
        );
        byte[] time = new byte[2];

        // Get timestamp on sixteen bits
        System.arraycopy(ByteUtils.longToBytes(currentTime), 6, time, 0, 2);

        byte[] timeOfDevice = new byte[4];
        System.arraycopy(ByteUtils.longToBytes(currentTime + 1), 4, timeOfDevice, 0, 4);

        byte[] timeHelloB = new byte[4];
        System.arraycopy(ByteUtils.longToBytes(currentTime), 4, timeHelloB, 0, 4);

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

        HelloMessageDetail helloMessageDetail = HelloMessageDetail.builder()
                .mac(mac)
                .timeFromHelloMessage(timeHello)
                .timeCollectedOnDevice(Integer.toUnsignedLong(timeReceived))
                .rssiCalibrated(-70)
                .build();

        Contact contact = Contact.builder()
                .ebid(ebid)
                .ecc(encryptedCountryCode)
                .messageDetails(Arrays.asList(helloMessageDetail))
                .build();

        this.contactService.saveContacts(Arrays.asList(contact));

        var mockedResponse = ValidateContactResponse
                .newBuilder()
                .setIdA(ByteString.copyFrom(this.registration.get().getPermanentIdentifier()))
                .setCountryCode(
                        ByteString.copyFrom(new byte[] { this.serverConfigurationService.getServerCountryCode() })
                )
                .setEpochId(currentEpochId)
                .build();
        when(cryptoServerClient.validateContact(any())).thenReturn(mockedResponse);

        assertFalse(CollectionUtils.isEmpty(this.contactService.findAll()));
        assertEquals(1, this.contactService.findAll().size());

        // When
        Contact processedContact = this.contactProcessor.process(contact);

        // Then
        assertNotNull(processedContact);
        contactItemWriter.write(Arrays.asList(processedContact));
        assertTrue(CollectionUtils.isEmpty(this.contactService.findAll()));
        Optional<Registration> expectedRegistration = this.registrationService
                .findById(registrationWithEE.getPermanentIdentifier());
        assertTrue(expectedRegistration.isPresent());

        assertFalse(CollectionUtils.isEmpty(expectedRegistration.get().getExposedEpochs()));
        assertEquals(nbOfExposedEpochs, expectedRegistration.get().getExposedEpochs().size());

        verify(this.cryptoServerClient, times(1)).validateContact(any());
    }

    @Test
    void testProcessContactSucceedsWhenHasAtLeastOneHelloMessageValid()
            throws RobertServerCryptoException, RobertScoringException {
        // Given
        this.registration = this.registrationService.createRegistration(ProcessorTestUtils.generateIdA());
        assertTrue(this.registration.isPresent());

        final long tpstStart = this.serverConfigurationService.getServiceTimeStart();
        final long currentTime = TimeUtils.convertUnixMillistoNtpSeconds(new Date().getTime());

        final int currentEpochId = TimeUtils.getNumberOfEpochsBetween(tpstStart, currentTime);

        byte[] ebid = this.cryptoService.generateEBID(
                new CryptoSkinny64(serverKey), currentEpochId,
                this.registration.get().getPermanentIdentifier()
        );

        byte[] encryptedCountryCode = this.cryptoService
                .encryptCountryCode(new CryptoAESECB(federationKey), ebid, countryCode);
        byte[] time = new byte[2];

        byte[] timeOfDevice = new byte[4];
        System.arraycopy(
                ByteUtils.longToBytes(currentTime + this.propertyLoader.getHelloMessageTimeStampTolerance() + 1), 4,
                timeOfDevice, 0, 4
        );

        byte[] validTimeOfDevice = new byte[4];
        System.arraycopy(ByteUtils.longToBytes(currentTime + 10), 4, validTimeOfDevice, 0, 4);

        byte[] timeHelloB = new byte[4];
        System.arraycopy(ByteUtils.longToBytes(currentTime), 4, timeHelloB, 0, 4);

        timeHelloB[0] = (byte) (timeHelloB[0] & 0x00);
        timeHelloB[1] = (byte) (timeHelloB[1] & 0x00);

        byte[] timeHelloC = new byte[4];
        System.arraycopy(ByteUtils.longToBytes(currentTime + 1), 4, timeHelloC, 0, 4);

        timeHelloC[0] = (byte) (timeHelloC[0] & 0x00);
        timeHelloC[1] = (byte) (timeHelloC[1] & 0x00);

        int timeReceived = ByteUtils.bytesToInt(timeOfDevice);
        int timeHello = ByteUtils.bytesToInt(timeHelloB);
        int nextTimeHello = ByteUtils.bytesToInt(timeHelloC);

        byte[] helloMessage = new byte[16];
        System.arraycopy(encryptedCountryCode, 0, helloMessage, 0, encryptedCountryCode.length);
        System.arraycopy(ebid, 0, helloMessage, encryptedCountryCode.length, ebid.length);
        System.arraycopy(time, 0, helloMessage, encryptedCountryCode.length + ebid.length, time.length);

        var mockedResponse = ValidateContactResponse
                .newBuilder()
                .setIdA(ByteString.copyFrom(this.registration.get().getPermanentIdentifier()))
                .setCountryCode(
                        ByteString.copyFrom(new byte[] { this.serverConfigurationService.getServerCountryCode() })
                )
                .setEpochId(currentEpochId)
                .build();
        when(cryptoServerClient.validateContact(any())).thenReturn(mockedResponse);

        byte[] mac = this.cryptoService
                .generateMACHello(
                        new CryptoHMACSHA256(getKeyMacFor(this.registration.get().getPermanentIdentifier())),
                        helloMessage
                );

        HelloMessageDetail helloMessageDetailToDiscard = HelloMessageDetail.builder()
                .mac(mac)
                .timeFromHelloMessage(timeHello)
                .timeCollectedOnDevice(Integer.toUnsignedLong(timeReceived))
                .rssiCalibrated(-70)
                .build();

        HelloMessageDetail helloMessageDetail = HelloMessageDetail.builder()
                .mac(mac)
                .timeFromHelloMessage(timeHello)
                .timeCollectedOnDevice(Integer.toUnsignedLong(ByteUtils.bytesToInt(validTimeOfDevice)))
                .rssiCalibrated(-70)
                .build();

        HelloMessageDetail helloMessageDetailC = HelloMessageDetail.builder()
                .mac(mac)
                .timeFromHelloMessage(nextTimeHello)
                .timeCollectedOnDevice(Integer.toUnsignedLong(ByteUtils.bytesToInt(validTimeOfDevice)))
                .rssiCalibrated(-20)
                .build();

        Contact contact = Contact.builder()
                .ebid(ebid)
                .ecc(encryptedCountryCode)
                .messageDetails(Arrays.asList(helloMessageDetailToDiscard, helloMessageDetail, helloMessageDetailC))
                .build();

        this.contactService.saveContacts(Arrays.asList(contact));

        assertFalse(CollectionUtils.isEmpty(this.contactService.findAll()));
        assertEquals(1, this.contactService.findAll().size());

        // When
        Contact processedContact = this.contactProcessor.process(contact);

        // Then
        Optional<Registration> expectedRegistration = this.registrationService
                .findById(this.registration.get().getPermanentIdentifier());
        assertNotNull(processedContact);
        contactItemWriter.write(Arrays.asList(processedContact));
        assertTrue(CollectionUtils.isEmpty(this.contactService.findAll()));
        assertTrue(expectedRegistration.isPresent());
        assertFalse(CollectionUtils.isEmpty(expectedRegistration.get().getExposedEpochs()));
        assertEquals(expectedRegistration.get().getExposedEpochs().size(), 1);

        verify(this.cryptoServerClient, times(1)).validateContact(any());
    }

    @Test
    void testProcessContactWhenTheRegistrationHasTooOldExposedEpochsFails()
            throws RobertServerCryptoException, RobertScoringException {
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
        registrationWithEE.setExposedEpochs(
                Arrays.asList(
                        EpochExposition
                                .builder()
                                .epochId(tooOldEpochId)
                                .expositionScores(Arrays.asList(0.0))
                                .build()
                )
        );

        // Simulate new exposed epochs
        registrationWithEE.setOutdatedRisk(true);

        this.registrationService.saveRegistration(registrationWithEE);

        byte[] ebid = this.cryptoService.generateEBID(
                new CryptoSkinny64(serverKey), currentEpochId,
                this.registration.get().getPermanentIdentifier()
        );
        byte[] encryptedCountryCode = this.cryptoService
                .encryptCountryCode(new CryptoAESECB(federationKey), ebid, countryCode);
        byte[] time = new byte[2];

        // Get timestamp on 16 bits
        System.arraycopy(ByteUtils.longToBytes(currentTime), 6, time, 0, 2);

        byte[] timeOfDevice = new byte[4];
        System.arraycopy(ByteUtils.longToBytes(currentTime + 1), 4, timeOfDevice, 0, 4);

        byte[] timeHelloB = new byte[4];
        System.arraycopy(ByteUtils.longToBytes(currentTime), 4, timeHelloB, 0, 4);

        timeHelloB[0] = (byte) (timeHelloB[0] & 0x00);
        timeHelloB[1] = (byte) (timeHelloB[1] & 0x00);

        int timeReceived = ByteUtils.bytesToInt(timeOfDevice);
        int timeHello = ByteUtils.bytesToInt(timeHelloB);

        byte[] helloMessage = new byte[16];
        System.arraycopy(encryptedCountryCode, 0, helloMessage, 0, encryptedCountryCode.length);
        System.arraycopy(ebid, 0, helloMessage, encryptedCountryCode.length, ebid.length);
        System.arraycopy(time, 0, helloMessage, encryptedCountryCode.length + ebid.length, time.length);

        when(cryptoServerClient.validateContact(any()))
                .thenReturn(
                        ValidateContactResponse
                                .newBuilder()
                                .setIdA(ByteString.copyFrom(this.registration.get().getPermanentIdentifier()))
                                .setCountryCode(
                                        ByteString.copyFrom(
                                                new byte[] { this.serverConfigurationService.getServerCountryCode() }
                                        )
                                )
                                .setEpochId(currentEpochId)
                                .build()
                );

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

        Contact contact = Contact.builder()
                .ebid(ebid)
                .ecc(encryptedCountryCode)
                .messageDetails(Arrays.asList(helloMessageDetail))
                .build();

        this.contactService.saveContacts(Arrays.asList(contact));

        assertFalse(CollectionUtils.isEmpty(this.contactService.findAll()));
        assertEquals(1, this.contactService.findAll().size());

        // When
        Contact processedContact = this.contactProcessor.process(contact);

        // Then
        Optional<Registration> expectedRegistration = this.registrationService
                .findById(registrationWithEE.getPermanentIdentifier());
        assertNotNull(processedContact);
        contactItemWriter.write(Arrays.asList(processedContact));
        assertTrue(CollectionUtils.isEmpty(this.contactService.findAll()));
        assertTrue(expectedRegistration.isPresent());
        assertFalse(CollectionUtils.isEmpty(expectedRegistration.get().getExposedEpochs()));
        assertThat(expectedRegistration.get().getExposedEpochs().size()).isEqualTo(2);

        verify(this.cryptoServerClient, times(1)).validateContact(any());
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
}
