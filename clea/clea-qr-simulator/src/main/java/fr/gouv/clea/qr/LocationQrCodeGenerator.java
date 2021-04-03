package fr.gouv.clea.qr;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import fr.gouv.clea.qr.model.QRCode;
import fr.inria.clea.lsp.Location;
import fr.inria.clea.lsp.Location.LocationBuilder;
import fr.inria.clea.lsp.LocationContact;
import fr.inria.clea.lsp.LocationSpecificPart;
import fr.inria.clea.lsp.exception.CleaCryptoException;
import fr.inria.clea.lsp.exception.CleaEncryptionException;
import fr.inria.clea.lsp.utils.TimeUtils;
import lombok.Builder;

public class LocationQrCodeGenerator {
    private final long qrCodeRenewalInterval;
    private final long periodDuration;
    private final Location location;
    private Instant periodStartTime;
    private List<QRCode> generatedQRs;

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
        this.periodDuration = periodDuration * TimeUtils.NB_SECONDS_PER_HOUR;
        this.periodStartTime = periodStartTime.truncatedTo(ChronoUnit.HOURS);

        LocationSpecificPart locationSpecificPart = this.createLocationSpecificPart(staff, periodDuration,
                qrCodeRenewalIntervalExponentCompact, countryCode, venueType, venueCategory1, venueCategory2);
        LocationContact contact = this.createLocationContact(this.periodStartTime, locationPhone, locationPin);
        this.location = this.createLocation(locationSpecificPart, contact, manualContactTracingAuthorityPublicKey,
                serverAuthorityPublicKey, permanentLocationSecretKey);
        this.generatedQRs = new ArrayList<>();
        this.generateQRCode(this.periodStartTime);
    }

    /**
     * start a new period and return its first valid QRCode
     * 
     * @param periodStart : instant at which the new period should start, will be
     *                rounded to the nearest hour.
     * @return a QRCode with the provided instant (rounded to the nearest hour) as
     *         starting validity
     * @throws InvalidInstantException
     * @throws CleaEncryptionException
     */
    public QRCode startNewPeriod(Instant periodStart) throws CleaCryptoException {
        this.generatedQRs.clear();
        this.periodStartTime = periodStart;
        return this.generateQRCode(periodStart);
    }

    public Instant getPeriodStart() {
        return this.periodStartTime;
    }

    /**
     * get a valid QR code for the provided instant
     * 
     * @param instant : instant at which the returned QRCode should be valid
     * @return a valid QRCode for the provided instant
     * @throws InvalidInstantException if instant is not included in the current
     *                                   period
     * @throws CleaEncryptionException
     */
    public QRCode getQrCodeAt(Instant instant) throws CleaCryptoException {
        if (instant.isBefore(this.periodStartTime) || instant.isAfter(this.periodStartTime.plus(this.periodDuration, ChronoUnit.SECONDS))) {
            throw new InvalidInstantException("Instant : " + instant
                    + " not in the current period of the generator: "
                    + this.periodStartTime + " -> " + this.periodStartTime.plus(this.periodDuration, ChronoUnit.SECONDS)
                    + ". consider starting a new period first.\n");
        }
        // check if qr already exists for timestamp
        QRCode qr = this.findExistingQrCode(instant);
        if (Objects.nonNull(qr)) {
            return qr;
        }
        // generate a new one
        qr = this.generateQRCode(instant);
        return qr;
    }

    /**
     * find a valid QRCode for the provided timestamp in the already generated one.
     * 
     * @param instant instant at which the QRCode should be valid
     * @return a valid QRCode or null if none were found.
     */
    public QRCode findExistingQrCode(Instant instant) {
        for (QRCode qr : generatedQRs) {
            if (qr.isValidScanTime(instant)) {
                return qr;
            }
        }
        return null;
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

    private QRCode generateQRCode(Instant instant) throws CleaCryptoException {
        QRCode qr;
        if (this.qrCodeRenewalInterval == 0) {
            String deepLink = this.location.newDeepLink(this.periodStartTime);
            qr = new QRCode(deepLink, this.periodStartTime, this.qrCodeRenewalInterval);
        } else {
            Instant qrCodeValidityStartTime = instant.minus(Duration.between(instant, this.periodStartTime).getSeconds() % this.qrCodeRenewalInterval, ChronoUnit.SECONDS);
            String deepLink = this.location.newDeepLink(this.periodStartTime, qrCodeValidityStartTime);
            qr = new QRCode(deepLink, instant, this.qrCodeRenewalInterval);
        }
        this.generatedQRs.add(qr);
        return qr;
    }

    public static class InvalidInstantException extends IllegalArgumentException {
        private static final long serialVersionUID = 1L;
    
        public InvalidInstantException(String msg) {
            super(msg);
        }
    }
}
