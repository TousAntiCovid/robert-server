package fr.gouv.clea.ws.model;

import fr.inria.clea.lsp.EncryptedLocationSpecificPart;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

import java.util.UUID;

@AllArgsConstructor
@Getter
@ToString
public class DecodedVisit {
    private final Long qrCodeScanTime; // t_qrScan
    private final EncryptedLocationSpecificPart encryptedLocationSpecificPart;
    private final boolean isBackward;

    public UUID getLocationTemporaryPublicId() {
        return this.encryptedLocationSpecificPart.getLocationTemporaryPublicId();
    }
}
