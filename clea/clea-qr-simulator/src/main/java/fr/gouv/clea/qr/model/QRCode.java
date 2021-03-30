package fr.gouv.clea.qr.model;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Base64;

import fr.inria.clea.lsp.Location;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class QRCode {
    private String qrCode;
    private Instant qrCodeValidityStartTime;
    private long qrCodeRenewalInterval;

    public boolean isValidScanTime(Instant instant) {
        if (this.qrCodeRenewalInterval > 0) {
            Instant qrCodeValidityEndTime = qrCodeValidityStartTime.plus(qrCodeRenewalInterval, ChronoUnit.SECONDS);
            return (instant.isAfter(qrCodeValidityStartTime) || instant.equals(qrCodeValidityStartTime))
            && (instant.isBefore(qrCodeValidityEndTime) || instant.equals(qrCodeValidityEndTime));
        } else {
            return instant.isAfter(qrCodeValidityStartTime) || instant.equals(qrCodeValidityStartTime);
        }
    }

    public String getLocationTemporaryPublicID() {
        byte[] locationTemporaryPublicIDByte = Arrays.copyOfRange(Base64.getDecoder().decode(this.qrCode.substring(Location.COUNTRY_SPECIFIC_PREFIX.length())), 1, 17) ;
        return Base64.getEncoder().encodeToString(locationTemporaryPublicIDByte);
    }

}
