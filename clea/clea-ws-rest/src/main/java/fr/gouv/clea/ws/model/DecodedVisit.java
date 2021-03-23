package fr.gouv.clea.ws.model;

import java.util.UUID;

import fr.inria.clea.lsp.EncryptedLocationSpecificPart;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@AllArgsConstructor
@Getter
@ToString
public class DecodedVisit {
    private Long qrCodeScanTime; // t_qrScan
    private EncryptedLocationSpecificPart encryptedLocationSpecificPart;
    
    public UUID getLocationTemporaryPublicId() {
        return this.encryptedLocationSpecificPart.getLocationTemporaryPublicId();
    }
}
