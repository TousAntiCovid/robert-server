package fr.gouv.clea.consumer.service.impl;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import fr.gouv.clea.consumer.configuration.VenueConsumerConfiguration;
import fr.gouv.clea.consumer.model.DecodedVisit;
import fr.gouv.clea.consumer.model.Visit;
import fr.gouv.clea.consumer.service.IDecodedVisitService;
import fr.gouv.clea.consumer.utils.MessageFormatter;
import fr.inria.clea.lsp.CleaEciesEncoder;
import fr.inria.clea.lsp.LocationSpecificPart;
import fr.inria.clea.lsp.LocationSpecificPartDecoder;
import fr.inria.clea.lsp.exception.CleaEncryptionException;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class DecodedVisitService implements IDecodedVisitService {

    private final LocationSpecificPartDecoder decoder;

    private final CleaEciesEncoder cleaEciesEncoder;

    private final VenueConsumerConfiguration config;

    @Autowired
    public DecodedVisitService(
            LocationSpecificPartDecoder decoder,
            CleaEciesEncoder cleaEciesEncoder,
            VenueConsumerConfiguration config) {
        this.decoder = decoder;
        this.cleaEciesEncoder = cleaEciesEncoder;
        this.config = config;
    }

    @Override
    public Optional<Visit> decryptAndValidate(DecodedVisit decodedVisit) {
        try {
            LocationSpecificPart lsp = this.decoder.decrypt(decodedVisit.getEncryptedLocationSpecificPart());
            Visit visit = Visit.from(lsp, decodedVisit);
            return this.verify(visit);
        } catch (Exception e) {
            log.warn("error decrypting [locationTemporaryPublicId: {}, qrCodeScanTime: {}, message: {}]", MessageFormatter.truncateUUID(decodedVisit.getStringLocationTemporaryPublicId()), decodedVisit.getQrCodeScanTime(), e.getLocalizedMessage());
            return Optional.empty();
        }
    }

    private Optional<Visit> verify(Visit visit) {
        if (this.isDrifting(visit)) {
            log.warn("drift check failed for [locationTemporaryPublicId: {}, qrCodeScanTime: {}]", MessageFormatter.truncateUUID(visit.getStringLocationTemporaryPublicId()), visit.getQrCodeScanTime());
            return Optional.empty();
        } else if (!this.hasValidTemporaryLocationPublicId(visit)) {
            log.warn("locationTemporaryPublicId check failed for [locationTemporaryPublicId: {}, qrCodeScanTime: {}]", MessageFormatter.truncateUUID(visit.getStringLocationTemporaryPublicId()), visit.getQrCodeScanTime());
            return Optional.empty();
        }
        return Optional.of(visit);
    }

    private boolean hasValidTemporaryLocationPublicId(Visit visit) {
        try {
            UUID computed = cleaEciesEncoder.computeLocationTemporaryPublicId(visit.getLocationTemporarySecretKey());
            return computed.equals(visit.getLocationTemporaryPublicId());
        } catch (CleaEncryptionException e) {
            log.warn("locationTemporaryPublicId check failed for [locationTemporaryPublicId: {}, qrCodeScanTime: {}]", MessageFormatter.truncateUUID(visit.getStringLocationTemporaryPublicId()), visit.getQrCodeScanTime());
            return false;
        }
    }

    private boolean isDrifting(Visit visit) {
        double qrCodeRenewalInterval = (visit.getQrCodeRenewalIntervalExponentCompact() == 0x1F)
                ? 0 : Math.pow(2, visit.getQrCodeRenewalIntervalExponentCompact());
        if (qrCodeRenewalInterval == 0) {
            return false;
        }
        return Duration.between(visit.getQrCodeScanTime(), visit.getQrCodeValidityStartTime()).abs().toSeconds() 
                > (qrCodeRenewalInterval + config.getDriftBetweenDeviceAndOfficialTimeInSecs() + config.getCleaClockDriftInSecs());
    }
}
