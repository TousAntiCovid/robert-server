package fr.gouv.stopc.robert.server.batch.service;

import fr.gouv.stopc.robert.server.batch.configuration.PropertyLoader;
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
import fr.gouv.stopc.robertserver.database.model.EpochExposition;
import fr.gouv.stopc.robertserver.database.model.HelloMessageDetail;
import fr.gouv.stopc.robertserver.database.model.Registration;
import fr.gouv.stopc.robertserver.database.service.ContactService;
import fr.gouv.stopc.robertserver.database.service.IRegistrationService;
import org.springframework.stereotype.Component;

import javax.crypto.spec.SecretKeySpec;

import java.security.Key;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Component
public class TestContext {

    private final IRegistrationService registrationService;

    private final IServerConfigurationService serverConfigurationService;

    private final CryptoService cryptoService;

    private final ContactService contactService;

    private final PropertyLoader propertyLoader;

    private final byte[] serverKey;

    private final Key federationKey;

    public final byte countryCode;

    private final long tpstStart;

    private final long currentTime;

    public int currentEpochId;

    private final int previousEpoch;

    private final long epochDuration;

    private final long serviceTimeStart;

    public TestContext(IRegistrationService registrationService,
            IServerConfigurationService serverConfigurationService,
            CryptoService cryptoService,
            ContactService contactService,
            PropertyLoader propertyLoader) {
        this.registrationService = registrationService;
        this.serverConfigurationService = serverConfigurationService;
        this.cryptoService = cryptoService;
        this.contactService = contactService;
        this.propertyLoader = propertyLoader;

        this.serverKey = this.generateKey(24);
        this.federationKey = new SecretKeySpec(this.generateKey(32), CryptoAES.AES_ENCRYPTION_KEY_SCHEME);
        this.countryCode = this.serverConfigurationService.getServerCountryCode();
        this.tpstStart = this.serverConfigurationService.getServiceTimeStart();
        this.epochDuration = this.serverConfigurationService.getEpochDurationSecs();
        this.serviceTimeStart = this.serverConfigurationService.getServiceTimeStart();

        this.currentTime = TimeUtils.convertUnixMillistoNtpSeconds(new Date().getTime());
        this.currentEpochId = TimeUtils.getNumberOfEpochsBetween(tpstStart, currentTime);
        this.previousEpoch = TimeUtils.getNumberOfEpochsBetween(tpstStart, currentTime - 900);

    }

    private byte[] generateKey(int sizeInBytes) {
        byte[] data = new byte[sizeInBytes];
        new SecureRandom().nextBytes(data);
        return data;
    }

    public Registration acceptableRegistrationWithExistingScoreAboveThreshold() {
        var registration = this.registrationService.createRegistration(ProcessorTestUtils.generateIdA());
        assertTrue(registration.isPresent());

        // Setup id with an existing score below threshold
        var registrationWithEE = registration.get();
        registrationWithEE.setExposedEpochs(
                Arrays.asList(
                        EpochExposition.builder()
                                .epochId(previousEpoch)
                                .expositionScores(Arrays.asList(20.0))
                                .build(),
                        EpochExposition.builder()
                                .epochId(currentEpochId)
                                .expositionScores(Arrays.asList(2.0))
                                .build()
                )
        );

        // Simulate new exposed epochs
        registrationWithEE.setOutdatedRisk(true);

        this.registrationService.saveRegistration(registrationWithEE);
        return registrationWithEE;
    }

    public Registration acceptableRegistrationWithExistingScoreEqualToZero() {
        var registration = this.registrationService.createRegistration(ProcessorTestUtils.generateIdA());
        assertTrue(registration.isPresent());

        // Setup id with an existing score below threshold
        var registrationWithEE = registration.get();
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

        this.registrationService.saveRegistration(registrationWithEE);
        return registrationWithEE;
    }

    public Registration acceptableRegistrationWithExistingScoreBelowThreshold() {
        var registration = this.registrationService.createRegistration(ProcessorTestUtils.generateIdA());
        assertTrue(registration.isPresent());

        // Setup id with an existing score below threshold
        var registrationWithEE = registration.get();
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
        return registrationWithEE;
    }

