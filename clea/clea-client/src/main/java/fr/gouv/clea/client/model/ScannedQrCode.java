package fr.gouv.clea.client.model;

import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import fr.inria.clea.lsp.EncryptedLocationSpecificPart;
import fr.inria.clea.lsp.Location;
import fr.inria.clea.lsp.LocationSpecificPartDecoder;
import fr.inria.clea.lsp.exception.CleaEncodingException;
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
    private Optional<UUID> locationTemporaryId; //LTId

    public ScannedQrCode(String qrCode, Instant scanTime){
        this.qrCode = qrCode;
        this.scanTime = scanTime;
        this.locationTemporaryId = Optional.empty();
    }

    public UUID getLocationTemporaryId() {
        return locationTemporaryId.orElseGet(() -> this.decodeLocationTemporaryId());
    }
    
    private UUID decodeLocationTemporaryId() {
        EncryptedLocationSpecificPart encryptedLsp;
        try {
            encryptedLsp = new LocationSpecificPartDecoder().decodeHeader(Base64.getUrlDecoder().decode(qrCode));
        } catch (CleaEncodingException e) {
            throw new RuntimeException(e);
        }
        locationTemporaryId = Optional.of(encryptedLsp.getLocationTemporaryPublicId());
        return locationTemporaryId.get();
    }

    public boolean startsWithPrefix(String prefix){
        return this.getLocationTemporaryId().toString().startsWith(prefix);
    }
    
    public long getScanTimeAsNtpTimestamp() {
        long timestamp = this.scanTime.getEpochSecond() + SECONDS_FROM_01_01_1900_TO_01_01_1970;
        // TODO check convertion to unsigned int
        return timestamp;
    }
}