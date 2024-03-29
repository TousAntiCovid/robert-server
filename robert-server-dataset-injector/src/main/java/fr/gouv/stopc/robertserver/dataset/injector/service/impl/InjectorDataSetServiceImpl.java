package fr.gouv.stopc.robertserver.dataset.injector.service.impl;

import fr.gouv.stopc.robert.crypto.grpc.server.storage.cryptographic.service.ICryptographicStorageService;
import fr.gouv.stopc.robert.crypto.grpc.server.storage.database.model.ClientIdentifier;
import fr.gouv.stopc.robert.crypto.grpc.server.storage.database.repository.ClientIdentifierRepository;
import fr.gouv.stopc.robert.server.common.service.IServerConfigurationService;
import fr.gouv.stopc.robert.server.common.utils.ByteUtils;
import fr.gouv.stopc.robert.server.common.utils.TimeUtils;
import fr.gouv.stopc.robert.server.crypto.service.CryptoService;
import fr.gouv.stopc.robert.server.crypto.structure.impl.CryptoAESECB;
import fr.gouv.stopc.robert.server.crypto.structure.impl.CryptoHMACSHA256;
import fr.gouv.stopc.robert.server.crypto.structure.impl.CryptoSkinny64;
import fr.gouv.stopc.robertserver.database.model.Contact;
import fr.gouv.stopc.robertserver.database.model.EpochExposition;
import fr.gouv.stopc.robertserver.database.model.HelloMessageDetail;
import fr.gouv.stopc.robertserver.database.model.Registration;
import fr.gouv.stopc.robertserver.database.service.ContactService;
import fr.gouv.stopc.robertserver.database.service.IRegistrationService;
import fr.gouv.stopc.robertserver.dataset.injector.service.GeneratorIdService;
import fr.gouv.stopc.robertserver.dataset.injector.service.InjectorDataSetService;
import fr.gouv.stopc.robertserver.dataset.injector.utils.PropertyLoader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.*;
import java.util.stream.IntStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class InjectorDataSetServiceImpl implements InjectorDataSetService {

    @Autowired
    private ContactService contactService;

    @Autowired
    private IRegistrationService registrationService;

    @Autowired
    private IServerConfigurationService serverConfigurationService;

    @Autowired
    private CryptoService cryptoService;

    @Autowired
    private ICryptographicStorageService cryptographicStorageService;

    @Autowired
    private ClientIdentifierRepository clientIdentifierRepository;

    @Value("${robert.injector.contact-per-registration:1}")
    private int contactPerRegistration;

    @Autowired
    private GeneratorIdService generatorIdService;

    private byte[] serverKey;

    private Key federationKey;

    private byte countryCode;

    private long epochDuration;

    private long serviceTimeStart;

    private final PropertyLoader propertyLoader;

    @Override
    public void injectContacts(int contactCount) {
        long time = getCurrentTimeNTPSeconds();
        int epochId = TimeUtils.getNumberOfEpochsBetween(this.serverConfigurationService.getServiceTimeStart(), time);
        cryptographicStorageService.init(
                propertyLoader.getKeyStorePassword(),
                propertyLoader.getKeyStoreConfigFile(),
                propertyLoader.getKeystoreType(),
                propertyLoader.getKeystoreFile()
        );
        serverKey = cryptographicStorageService
                .getServerKey(epochId, serverConfigurationService.getServiceTimeStart(), false);
        federationKey = cryptographicStorageService.getFederationKey();

        countryCode = this.serverConfigurationService.getServerCountryCode();
        epochDuration = this.serverConfigurationService.getEpochDurationSecs();
        serviceTimeStart = this.serverConfigurationService.getServiceTimeStart();

        IntStream.range(0, contactCount / contactPerRegistration).parallel().forEach(
                nbr -> {
                    try {

                        contactService.saveContacts(buildContacts());
                    } catch (Exception e) {
                        log.error("Can not create the list of contact #" + nbr, e);
                    }
                    ;
                }
        );

    }

    @Override
    public void injectRegistrations(int registrationCount) {
        IntStream.range(0, registrationCount).parallel().forEach(
                nbr -> {
                    registrationService.saveRegistration(buildRegistration());
                }
        );

    }

    private Registration buildRegistration() {
        final long tpstStart = this.serverConfigurationService.getServiceTimeStart();
        final long currentTime = TimeUtils.convertUnixMillistoNtpSeconds(new Date().getTime());

        final int currentEpochId = TimeUtils.getNumberOfEpochsBetween(tpstStart, currentTime);

        final int previousEpoch = TimeUtils.getNumberOfEpochsBetween(tpstStart, currentTime - 3600 * 24 * 15);

        // Setup id with an existing score below threshold
        Optional<Registration> registration = registrationService.createRegistration(generatorIdService.generateIdA());
        if (!registration.isPresent()) {
            log.error("Unable to create a registration");
        }

        Registration registrationWithEE = registration.get();
        registrationWithEE.setExposedEpochs(
                Arrays.asList(
                        EpochExposition.builder()
                                .epochId(previousEpoch)
                                .expositionScores(Collections.singletonList(800.0))
                                .build(),
                        EpochExposition.builder()
                                .epochId(currentEpochId)
                                .expositionScores(Collections.singletonList(800.0))
                                .build()
                )
        );

        return registrationWithEE;
    }

    private List<Contact> buildContacts() throws Exception {

        List<Contact> contacts = new ArrayList<>();

        final long tpstStart = this.serverConfigurationService.getServiceTimeStart();
        final long currentTime = TimeUtils.convertUnixMillistoNtpSeconds(new Date().getTime());
        final int previousEpoch = TimeUtils.getNumberOfEpochsBetween(tpstStart, currentTime - 900);
        final int currentEpochId = TimeUtils.getNumberOfEpochsBetween(tpstStart, currentTime);

        // generate a idA
        byte[] permanentIdentifier = generatorIdService.generateIdA();

        // save the associated registration
        Registration registration = Registration.builder()
                .permanentIdentifier(permanentIdentifier)
                .build();
        registration.setExposedEpochs(
                Arrays.asList(
                        EpochExposition.builder()
                                .epochId(previousEpoch)
                                .expositionScores(Collections.singletonList(800.0))
                                .build(),
                        EpochExposition.builder()
                                .epochId(currentEpochId)
                                .expositionScores(Collections.singletonList(800.0))
                                .build()
                )
        );

        registrationService.saveRegistration(registration);

        // retrieve the key_for_mac from crypto server
        Optional<ClientIdentifier> clientId = clientIdentifierRepository
                .findByIdA(Base64.getEncoder().encodeToString(permanentIdentifier));

        Key clientKek = this.cryptographicStorageService.getKeyForEncryptingClientKeys();
        if (Objects.isNull(clientKek)) {
            log.error("The clientKek to decrypt the client keys is null.");
            return contacts;
        }

        if (!clientId.isPresent()) {
            log.error("Could not find id in clientIdentifier DB table.");
            return contacts;
        }

        for (int i = 0; i < contactPerRegistration; i++) {
            ClientIdentifier clientIdentifier = clientId.get();
            byte[] decryptedKeyForMac = generatorIdService
                    .decryptStoredKeyWithAES256GCMAndKek(
                            Base64.getDecoder().decode(clientIdentifier.getKeyForMac()), clientKek
                    );

            byte[] ebid = this.cryptoService
                    .generateEBID(new CryptoSkinny64(serverKey), currentEpochId, permanentIdentifier);

            byte[] encryptedCountryCode = this.cryptoService
                    .encryptCountryCode(new CryptoAESECB(federationKey), ebid, countryCode);

            // Create HELLO message that will make total score exceed threshold
            long t = currentEpochId * this.epochDuration + this.serviceTimeStart + 15L;
            List<HelloMessageDetail> messages = new ArrayList<>();
            Random r = new Random();
            for (int j = 0; j <= 65; j++) {
                int rssi = -r.nextInt(80);
                messages.add(generateHelloMessageFor(decryptedKeyForMac, ebid, encryptedCountryCode, t + j, rssi));
            }

            contacts.add(
                    Contact.builder()
                            .ebid(ebid)
                            .ecc(encryptedCountryCode)
                            .messageDetails(messages)
                            .build()
            );
        }

        return contacts;
    }

    private HelloMessageDetail generateHelloMessageFor(byte[] decryptedKeyForMac, byte[] ebid,
            byte[] encryptedCountryCode, long t, int rssi) throws Exception {
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
                .generateMACHello(new CryptoHMACSHA256(decryptedKeyForMac), helloMessage);

        return HelloMessageDetail.builder()
                .timeFromHelloMessage(timeHello)
                .timeCollectedOnDevice(Integer.toUnsignedLong(timeReceived))
                .rssiCalibrated(rssi)
                .mac(mac)
                .build();
    }

    private long getCurrentTimeNTPSeconds() {
        return System.currentTimeMillis() / 1000 + 2208988800L;
    }
}
