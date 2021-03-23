package fr.gouv.tacw.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.RandomUtils;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class DetectedVenue {

    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "org.hibernate.id.UUIDGenerator")
    private String id;

    private int version;
    private int type;
    private int countryCode;
    private boolean staff;
    private String locationTemporaryPublicId;
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

    @CreationTimestamp
    private Instant createdAt; // for db ops/maintenance

    @UpdateTimestamp
    private Instant updatedAt; // for db ops/maintenance

    public static DetectedVenue createDetectedVenue(Instant qrCodeScanTime) {
        return new DetectedVenue(
                null,
                0,
                0,
                0,
                false,
                UUID.randomUUID().toString(),
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                RandomUtils.nextBytes(20),
                RandomUtils.nextBytes(20),
                qrCodeScanTime,
                null,
                null
        );
    }
}
