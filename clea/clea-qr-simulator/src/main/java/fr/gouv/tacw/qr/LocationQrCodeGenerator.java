package fr.gouv.tacw.qr;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import fr.gouv.tacw.qr.models.QRCode;
import fr.inria.clea.lsp.CleaEncryptionException;
import fr.inria.clea.lsp.Location;
import fr.inria.clea.lsp.LocationContact;
import fr.inria.clea.lsp.LocationSpecificPart;
import fr.inria.clea.lsp.Location.LocationBuilder;
import fr.inria.clea.lsp.utils.TimeUtils;
import lombok.Builder;

public class LocationQrCodeGenerator {
    private final long                  qrCodeRenewalInterval;
    private final long                  periodDuration;
    private final Location              location;
    private long                        periodStartTime;
    private List<QRCode>                generatedQRs;

    @Builder
    private LocationQrCodeGenerator(  Instant periodStartTime, 
                        int     periodDuration, 
                        int     qrCodeRenewalIntervalExponentCompact, 
                        boolean staff,
                        int     countryCode, 
                        int     venueType, 
                        int     venueCategory1, 
                        int     venueCategory2,
                        String  manualContactTracingAuthorityPublicKey, 
                        String  serverAuthorityPublicKey,
                        String  permanentLocationSecretKey, 
                        String  locationPhone, 
                        String  locationPin
                        ) throws CleaEncryptionException{
        // TODO: check data validity (eg. < periodDuration <= 255)
        if (qrCodeRenewalIntervalExponentCompact == 0x1F)
            this.qrCodeRenewalInterval = 0;
        else
            this.qrCodeRenewalInterval = 1 << qrCodeRenewalIntervalExponentCompact;
        this.periodDuration = periodDuration * TimeUtils.NB_SECONDS_PER_HOUR;
        this.periodStartTime = TimeUtils.hourRoundedTimestamp(TimeUtils.ntpTimestampFromInstant(periodStartTime));

        LocationSpecificPart locationSpecificPart = this.createLocationSpecificPart(staff, periodDuration, qrCodeRenewalIntervalExponentCompact, countryCode, venueType, venueCategory1, venueCategory2);
        LocationContact contact = this.createLocationContact(this.periodStartTime, locationPhone, locationPin);
        this.location = this.createLocation(locationSpecificPart, contact, manualContactTracingAuthorityPublicKey, serverAuthorityPublicKey, permanentLocationSecretKey);
        this.generatedQRs = new ArrayList<>();
        this.generateQRCode(this.periodStartTime);

    }

    private Location createLocation(LocationSpecificPart lsp, LocationContact contact, String manualContactTracingAuthorityPublicKey, String serverAuthorityPublicKey, String permanentLocationSecretKey){
        LocationBuilder locationBuilder = Location.builder()
                                                .locationSpecificPart(lsp)
                                                .manualContactTracingAuthorityPublicKey(manualContactTracingAuthorityPublicKey)
                                                .serverAuthorityPublicKey(serverAuthorityPublicKey)
                                                .permanentLocationSecretKey(permanentLocationSecretKey);
        if (Objects.nonNull(contact)){
            locationBuilder.contact(contact);
        }
        return locationBuilder.build();
    }

