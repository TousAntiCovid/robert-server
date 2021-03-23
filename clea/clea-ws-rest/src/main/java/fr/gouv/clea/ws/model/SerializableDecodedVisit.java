package fr.gouv.clea.ws.model;

import fr.inria.clea.lsp.EncryptedLocationSpecificPart;
import lombok.Getter;
import lombok.ToString;

import java.io.Serializable;

@Getter
@ToString
public class SerializableDecodedVisit extends DecodedVisit implements Serializable {

    private final Long pivotDate;

    public SerializableDecodedVisit(Long qrCodeScanTime, EncryptedLocationSpecificPart encryptedLocationSpecificPart, Long pivotDate) {
        super(qrCodeScanTime, encryptedLocationSpecificPart);
        this.pivotDate = pivotDate;
    }
}
