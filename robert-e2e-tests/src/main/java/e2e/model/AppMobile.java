package e2e.model;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import e2e.crypto.ByteUtils;
import e2e.crypto.CryptoAESGCM;
import e2e.crypto.CryptoHMACSHA256;
import e2e.crypto.TimeUtils;
import e2e.crypto.exception.RobertServerCryptoException;
import e2e.dto.RegisterSuccessResponse;
import e2e.enums.DigestSaltEnum;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.validation.constraints.NotNull;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import static e2e.crypto.EcdhUtils.deriveKeysFromBackendPublicKey;
import static e2e.crypto.EcdhUtils.generateKeyPair;

@Slf4j
public class AppMobile {

    @Getter
    private final String captchaId;

    private final String captcha;

    private final String robertPublicKey;

    private long timestart;

    @Getter(AccessLevel.PRIVATE)
    List<Contact> contacts;

    @Getter(AccessLevel.PRIVATE)
    List<EphemeralTupleJson> decodedTuples;

    @Getter(AccessLevel.PRIVATE)
    Optional<ClientIdentifierBundle> clientIdentifierBundleWithPublicKey;

    KeyPair keyPair;

    public AppMobile(String captchaId, String captcha, String robertPublicKey)
            throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, RobertServerCryptoException {
        this.captcha = captcha;
        this.captchaId = captchaId;
        this.robertPublicKey = robertPublicKey;
        this.keyPair = generateKeyPair();
        this.contacts = new ArrayList<>();
        generateKeyForTuples();

    }

    private void generateKeyForTuples() throws RobertServerCryptoException {
        this.clientIdentifierBundleWithPublicKey = deriveKeysFromBackendPublicKey(
                Base64.getDecoder().decode(this.robertPublicKey), keyPair
        );
    }

    public String getPublicKey() {
        return Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
    }

    public void decryptRegisterResponse(@NotNull RegisterSuccessResponse registerData)
            throws RobertServerCryptoException {
        this.timestart = registerData.getTimeStart();
        updateTuples(registerData.getTuples());
    }

    private void updateTuples(byte[] encryptedTuples) throws RobertServerCryptoException {
        CryptoAESGCM aesGcm = new CryptoAESGCM(clientIdentifierBundleWithPublicKey.get().getKeyForTuples());
        try {
            byte[] decryptedTuples = aesGcm.decrypt(encryptedTuples);
            ObjectMapper objectMapper = new ObjectMapper();
            this.decodedTuples = objectMapper.readValue(
                    decryptedTuples,
                    new TypeReference<>() {
                    }
            );
        } catch (IOException | RobertServerCryptoException e) {
            throw new RobertServerCryptoException(e);
        }
    }

    public void generateHelloMessageDuring(AppMobile appMobile,
            List<String> limitedAppMobileIds,
            Map<String, AppMobile> appMobileMap,
            Integer durationOfExchangeInMin) {
        log.debug(
                "Generate hello message between {} and other AppMobile during {} min",
                appMobile.getCaptchaId(),
                durationOfExchangeInMin
        );

        List<AppMobile> otherApps = limitedAppMobileIds.stream().map(appMobileMap::get)
                .collect(Collectors.toList());

        long initialTime = System.currentTimeMillis();
        for (int i = 0; i < durationOfExchangeInMin; i++) {
            // On ajoute une minute à la date d'échange
            // NOTE : En théorie il faut à peu près 40 min de contact et 1 message par
            // seconde
            // Mais en configurant scoringThreshold = "0.0001"
            // cela devrait déclencher le calcul comme "risque" dès le premier échange.
            initialTime += (1 * 60 * 1000);

            // On crée les helloMessages
            this.exchangeEbIdWithRand(otherApps, initialTime);
        }
    }

    private EphemeralTupleJson getCurrentTuple() {
        int currentEpoch = TimeUtils.getCurrentEpochFrom(timestart);

        EphemeralTupleJson tuple = decodedTuples.stream()
                .filter(e -> e.getEpochId() == currentEpoch)
                .collect(Collectors.toList())
                .get(0);

        log.debug("For epoch {}, tuple of app-mobile {} is {} ", currentEpoch, captchaId, tuple.toString());

        return tuple;

    }

    public void exchangeEbIdWithRand(List<AppMobile> otherAppMobileList, long timeInMillis) {

        for (AppMobile otherAppMobile : otherAppMobileList) {
            log.debug(
                    "exchange hello message between {} and {} mobile applications", this.captchaId,
                    otherAppMobile.captchaId
            );

            HelloMessageDetail helloMessageDetail = generateHelloMessage(
                    otherAppMobile,
                    TimeUtils.convertUnixMillistoNtpSeconds(timeInMillis),
                    ThreadLocalRandom.current().nextInt(-10, 3)
            );

            saveHelloMessage(
                    otherAppMobile.getCurrentTuple().getKey().getEcc(),
                    otherAppMobile.getCurrentTuple().getKey().getEbid(),
                    helloMessageDetail
            );
        }

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

        int timeReceived = ByteUtils.bytesToInt(timeOfDevice);
        int timeHello = ByteUtils.bytesToInt(timeHelloB);

        final HelloMessageDetail helloMessageDetail = new HelloMessageDetail();

        helloMessageDetail.setTimeCollectedOnDevice(Long.valueOf(timeReceived));
        helloMessageDetail.setTimeFromHelloMessage(timeHello);
        helloMessageDetail.setMac(appMobile.generateMACforHelloMessage(ebid, ecc, timeHelloB));
        helloMessageDetail.setRssiCalibrated(rssiCalibrated);

        return helloMessageDetail;
    }

    private byte[] generateHMAC(final CryptoHMACSHA256 cryptoHMACSHA256S, final byte[] argument,
            final DigestSaltEnum salt)
            throws Exception {

        final byte[] prefix = new byte[] { salt.getValue() };

        // HMAC-SHA256 processing
        return cryptoHMACSHA256S.encrypt(ByteUtils.addAll(prefix, argument));
    }

    private byte[] generateMACforHelloMessage(byte[] ebid, byte[] ecc, byte[] timeHelloMessage) {
        // Merge arrays
        // HMAC-256
        // return hash
        byte[] mai = new byte[ebid.length + ecc.length + 2];
        System.arraycopy(ecc, 0, mai, 0, ecc.length);
        System.arraycopy(ebid, 0, mai, ecc.length, ebid.length);
        // take into account the 2 last bytes
        System.arraycopy(timeHelloMessage, 2, mai, ecc.length + ebid.length, 2);

        byte[] encryptedMac = new byte[32];
        try {
            encryptedMac = this.generateHMAC(
                    new CryptoHMACSHA256(getClientIdentifierBundleWithPublicKey().get().getKeyForMac()), mai,
                    DigestSaltEnum.HELLO
            );
        } catch (Exception e) {
            log.info("Problem generating SHA256");
        }

        // truncate the result from 0 to 40-bits
        return Arrays.copyOfRange(encryptedMac, 0, 5);
    }

    public int numberOfContacts() {
        return contacts.size();
    }

    private void saveHelloMessage(byte[] ecc, byte[] ebid, HelloMessageDetail helloMessageDetail) {

        Optional<Contact> contact = getContacts().stream().filter(c -> c.getEbid() == ebid)
                .filter(c -> c.getEcc() == ecc).findFirst();

        if (contact.isPresent()) {
            contact.get().addIdsItem(helloMessageDetail);
        } else {
            Contact newContact = new Contact();
            newContact.setEbid(ebid);
            newContact.setEcc(ecc);
            newContact.addIdsItem(helloMessageDetail);
            contacts.add(newContact);
        }
    }

}
