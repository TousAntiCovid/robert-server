package fr.gouv.clea.ws.vo;

import javax.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Visit {
    @NotNull
    private String qrCode;
    @NotNull
    private Long qrCodeScanTime; // t_qrScan
}
