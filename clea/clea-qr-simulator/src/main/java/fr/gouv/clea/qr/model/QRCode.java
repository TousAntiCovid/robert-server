package fr.gouv.clea.qr.model;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Base64;
import java.util.UUID;

import fr.inria.clea.lsp.EncryptedLocationSpecificPart;
import fr.inria.clea.lsp.Location;
import fr.inria.clea.lsp.LocationSpecificPartDecoder;
import fr.inria.clea.lsp.exception.CleaEncodingException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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

    public UUID getLocationTemporaryPublicID() {
        EncryptedLocationSpecificPart encryptedLsp;
        try {
            encryptedLsp = new LocationSpecificPartDecoder().decodeHeader(Base64.getUrlDecoder().decode(qrCode.substring(Location.COUNTRY_SPECIFIC_PREFIX.length())));
        } catch (CleaEncodingException e) {
            throw new RuntimeException(e);
        }
        return encryptedLsp.getLocationTemporaryPublicId();
    }
}
