package fr.gouv.clea.qr;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import fr.gouv.clea.qr.model.QRCode;
import fr.inria.clea.lsp.Location;
import fr.inria.clea.lsp.Location.LocationBuilder;
import fr.inria.clea.lsp.LocationContact;
import fr.inria.clea.lsp.LocationSpecificPart;
import fr.inria.clea.lsp.exception.CleaCryptoException;
import fr.inria.clea.lsp.exception.CleaEncryptionException;
import fr.inria.clea.lsp.utils.TimeUtils;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
@Slf4j
public class LocationQrCodeGenerator {
    private final long qrCodeRenewalInterval;
    private final Duration periodDuration;
    private final Location location;
    private Instant initialPeriodStartTime;
    private Map<Period, List<QRCode>> generatedQRs;

    @Builder
    private LocationQrCodeGenerator(Instant periodStartTime, int periodDuration,
            int qrCodeRenewalIntervalExponentCompact, boolean staff, int countryCode, int venueType, int venueCategory1,
            int venueCategory2, String manualContactTracingAuthorityPublicKey, String serverAuthorityPublicKey,
            String permanentLocationSecretKey, String locationPhone, String locationPin)
            throws CleaCryptoException {
        // TODO: check data validity (eg. < periodDuration <= 255)
        if (qrCodeRenewalIntervalExponentCompact == 0x1F)
            this.qrCodeRenewalInterval = 0;
        else
            this.qrCodeRenewalInterval = 1 << qrCodeRenewalIntervalExponentCompact;
        this.periodDuration = Duration.ofSeconds(periodDuration * TimeUtils.NB_SECONDS_PER_HOUR);
        this.initialPeriodStartTime = periodStartTime.truncatedTo(ChronoUnit.HOURS);

        LocationSpecificPart locationSpecificPart = this.createLocationSpecificPart(staff, periodDuration,
                qrCodeRenewalIntervalExponentCompact, countryCode, venueType, venueCategory1, venueCategory2);
        LocationContact contact = this.createLocationContact(this.initialPeriodStartTime, locationPhone, locationPin);
        this.location = this.createLocation(locationSpecificPart, contact, manualContactTracingAuthorityPublicKey,
                serverAuthorityPublicKey, permanentLocationSecretKey);
        this.generatedQRs = new HashMap<>();
        this.createPeriod(this.initialPeriodStartTime);
    }


    private Location createLocation(LocationSpecificPart lsp, LocationContact contact,
            String manualContactTracingAuthorityPublicKey, String serverAuthorityPublicKey,
            String permanentLocationSecretKey) {
        LocationBuilder locationBuilder = Location.builder()
                .locationSpecificPart(lsp)
                .manualContactTracingAuthorityPublicKey(manualContactTracingAuthorityPublicKey)
                .serverAuthorityPublicKey(serverAuthorityPublicKey)
                .permanentLocationSecretKey(permanentLocationSecretKey);
        if (Objects.nonNull(contact)) {
            locationBuilder.contact(contact);
        }
        return locationBuilder.build();
    }

    private LocationSpecificPart createLocationSpecificPart(boolean staff, int periodDuration,
            int qrCodeRenewalIntervalExponentCompact, int countryCode, int venueType, int venueCategory1,
            int venueCategory2) {
        LocationSpecificPart lsp = LocationSpecificPart.builder()
                .staff(staff)
                .periodDuration(periodDuration)
                .countryCode(countryCode)
                .qrCodeRenewalIntervalExponentCompact(qrCodeRenewalIntervalExponentCompact)
                .venueType(venueType)
                .venueCategory1(venueCategory1)
                .venueCategory2(venueCategory2)
                .build();
        return lsp;
    }

    private LocationContact createLocationContact(Instant periodStartTime, String locationPhone, String locationPin) {
        LocationContact contact = null;
        if (locationPhone != null && locationPin != null) {
            contact = LocationContact.builder()
                    .locationPhone(locationPhone)
                    .locationPin(locationPin)
                    .periodStartTime(periodStartTime)
                    .build();
        }
        return contact;
    }

