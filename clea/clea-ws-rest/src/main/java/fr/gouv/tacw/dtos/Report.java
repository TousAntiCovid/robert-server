package fr.gouv.tacw.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Report {
    private String qrCode;
    private Long qrCodeScanTime; // t_qrScan
}
