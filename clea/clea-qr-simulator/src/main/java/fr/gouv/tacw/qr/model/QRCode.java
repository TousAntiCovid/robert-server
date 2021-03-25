package fr.gouv.tacw.qr.model;

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
    private long qrCodeValidityStartTime;
    private long qrCodeRenewalInterval;

    public boolean isValidScanTime(long timestamp){
        if( this.qrCodeRenewalInterval > 0)
            return (timestamp >= qrCodeValidityStartTime) && (timestamp <= qrCodeValidityStartTime + qrCodeRenewalInterval);
        else
            return (timestamp >= qrCodeValidityStartTime);
    }

    public String getLocationTemporaryPublicID(){
        byte[] locationTemporaryPublicIDByte = Arrays.copyOfRange(Base64.getDecoder().decode(this.qrCode.substring(Location.COUNTRY_SPECIFIC_PREFIX.length())), 1, 17) ;
        return Base64.getEncoder().encodeToString(locationTemporaryPublicIDByte);
    }

}
