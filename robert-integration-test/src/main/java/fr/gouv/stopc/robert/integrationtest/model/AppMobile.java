package fr.gouv.stopc.robert.integrationtest.model;


import com.fasterxml.jackson.databind.ObjectMapper;


import fr.gouv.stopc.robert.integrationtest.enums.DigestSaltEnum;

import fr.gouv.stopc.robert.integrationtest.exception.RobertServerCryptoException;
import fr.gouv.stopc.robert.integrationtest.model.api.request.AuthentifiedRequest;
import fr.gouv.stopc.robert.integrationtest.model.api.request.RegisterSuccessResponse;
import fr.gouv.stopc.robert.integrationtest.model.api.response.ExposureStatusResponse;
import fr.gouv.stopc.robert.integrationtest.utils.ExchangeHelloMessageTimer;
import fr.gouv.stopc.robert.integrationtest.utils.common.ByteUtils;
import fr.gouv.stopc.robert.integrationtest.utils.common.TimeUtils;
import fr.gouv.stopc.robert.integrationtest.utils.crypto.CryptoAESGCM;
import fr.gouv.stopc.robert.integrationtest.utils.crypto.CryptoHMACSHA256;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import com.fasterxml.jackson.core.type.TypeReference;

import static fr.gouv.stopc.robert.integrationtest.utils.EcdhUtils.deriveKeysFromBackendPublicKey;
import static fr.gouv.stopc.robert.integrationtest.utils.EcdhUtils.generateKeyPair;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;


@Slf4j
public class AppMobile {

    @Getter
    private final String captchaId;
    private final String captcha;
    private final String robertPublicKey;
    private long timestart;

    private ExchangeHelloMessageTimer timer;

    @Getter(AccessLevel.PRIVATE)
    List<Contact> contacts;

    @Getter(AccessLevel.PRIVATE)
    List<EphemeralTupleJson> decodedTuples;

    @Getter(AccessLevel.PRIVATE)
    Optional<ClientIdentifierBundle> clientIdentifierBundleWithPublicKey;

    KeyPair keyPair;

    public AppMobile(String captchaId, String captcha, String robertPublicKey) throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, RobertServerCryptoException {
        this.captcha = captcha;
        this.captchaId = captchaId;
        this.robertPublicKey = robertPublicKey;
        this.keyPair = generateKeyPair();
        this.contacts = new ArrayList<>();
        generateKeyForTuples();

    }

    public List<Contact> getContactsAndRemoveThem() {

        List<Contact> returnedContacts = new ArrayList<>(contacts);
        contacts.clear();

        log.info("Contacts from Mobile App {} have been cleared", this.captchaId);
        return returnedContacts;
    }

    public String getPublicKey() {
        return org.bson.internal.Base64.encode(keyPair.getPublic().getEncoded());
    }

    public AuthentifiedRequest prepareAuthRequest(final int adjustTimeInSeconds, final DigestSaltEnum saltEnum) {

        EphemeralTupleJson tuple = getCurrentTuple();

        byte[] ebid = tuple.getKey().getEbid();
        int epochId = tuple.getEpochId();
        byte[] time = generateTime32(System.currentTimeMillis(), adjustTimeInSeconds);
        byte[] mac = generateMAC(ebid, epochId, time, saltEnum);

        AuthentifiedRequest authentifiedRequest = new AuthentifiedRequest();
        authentifiedRequest.setEbid(ebid);
        authentifiedRequest.setEpochId(epochId);
        authentifiedRequest.setTime(time);
        authentifiedRequest.setMac(mac);

        return authentifiedRequest;
    }

    public void decryptStatusResponse(ExposureStatusResponse exposureStatusResponse){
        updateTuples(exposureStatusResponse.getTuples());
    }

    public void decryptRegisterResponse(RegisterSuccessResponse registerData) {
        this.timestart = registerData.getTimeStart();
        updateTuples(registerData.getTuples());
    }

    public void startHelloMessageExchanges(List<AppMobile> otherApps, Duration delayInMin) {
        if (Objects.isNull(timer)) {
            timer = new ExchangeHelloMessageTimer(String.format("App Mobile %s is receiving hello messages from following mobile applications [%s] every %s",
                    this.captchaId, otherApps.stream().map(AppMobile::getCaptchaId).collect(Collectors.joining(",")), delayInMin.toString()));
            timer.scheduleAtFixedRate(new HelloMessageExchangeTask(otherApps), 0, delayInMin.toMillis());
        } else {
            log.warn("A timer with context {} is already running on this application. Please stop it and start it again if needed.", timer.getFunctionalContext());
        }
    }

    public void stopHelloMessageExchanges() {
        timer.cancel();
        timer.purge();
        timer = null;
    }

    private void generateKeyForTuples() throws RobertServerCryptoException {
        this.clientIdentifierBundleWithPublicKey = deriveKeysFromBackendPublicKey(Base64.getDecoder().decode(this.robertPublicKey), keyPair);
    }