    private LocationSpecificPart createLocationSpecificPart(boolean staff, int periodDuration, int qrCodeRenewalIntervalExponentCompact, int countryCode, int venueType, int venueCategory1, int venueCategory2){
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

    private LocationContact createLocationContact(long periodStartTime, String locationPhone, String locationPin){
        LocationContact contact = null;
        if (locationPhone != null && locationPin != null) {
            contact = LocationContact.builder()
                                                     .locationPhone(locationPhone)
                                                     .locationPin(locationPin)
                                                     .periodStartTime((int)periodStartTime)
                                                     .build();
        }
        return contact;
    }

    /**
     * find a valid QRCode for the provided timestamp in the already generated one.
     * @param timestamp timestamp at which the QRCode should be valid.
     * @return a valid QRCode or null if none were found.
     */
    public QRCode findExistingQrCode(long timestamp){
        for(QRCode qr : generatedQRs){
            if(qr.isValidScanTime(timestamp)){
                return qr;
            }
        }
        return null;
    }

    /**
     * find a valid QRCode for the provided timestamp in the already generated one.
     * @param instant instant at which the QRCode should be valid
     * @return a valid QRCode or null if none were found.
     */
    public QRCode findExistingQrCode(Instant instant){
        return this.findExistingQrCode(TimeUtils.ntpTimestampFromInstant(instant));
    }

    private QRCode generateQRCode(long timestamp) throws CleaEncryptionException{
        QRCode qr;
        if(this.qrCodeRenewalInterval == 0){
            String deepLink = this.location.newDeepLink((int)this.periodStartTime);
            qr = new QRCode(deepLink, this.periodStartTime, this.qrCodeRenewalInterval);            
        }else{
            timestamp = timestamp - ((timestamp - this.periodStartTime) % this.qrCodeRenewalInterval);
            String deepLink = this.location.newDeepLink((int)this.periodStartTime, (int)timestamp);
            qr = new QRCode(deepLink, timestamp, this.qrCodeRenewalInterval);
        }
        this.generatedQRs.add(qr);
        return qr;
    }

    /**
     * get a valid QR code for the provided NTP timestamp
     * @param timestamp timestamp at which the returned QRCode should be valid
     * @return  a valid QRCode for the provided timestamp
     * @throws InvalidTimestampException
     * @throws CleaEncryptionException
     */
    public QRCode getQrCodeAt(long timestamp) throws InvalidTimestampException, CleaEncryptionException{
        if(timestamp < this.periodStartTime || timestamp > this.periodStartTime + this.periodDuration){
           throw new InvalidTimestampException("timestamp : "+ timestamp+ " not in the current period of the generator. consider starting a new period first.\n");
        }
        //check if qr already exists for timestamp
        QRCode qr = this.findExistingQrCode(timestamp);
        if(Objects.nonNull(qr)){
            return qr;
        }
        //generate a new one
        qr = this.generateQRCode(timestamp);
        return qr;
    }

    /**
     * start a new period and return its first valid QRCode
     * @param periodStart NTP timestamp at which the period started.
     * @return a QRCode with the provided timestamp (rounded to the nearest hour) as starting validity
     * @throws InvalidTimestampException
     * @throws CleaEncryptionException
     */
    public QRCode startNewPeriod(long periodStart) throws InvalidTimestampException, CleaEncryptionException{
        this.periodStartTime = TimeUtils.hourRoundedTimestamp(periodStart);
        this.generatedQRs.clear();  
        return this.generateQRCode(periodStart);
    }

    /**
     * start a new period and return its first valid QRCode
     * @param instant : instant at which the new period should start, will be rounded to the nearest hour.
     * @return a QRCode with the provided instant (rounded to the nearest hour) as starting validity
     * @throws InvalidTimestampException
     * @throws CleaEncryptionException
     */
    public QRCode startNewPeriod(Instant instant) throws InvalidTimestampException, CleaEncryptionException{
        return this.startNewPeriod(TimeUtils.ntpTimestampFromInstant(instant));
    }

    /**
     * get a valid QR code for the provided instant
     * @param instant : instant at which the returned QRCode should be valid
     * @return a valid QRCode for the provided instant
     * @throws InvalidTimestampException if instant is not included in the current period
     * @throws CleaEncryptionException
     */
    public QRCode getQrCodeAt(Instant instant) throws InvalidTimestampException, CleaEncryptionException{
        return this.getQrCodeAt(TimeUtils.ntpTimestampFromInstant(instant));
    }   
    
    public static class InvalidTimestampException extends IllegalArgumentException{
        public InvalidTimestampException(String msg){
            super(msg);
        }        
    }

    public long getPeriodStart(){
        return this.periodStartTime;
    }
}