    public Instant getInitialPeriodStart() {
        return this.initialPeriodStartTime;
    }

    /**
     * get a valid QR code for the provided instant
     * 
     * @param instant : instant at which the returned QRCode should be valid
     * @return a valid QRCode for the provided instant
     * @throws CleaEncryptionException
     */
    public QRCode getQrCodeAt(Instant instant) throws CleaCryptoException {
        Period period = this.findOrCreatePeriod(instant);
        QRCode qr = this.findOrCreateQrCode(period, instant);
        log.debug("QR Code At {} : {}", instant.toString(), qr.getQrCode());
        return qr;
    }

    private Period findOrCreatePeriod(Instant instant){
        return this.findExistingPeriod(instant).orElseGet(() -> this.createPeriod(instant));
    }

    private Optional<Period> findExistingPeriod(Instant instant){
        log.debug("search existing period for {}", instant.toString());
        for(Period period : this.generatedQRs.keySet()){
            log.debug("is {} in {} ? {}", instant.toString(), period.toString(), period.isInPeriod(instant));
            if(period.isInPeriod(instant))
                return Optional.of(period);
        }
        return Optional.empty();
    }

    private Period createPeriod(Instant instant){
        // Assumption : All Periods are contiguous, making auto period-creation easier.
        // This is not the real-world case. Could emulate how real-world will be done using a period duration such as periodDuration%24 == 0.
        Instant periodStart = instant.minus(Duration.between(instant, this.initialPeriodStartTime).abs().getSeconds() % this.periodDuration.getSeconds(), ChronoUnit.SECONDS);
        Period period = new Period(periodStart, periodDuration);
        this.generatedQRs.put(period, new ArrayList<>());
        log.debug("new Period created");
        return period;
    }


    private QRCode findOrCreateQrCode(Period period, Instant instant) throws CleaCryptoException{
        Optional<QRCode> existingQr = this.findExistingQrCode(period, instant);
        return existingQr.isPresent() ? existingQr.get() : this.createQrCode(period, instant);
    }

    private Optional<QRCode> findExistingQrCode(Period period, Instant instant){
        for(QRCode qr : this.generatedQRs.get(period)){
            if(qr.isValidScanTime(instant)){
                return Optional.of(qr);
            }
        }
        return Optional.empty();
    }

    private QRCode createQrCode(Period period, Instant instant) throws CleaCryptoException{
        QRCode qr;
        if (this.qrCodeRenewalInterval == 0) {
            String deepLink = this.location.newDeepLink(period.getStartTime());
            qr = new QRCode(deepLink, period.getStartTime(), this.qrCodeRenewalInterval);
        } else {
            Instant qrCodeValidityStartTime = instant.minus(Duration.between(instant, period.getStartTime()).abs().getSeconds() % this.qrCodeRenewalInterval, ChronoUnit.SECONDS);
            String deepLink = this.location.newDeepLink(period.startTime, qrCodeValidityStartTime);
            qr = new QRCode(deepLink, qrCodeValidityStartTime, this.qrCodeRenewalInterval);
        }
        this.generatedQRs.get(period).add(qr);
        log.debug("new QR created");
        return qr;
    }
    public static class InvalidInstantException extends IllegalArgumentException {
        private static final long serialVersionUID = 1L;
    
        public InvalidInstantException(String msg) {
            super(msg);
        }
    }
    @Data
    @AllArgsConstructor
    public class Period {
        private Instant startTime;
        private Duration duration;

        public boolean isInPeriod(Instant instant){
            return !(instant.isBefore(startTime) || instant.isAfter(startTime.plus(duration)));
        }

        public String toString(){
            return startTime + " - "+startTime.plus(duration);
        }
    }
}
