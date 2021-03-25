package fr.gouv.clea.client.model;

import java.util.Arrays;
import java.util.Base64;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ScannedQrCode {

    private String qrCode;

    @JsonProperty("qrCodeScanTime")
    private long scanTime;

    @JsonIgnore
    private String locationTemporaryId; //LTId

    public ScannedQrCode(String qrCode, long scanTime){
        this.qrCode = qrCode;
        this.scanTime = scanTime;
        this.extractLocationTemporaryId();
    }

    private void extractLocationTemporaryId(){
        byte[] tlIdByte = Arrays.copyOfRange(Base64.getDecoder().decode(qrCode), 1, 17) ;
        this.locationTemporaryId = Base64.getEncoder().encodeToString(tlIdByte);
    }

    public boolean startWithPrefix(String prefix){
        return locationTemporaryId.startsWith(prefix);
    }

}