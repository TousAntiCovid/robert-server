package fr.gouv.clea.ws.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Visit {
    private String qrCode;
    private Long qrCodeScanTime; // t_qrScan
}