    private void updateTuples(byte[] encryptedTuples) {
        CryptoAESGCM aesGcm = new CryptoAESGCM(clientIdentifierBundleWithPublicKey.get().getKeyForTuples());
        try {
            byte[] decryptedTuples = aesGcm.decrypt(encryptedTuples);
            ObjectMapper objectMapper = new ObjectMapper();
            this.decodedTuples = objectMapper.readValue(
                    decryptedTuples,
                    new TypeReference<List<EphemeralTupleJson>>() {
                    });
        } catch (IOException | RobertServerCryptoException e) {
            log.error(e.getMessage(), e);
        }
    }

    private EphemeralTupleJson getCurrentTuple() {
        int currentEpoch = TimeUtils.getCurrentEpochFrom(timestart);

        EphemeralTupleJson tuple = decodedTuples.stream()
                .filter(e -> e.getEpochId()==currentEpoch)
                .collect(Collectors.toList())
                .get(0);

        log.debug("For epoch {}, tuple of app-mobile {} is {} ", currentEpoch, captchaId, tuple.toString());

        return tuple;

    }

    private byte[] generateTime32(long unixTimeInMillis, int adjustTimeInSeconds) {
        long tsInSeconds = TimeUtils.convertUnixMillistoNtpSeconds(unixTimeInMillis);
        tsInSeconds += adjustTimeInSeconds;
        byte[] tsInSecondsB = ByteUtils.longToBytes(tsInSeconds);
        byte[] time = new byte[4];

        System.arraycopy(tsInSecondsB, 4, time, 0, 4);

        return time;
    }

    private byte[] generateMAC(byte[] ebid, int epochId, byte[] time, DigestSaltEnum saltEnum) {
        // Merge arrays
        // HMAC-256
        // return hash
        byte[] agg = new byte[8 + 4 + 4];
        System.arraycopy(ebid, 0, agg, 0, ebid.length);
        System.arraycopy(ByteUtils.intToBytes(epochId), 0, agg, ebid.length, Integer.BYTES);
        System.arraycopy(time, 0, agg, ebid.length + Integer.BYTES, time.length);

        byte[] mac = new byte[32];
        try {
            mac = this.generateHMAC(new CryptoHMACSHA256(getClientIdentifierBundleWithPublicKey().get().getKeyForMac()), agg, saltEnum);
        } catch (Exception e) {
            log.info("Problem generating SHA256");
        }
        return mac;
    }

    private byte[] generateHMAC(final CryptoHMACSHA256 cryptoHMACSHA256S, final byte[] argument, final DigestSaltEnum salt)
            throws Exception {

        final byte[] prefix = new byte[]{salt.getValue()};

        // HMAC-SHA256 processing
        return cryptoHMACSHA256S.encrypt(ByteUtils.addAll(prefix, argument));
    }

    private class HelloMessageExchangeTask extends TimerTask {

        private final List<AppMobile> appMobiles;

        public HelloMessageExchangeTask(List<AppMobile> appMobiles) {
            this.appMobiles = appMobiles;
        }

        public void run() {
            appMobiles.forEach(AppMobile.this::exchangeEbIdWith);
        }
    }

    private void exchangeEbIdWith(AppMobile otherAppMobile) {

        log.debug("exchange hello message between {} and {} mobile applications", this.captchaId, otherAppMobile.captchaId);

        HelloMessageDetail helloMessageDetail = generateHelloMessage(otherAppMobile,
                TimeUtils.convertUnixMillistoNtpSeconds(System.currentTimeMillis()),
                ThreadLocalRandom.current().nextInt(-10,3));

        saveHelloMessage(otherAppMobile.getCurrentTuple().getKey().getEcc(),
                otherAppMobile.getCurrentTuple().getKey().getEbid(),
                helloMessageDetail);

    }

    private void saveHelloMessage(byte[] ecc, byte[] ebid, HelloMessageDetail helloMessageDetail) {

        Optional<Contact> contact = getContacts().stream().filter(c -> c.getEbid()== ebid)
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


    private byte[] generateMACforHelloMessage(byte[] ebid, byte[] ecc, byte[] timeHelloMessage) {
        // Merge arrays
        // HMAC-256
        // return hash
        byte[] mai = new byte[ebid.length + ecc.length + 2];
        System.arraycopy(ecc, 0, mai, 0, ecc.length);
        System.arraycopy(ebid, 0, mai, ecc.length, ebid.length);
        //take into account the 2 last bytes
        System.arraycopy(timeHelloMessage, 2, mai, ecc.length + ebid.length, 2);


        byte[] encryptedMac = new byte[32];
        try {
            encryptedMac = this.generateHMAC(new CryptoHMACSHA256(getClientIdentifierBundleWithPublicKey().get().getKeyForMac()), mai, DigestSaltEnum.HELLO);
        } catch (Exception e) {
            log.info("Problem generating SHA256");
        }

        // truncate the result from 0 to 40-bits
        return Arrays.copyOfRange(encryptedMac, 0, 5);
    }

}
