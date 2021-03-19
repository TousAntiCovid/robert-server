package fr.gouv.tacw.data;

import fr.inria.clea.lsp.LocationSpecificPart;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DecodedLocationSpecificPart {

    private int version;
    private int type;
    private int countryCode;
    private boolean staff;
    private UUID locationTemporaryPublicId;
    private int qrCodeRenewalIntervalExponentCompact;
    private int venueType;
    private int venueCategory1;
    private int venueCategory2;
    private int periodDuration;
    private int compressedPeriodStartTime;
    private int qrCodeValidityStartTime;
    private byte[] locationTemporarySecretKey;
    private byte[] encryptedLocationContactMessage;
    private long qrCodeScanTime;

    private String qrCode; // FIXME need it for logs

    public static DecodedLocationSpecificPart fromLocationSpecificPart(LocationSpecificPart locationSpecificPart, long qrCodeScanTime, String qrCode) {
        return new DecodedLocationSpecificPart(
                locationSpecificPart.getVersion(),
                locationSpecificPart.getType(),
                locationSpecificPart.getCountryCode(),
                locationSpecificPart.isStaff(),
                locationSpecificPart.getLocationTemporaryPublicId(),
                locationSpecificPart.getQrCodeRenewalIntervalExponentCompact(),
                locationSpecificPart.getVenueType(),
                locationSpecificPart.getVenueCategory1(),
                locationSpecificPart.getVenueCategory2(),
                locationSpecificPart.getPeriodDuration(),
                locationSpecificPart.getCompressedPeriodStartTime(),
                locationSpecificPart.getQrCodeValidityStartTime(),
                locationSpecificPart.getLocationTemporarySecretKey(),
                locationSpecificPart.getEncryptedLocationContactMessage(),
                qrCodeScanTime,
                qrCode
        );
    }
}
