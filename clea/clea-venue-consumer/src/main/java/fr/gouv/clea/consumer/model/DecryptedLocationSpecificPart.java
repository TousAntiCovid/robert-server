package fr.gouv.clea.consumer.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DecryptedLocationSpecificPart {
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
    private Instant qrCodeScanTime;
    private boolean isBackward;
}

