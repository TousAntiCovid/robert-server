package fr.gouv.tacw.dto;

import fr.gouv.tacw.data.DetectedVenue;
import fr.inria.clea.lsp.utils.TimeUtils;
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

    public static DecodedLocationSpecificPart createDecodedLocationSpecificPart(
            UUID locationTemporaryPublicId,
            byte[] locationTemporarySecretKey,
            byte[] encryptedLocationContactMessage
    ) {
        return new DecodedLocationSpecificPart(
                0,
                0,
                0,
                false,
                locationTemporaryPublicId,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                locationTemporarySecretKey,
                encryptedLocationContactMessage,
                0
        );
    }

    public DetectedVenue toDetectedVenue() {
        return new DetectedVenue(
                null,
                version,
                type,
                countryCode,
                staff,
                locationTemporaryPublicId.toString(),
                qrCodeRenewalIntervalExponentCompact,
                venueType,
                venueCategory1,
                venueCategory2,
                periodDuration,
                compressedPeriodStartTime,
                qrCodeValidityStartTime,
                locationTemporarySecretKey,
                encryptedLocationContactMessage,
                TimeUtils.instantFromTimestamp(qrCodeScanTime),
                null,
                null
        );
    }
}

