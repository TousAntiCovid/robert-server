package fr.gouv.clea.ws.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Visit {
    @NotNull
    private String qrCode;

    @NotNull
    @JsonProperty("qrCodeScanTime")
    private Long qrCodeScanTimeAsNtpTimestamp; // t_qrScan
}
