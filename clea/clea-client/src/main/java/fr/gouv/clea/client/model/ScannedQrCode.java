package fr.gouv.clea.client.model;

import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ScannedQrCode {
    //  Number of seconds to fill the gap between UNIX timestamp (1/1/1970) and NTP timestamp (1/1/1900)
    public final static long SECONDS_FROM_01_01_1900_TO_01_01_1970 = 2208988800L;
    
    private String qrCode;

    @JsonProperty("qrCodeScanTime")
    private Instant scanTime;

    @JsonIgnore
    private Optional<String> locationTemporaryId; //LTId

    public ScannedQrCode(String qrCode, Instant scanTime){
        this.qrCode = qrCode;
        this.scanTime = scanTime;
        this.locationTemporaryId = Optional.empty();
    }

    public String getLocationTemporaryId() {
        return locationTemporaryId.orElse(this.decodeLocationTemporaryId());
    }
    
    private String decodeLocationTemporaryId() {
        byte[] tlIdByte = Arrays.copyOfRange(Base64.getDecoder().decode(qrCode), 1, 17) ;
        locationTemporaryId = Optional.of(Base64.getEncoder().encodeToString(tlIdByte));
        return locationTemporaryId.get();
    }

    public boolean startWithPrefix(String prefix){
        return this.getLocationTemporaryId().startsWith(prefix);
    }
    
    public long getScanTimeAsNtpTimestamp() {
        long timestamp = this.scanTime.getEpochSecond() + SECONDS_FROM_01_01_1900_TO_01_01_1970;
        // TODO check convertion to unsigned int
        return timestamp;
    }
}