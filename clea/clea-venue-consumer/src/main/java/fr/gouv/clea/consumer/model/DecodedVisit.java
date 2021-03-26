package fr.gouv.clea.consumer.model;

import fr.inria.clea.lsp.EncryptedLocationSpecificPart;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.time.Instant;

@AllArgsConstructor
@Builder
@Getter
@ToString
public class DecodedVisit {
    private final Instant qrCodeScanTime; // t_qrScan
    private final EncryptedLocationSpecificPart encryptedLocationSpecificPart;
    private final boolean isBackward;

    public String getStringLocationTemporaryPublicId() {
        return this.encryptedLocationSpecificPart.getLocationTemporaryPublicId().toString();
    }
}
