package fr.gouv.clea.consumer.service.impl;

import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import fr.gouv.clea.consumer.model.DecodedVisit;
import fr.gouv.clea.consumer.model.Visit;
import fr.gouv.clea.consumer.service.IDecodedVisitService;
import fr.inria.clea.lsp.CleaEciesEncoder;
import fr.inria.clea.lsp.CleaEncodingException;
import fr.inria.clea.lsp.CleaEncryptionException;
import fr.inria.clea.lsp.LocationSpecificPart;
import fr.inria.clea.lsp.LocationSpecificPartDecoder;
import fr.inria.clea.lsp.utils.TimeUtils;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class DecodedVisitService implements IDecodedVisitService {

    private final LocationSpecificPartDecoder decoder;

    @Autowired
    public DecodedVisitService(LocationSpecificPartDecoder decoder) {
        this.decoder = decoder;
    }

    @Override
    public Optional<Visit> decryptAndValidate(DecodedVisit decodedVisit) {
        try {
            LocationSpecificPart lsp = this.decoder.decrypt(decodedVisit.getEncryptedLocationSpecificPart());
            Visit visit = Visit.from(lsp, decodedVisit);
            return this.verify(visit);
        } catch (CleaEncryptionException | CleaEncodingException e) {
            log.error("Cannot decrypt visit", e);
            return Optional.empty();
        }
    }

    private Optional<Visit> verify(Visit visit) {
        if (!this.isFresh(visit)) {
            log.warn("");  // FIXME
            return Optional.empty();
        } else if (!this.hasValidTemporaryLocationPublicId(visit)) {
            log.warn("");  // FIXME
            return Optional.empty();
        }
        log.info("");  // FIXME
        return Optional.of(this.setExposureTime(visit));
    }

    private boolean hasValidTemporaryLocationPublicId(Visit visit) {
        try {
            UUID computed = new CleaEciesEncoder().computeLocationTemporaryPublicId(visit.getLocationTemporarySecretKey());
            return computed.equals(visit.getLocationTemporaryPublicId());
        } catch (CleaEncryptionException e) {
            log.debug("Cannot check TemporaryLocationPublicId", e);
            return false;
        }
    }

    private boolean isFresh(Visit visit) {
        double qrCodeRenewalInterval = (visit.getQrCodeRenewalIntervalExponentCompact() == 0x1F)
                ? 0 : Math.pow(2, visit.getQrCodeRenewalIntervalExponentCompact());
        if (qrCodeRenewalInterval <= 0)
            return true;
        return Math.abs(TimeUtils.ntpTimestampFromInstant(visit.getQrCodeScanTime()) - visit.getQrCodeValidityStartTime()) < (qrCodeRenewalInterval + 300 + 300);
    }

    private Visit setExposureTime(Visit visit) {
        // FIXME implement when specs are precise
        return visit;
    }
}
