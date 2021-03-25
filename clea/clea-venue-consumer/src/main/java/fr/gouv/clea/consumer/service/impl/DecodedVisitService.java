package fr.gouv.clea.consumer.service.impl;

import fr.gouv.clea.consumer.model.DecodedVisit;
import fr.gouv.clea.consumer.model.Visit;
import fr.gouv.clea.consumer.service.IDecodedVisitService;
import fr.gouv.clea.consumer.utils.MessageFormatter;
import fr.inria.clea.lsp.CleaEciesEncoder;
import fr.inria.clea.lsp.CleaEncryptionException;
import fr.inria.clea.lsp.LocationSpecificPart;
import fr.inria.clea.lsp.LocationSpecificPartDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@Slf4j
public class DecodedVisitService implements IDecodedVisitService {

    private final LocationSpecificPartDecoder decoder;

    private final CleaEciesEncoder cleaEciesEncoder;

    private final int driftBetweenDeviceAndOfficialTimeInSecs;

    private final int cleaClockDriftInSecs;

    @Autowired
    public DecodedVisitService(
            LocationSpecificPartDecoder decoder,
            CleaEciesEncoder cleaEciesEncoder,
            @Value("${clea.conf.driftBetweenDeviceAndOfficialTimeInSecs}") int driftBetweenDeviceAndOfficialTimeInSecs,
            @Value("${clea.conf.cleaClockDriftInSecs}") int cleaClockDriftInSecs
    ) {
        this.decoder = decoder;
        this.cleaEciesEncoder = cleaEciesEncoder;
        this.driftBetweenDeviceAndOfficialTimeInSecs = driftBetweenDeviceAndOfficialTimeInSecs;
        this.cleaClockDriftInSecs = cleaClockDriftInSecs;
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
        if (!this.isFresh(visit)) {
            log.warn("drift check failed for [locationTemporaryPublicId: {}, qrCodeScanTime: {}]", MessageFormatter.truncateUUID(visit.getStringLocationTemporaryPublicId()), visit.getQrCodeScanTime());
            return Optional.empty();
        } else if (!this.hasValidTemporaryLocationPublicId(visit)) {
            return Optional.empty();
        }
        return Optional.of(this.setExposureTime(visit));
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

    private boolean isFresh(Visit visit) {
        return true;
        /*
        double qrCodeRenewalInterval = (visit.getQrCodeRenewalIntervalExponentCompact() == 0x1F)
                ? 0 : Math.pow(2, visit.getQrCodeRenewalIntervalExponentCompact());
        if (qrCodeRenewalInterval == 0)
            return true;
        return Math.abs(TimeUtils.ntpTimestampFromInstant(visit.getQrCodeScanTime()) - visit.getQrCodeValidityStartTime()) < (qrCodeRenewalInterval + driftBetweenDeviceAndOfficialTimeInSecs + cleaClockDriftInSecs);
         */
    }

    private Visit setExposureTime(Visit visit) {
        // FIXME implement when specs are precise
        return visit;
    }
}