    public Registration acceptableRegistration() {
        var registration = this.registrationService.createRegistration(ProcessorTestUtils.generateIdA());
        assertTrue(registration.isPresent());

        return registration.get();
    }

    public Contact addBadHelloMessageWithExcedeedTimeToleranceToContact(Contact contact, byte[] permanentIdentifier)
            throws RobertServerCryptoException {
        byte[] ebid = this.cryptoService.generateEBID(
                new CryptoSkinny64(serverKey),
                currentEpochId,
                permanentIdentifier
        );
        byte[] encryptedCountryCode = this.cryptoService.encryptCountryCode(
                new CryptoAESECB(federationKey),
                ebid,
                countryCode
        );

        List<HelloMessageDetail> helloMessageDetails = contact.getMessageDetails();
        helloMessageDetails.add(
                generateBadHelloMessageWithExceededTimeStampTolerance(permanentIdentifier, ebid, encryptedCountryCode)
        );
        contact.setMessageDetails(helloMessageDetails);
        // Delete because get/upgate does not exist in service
        this.contactService.deleteAll();
        this.contactService.saveContacts(Arrays.asList(contact));
        return contact;
    }

    public Contact addHelloMessagesWithTotalScoreUpperThanThreshold(Contact contact, byte[] permanentIdentifier)
            throws Exception {
        byte[] ebid = this.cryptoService.generateEBID(
                new CryptoSkinny64(serverKey),
                currentEpochId,
                permanentIdentifier
        );
        byte[] encryptedCountryCode = this.cryptoService.encryptCountryCode(
                new CryptoAESECB(federationKey),
                ebid,
                countryCode
        );

        long t = currentEpochId * this.epochDuration + this.serviceTimeStart + 15L;
        List<HelloMessageDetail> helloMessageDetails = contact.getMessageDetails();
        helloMessageDetails.add(generateHelloMessageFor(permanentIdentifier, ebid, encryptedCountryCode, t, -78));
        helloMessageDetails
                .add(generateHelloMessageFor(permanentIdentifier, ebid, encryptedCountryCode, t + 165L, -50));
        helloMessageDetails
                .add(generateHelloMessageFor(permanentIdentifier, ebid, encryptedCountryCode, t + 300L, -35));
        contact.setMessageDetails(helloMessageDetails);
        // Delete because get/upgate does not exist in service
        this.contactService.deleteAll();
        this.contactService.saveContacts(Arrays.asList(contact));
        return contact;
    }

    public Contact addValidHelloMessage(Contact contact, byte[] permanentIdentifier)
            throws RobertServerCryptoException {
        byte[] ebid = this.cryptoService.generateEBID(
                new CryptoSkinny64(serverKey),
                currentEpochId,
                permanentIdentifier
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
                        new CryptoHMACSHA256(getKeyMac()),
                        helloMessage
                );

        List<HelloMessageDetail> helloMessageDetails = contact.getMessageDetails();
        HelloMessageDetail helloMessageDetail = HelloMessageDetail.builder()
                .mac(mac)
                .timeFromHelloMessage(timeHello)
                .timeCollectedOnDevice(Integer.toUnsignedLong(timeReceived))
                .rssiCalibrated(-70)
                .build();
        helloMessageDetails.add(helloMessageDetail);
        contact.setMessageDetails(helloMessageDetails);
        this.contactService.deleteAll();
        this.contactService.saveContacts(Arrays.asList(contact));
        return contact;
    }

