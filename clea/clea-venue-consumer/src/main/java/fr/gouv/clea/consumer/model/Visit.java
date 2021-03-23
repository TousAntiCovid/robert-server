package fr.gouv.clea.consumer.model;

import java.time.Instant;

import fr.inria.clea.lsp.LocationSpecificPart;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Getter
@ToString
public class Visit extends LocationSpecificPart {
    
    protected Instant qrCodeScanTime;
    protected boolean isBackward;
    
}

