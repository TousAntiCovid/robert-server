package fr.gouv.clea.consumer.model;

import fr.inria.clea.lsp.EncryptedLocationSpecificPart;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@AllArgsConstructor
@Getter
@ToString
public class DecodedVisit {
    private final Long qrCodeScanTime; // t_qrScan
    private final EncryptedLocationSpecificPart encryptedLocationSpecificPart;
    private final boolean isBackward;
}