    public Contact addHelloMessageExcededTimestamptolerance(Contact contact, byte[] permanentIdentifier)
            throws RobertServerCryptoException {
        byte[] ebid = this.cryptoService.generateEBID(
                new CryptoSkinny64(serverKey),
                currentEpochId,
                permanentIdentifier
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

        byte[] mac = this.cryptoService
                .generateMACHello(
                        new CryptoHMACSHA256(getKeyMac()),
                        helloMessage
                );

        List<HelloMessageDetail> helloMessageDetails = contact.getMessageDetails();
        HelloMessageDetail helloMessageDetail = HelloMessageDetail.builder()
                .mac(mac)
                .timeFromHelloMessage(timeHello)
                .timeCollectedOnDevice(Integer.toUnsignedLong(timeReceived))
                .rssiCalibrated(-70)
                .build();
        helloMessageDetails.add(helloMessageDetail);
        contact.setMessageDetails(helloMessageDetails);
        this.contactService.deleteAll();
        this.contactService.saveContacts(Arrays.asList(contact));
        return contact;
    }

    public Contact addHelloMessageWithBadEbidCoherence(Contact contact, byte[] permanentIdentifier)
            throws RobertServerCryptoException {
        byte[] ebid = this.cryptoService.generateEBID(
                new CryptoSkinny64(serverKey),
                currentEpochId,
                permanentIdentifier
        );
        byte[] encryptedCountryCode = this.cryptoService.encryptCountryCode(
                new CryptoAESECB(federationKey),
                ebid,
                countryCode
        );
        byte[] time = new byte[2];

        // Get timestamp on sixteen bits
        System.arraycopy(ByteUtils.longToBytes(currentTime), 6, time, 0, 2);

        // The timestamps are coherent between each other but not with the epoch
        // embedded in the EBID
        byte[] timeOfDevice = new byte[4];
        long tsDevice = currentTime + this.serverConfigurationService.getEpochDurationSecs() * 2L + 2;
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

        byte[] mac = this.cryptoService
                .generateMACHello(
                        new CryptoHMACSHA256(getKeyMac()),
                        helloMessage
                );

        List<HelloMessageDetail> helloMessageDetails = contact.getMessageDetails();
        HelloMessageDetail helloMessageDetail = HelloMessageDetail.builder()
                .mac(mac)
                .timeFromHelloMessage(timeHello)
                .timeCollectedOnDevice(Integer.toUnsignedLong(timeReceived))
                .rssiCalibrated(-70)
                .build();
        helloMessageDetails.add(helloMessageDetail);
        contact.setMessageDetails(helloMessageDetails);
        this.contactService.deleteAll();
        this.contactService.saveContacts(Arrays.asList(contact));
        return contact;
    }

    public Contact addHelloMessagesWithTotalScoreUnderThreshold(Contact contact, byte[] permanentIdentifier)
            throws Exception {
        byte[] ebid = this.cryptoService.generateEBID(
                new CryptoSkinny64(serverKey),
                currentEpochId,
                permanentIdentifier
        );
        byte[] encryptedCountryCode = this.cryptoService.encryptCountryCode(
                new CryptoAESECB(federationKey),
                ebid,
                countryCode
        );

        long t = currentEpochId * this.epochDuration + this.serviceTimeStart + 15L;
        List<HelloMessageDetail> helloMessageDetails = contact.getMessageDetails();
        helloMessageDetails.add(generateHelloMessageFor(permanentIdentifier, ebid, encryptedCountryCode, t, -78));
        helloMessageDetails
                .add(generateHelloMessageFor(permanentIdentifier, ebid, encryptedCountryCode, t + 165L, -50));
        contact.setMessageDetails(helloMessageDetails);
        // Delete because get/update does not exist in service
        this.contactService.deleteAll();
        this.contactService.saveContacts(Arrays.asList(contact));
        return contact;
    }

    public Contact contactWithBadCountryCode(byte[] permanentIdentifier) throws Exception {
        final long tpstStart = this.serverConfigurationService.getServiceTimeStart();
        final long currentTime = TimeUtils.convertUnixMillistoNtpSeconds(new Date().getTime());
        final int currentEpochId = TimeUtils.getNumberOfEpochsBetween(tpstStart, currentTime);
        byte[] ebid = this.cryptoService
                .generateEBID(new CryptoSkinny64(serverKey), currentEpochId, ProcessorTestUtils.generateIdA());

        // Create a fake Encrypted Country Code (ECC)
        byte[] encryptedCountryCode = new byte[] { (byte) 0xff };
        var contact = Contact.builder()
                .ebid(new byte[8])
                .ecc(encryptedCountryCode)
                .messageDetails(acceptableHelloMessages(permanentIdentifier, ebid, encryptedCountryCode))
                .build();
        this.contactService.saveContacts(Arrays.asList(contact));
        return contact;
    }

    public Contact generateAcceptableContactForRegistration(Registration registration) throws Exception {
        byte[] ebid = this.cryptoService.generateEBID(
                new CryptoSkinny64(serverKey),
                currentEpochId,
                registration.getPermanentIdentifier()
        );
        byte[] encryptedCountryCode = this.cryptoService.encryptCountryCode(
                new CryptoAESECB(federationKey),
                ebid,
                countryCode
        );

        Contact contact = Contact.builder()
                .ebid(ebid)
                .ecc(encryptedCountryCode)
                .messageDetails(
                        acceptableHelloMessages(registration.getPermanentIdentifier(), ebid, encryptedCountryCode)
                )
                .build();

        this.contactService.saveContacts(Arrays.asList(contact));

        return contact;
    }

    public Contact acceptableContactWithoutHelloMessage(Registration registration) throws Exception {
        byte[] ebid = this.cryptoService.generateEBID(
                new CryptoSkinny64(serverKey),
                currentEpochId,
                registration.getPermanentIdentifier()
        );
        byte[] encryptedCountryCode = this.cryptoService.encryptCountryCode(
                new CryptoAESECB(federationKey),
                ebid,
                countryCode
        );

        Contact contact = Contact.builder()
                .ebid(ebid)
                .ecc(encryptedCountryCode)
                .messageDetails(new ArrayList<>())
                .build();

        this.contactService.saveContacts(Arrays.asList(contact));

        return contact;
    }

    private List<HelloMessageDetail> acceptableHelloMessages(byte[] permanentIdentifier, byte[] ebid,
            byte[] encryptedCountryCode) throws Exception {
        long t = currentEpochId * this.epochDuration + this.serviceTimeStart + 15L;
        List<HelloMessageDetail> messages = new ArrayList<>();
        messages.add(generateHelloMessageFor(permanentIdentifier, ebid, encryptedCountryCode, t, -78));
        messages.add(generateHelloMessageFor(permanentIdentifier, ebid, encryptedCountryCode, t + 165L, -50));
        messages.add(generateHelloMessageFor(permanentIdentifier, ebid, encryptedCountryCode, t + 300L, -35));
        return messages;
    }

    private HelloMessageDetail generateBadHelloMessageWithExceededTimeStampTolerance(byte[] permanentIdentifier,
            byte[] ebid,
            byte[] encryptedCountryCode) throws RobertServerCryptoException {
        byte[] timeOfDeviceExceded = new byte[4];
        var exceededTime = ByteUtils
                .longToBytes(currentTime + this.propertyLoader.getHelloMessageTimeStampTolerance() + 1);
        System.arraycopy(
                exceededTime, 4,
                timeOfDeviceExceded, 0, 4
        );
        long exceededTimeReceived = Integer.toUnsignedLong(
                ByteUtils.bytesToInt(timeOfDeviceExceded)
        );

        byte[] timeHelloB = new byte[4];
        System.arraycopy(ByteUtils.longToBytes(currentTime), 4, timeHelloB, 0, 4);
        int timeHello = ByteUtils.bytesToInt(timeHelloB);

        byte[] helloMessage = new byte[16];
        System.arraycopy(encryptedCountryCode, 0, helloMessage, 0, encryptedCountryCode.length);
        System.arraycopy(ebid, 0, helloMessage, encryptedCountryCode.length, ebid.length);

        byte[] time = new byte[2];
        System.arraycopy(time, 0, helloMessage, encryptedCountryCode.length + ebid.length, time.length);

        byte[] mac = this.cryptoService
                .generateMACHello(
                        new CryptoHMACSHA256(getKeyMac()),
                        helloMessage
                );

        return HelloMessageDetail.builder()
                .mac(mac)
                .timeFromHelloMessage(timeHello)
                .timeCollectedOnDevice(exceededTimeReceived)
                .rssiCalibrated(-70)
                .build();
    }

    private HelloMessageDetail generateHelloMessageFor(byte[] permanentIdentifier, byte[] ebid,
            byte[] encryptedCountryCode, long t, int rssi)
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
                        new CryptoHMACSHA256(getKeyMac()),
                        helloMessage
                );

        return HelloMessageDetail.builder()
                .timeFromHelloMessage(timeHello)
                .timeCollectedOnDevice(Integer.toUnsignedLong(timeReceived))
                .rssiCalibrated(rssi)
                .mac(mac)
                .build();
    }

    private byte[] getKeyMac() {
        return ProcessorTestUtils.generateRandomByteArrayOfSize(32);
    }
}
