package fr.gouv.clea.consumer.service.impl;

import fr.gouv.clea.consumer.data.ExposedVisit;
import fr.gouv.clea.consumer.model.DecodedVisit;
import fr.gouv.clea.consumer.model.DecryptedLocationSpecificPart;
import fr.gouv.clea.consumer.service.IVerificationService;
import fr.inria.clea.lsp.CleaEciesEncoder;
import fr.inria.clea.lsp.CleaEncryptionException;
import fr.inria.clea.lsp.LocationSpecificPartDecoder;
import fr.inria.clea.lsp.utils.TimeUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@Slf4j
public class VerificationService implements IVerificationService {

    private final LocationSpecificPartDecoder decoder;

    @Autowired
    public VerificationService(LocationSpecificPartDecoder decoder) {
        this.decoder = decoder;
    }

    @Override
    public Optional<ExposedVisit> decryptAndValidate(DecodedVisit decodedVisit) {
        return Optional.empty();
    }

    private Optional<DecryptedLocationSpecificPart> verify(DecryptedLocationSpecificPart decryptedLocationSpecificPart) {
        if (!this.isFresh(decryptedLocationSpecificPart)) {
            log.warn("");  // FIXME
            return Optional.empty();
        } else if (!this.isComputedTemporaryLocationPublicIdValid(decryptedLocationSpecificPart)) {
            log.warn("");  // FIXME
            return Optional.empty();
        }
        log.info("");  // FIXME
        return Optional.of(this.setExposureTime(decryptedLocationSpecificPart));
    }

    private boolean isComputedTemporaryLocationPublicIdValid(DecryptedLocationSpecificPart decryptedLocationSpecificPart) {
        try {
            UUID computed = new CleaEciesEncoder().computeLocationTemporaryPublicId(decryptedLocationSpecificPart.getLocationTemporarySecretKey());
            return computed.equals(decryptedLocationSpecificPart.getLocationTemporaryPublicId());
        } catch (CleaEncryptionException e) {
            return false;
        }
    }

    private boolean isFresh(DecryptedLocationSpecificPart decryptedLocationSpecificPart) {
        double qrCodeRenewalInterval = (decryptedLocationSpecificPart.getQrCodeRenewalIntervalExponentCompact() == 0x1F)
                ? 0 : Math.pow(2, decryptedLocationSpecificPart.getQrCodeRenewalIntervalExponentCompact());
        if (qrCodeRenewalInterval <= 0)
            return true;
        return Math.abs(TimeUtils.ntpTimestampFromInstant(decryptedLocationSpecificPart.getQrCodeScanTime()) - decryptedLocationSpecificPart.getQrCodeValidityStartTime()) < (qrCodeRenewalInterval + 300 + 300);
    }

    private DecryptedLocationSpecificPart setExposureTime(DecryptedLocationSpecificPart decryptedLocationSpecificPart) {
        // FIXME implement when specs are precise
        return decryptedLocationSpecificPart;
    }
}
