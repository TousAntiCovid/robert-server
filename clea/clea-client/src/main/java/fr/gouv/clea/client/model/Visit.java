package fr.gouv.clea.client.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Visit {

    private String qrCode;
    
    @JsonProperty("qrCodeScanTime")
    private long scanTime;
}
