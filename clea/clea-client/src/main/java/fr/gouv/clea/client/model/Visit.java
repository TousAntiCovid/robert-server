package fr.gouv.clea.client.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Visit {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String qrCode;
    
    @JsonProperty("qrCodeScanTime")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long scanTime;
}
