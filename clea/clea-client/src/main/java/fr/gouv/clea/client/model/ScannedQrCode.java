package fr.gouv.clea.client.model;

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

    private String qrCode;

    @JsonProperty("qrCodeScanTime")
    private long scanTime;

    @JsonIgnore
    private Optional<String> locationTemporaryId; //LTId

    public ScannedQrCode(String qrCode, long scanTime){
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
    
}