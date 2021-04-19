package fr.gouv.clea.ws.service.impl;

import fr.gouv.clea.ws.model.DecodedVisit;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;

import java.time.Instant;

@Data
public class VisitsInSameUnitCounter {

    @Value("${clea.conf.exposureTimeUnitInSeconds}")
    private long exposureTimeUnit;

    private int count;

    private Instant lastScanTime = null;

    public int incrementScanInSameUnitCount() {
        return ++count;
    }

    DecodedVisit incrementIfScannedInSameTimeUnitThanLastScanTime(final DecodedVisit decodedVisit) {
        final Instant qrCodeScanTime = decodedVisit.getQrCodeScanTime();
        if (this.getLastScanTime() == null) {
            this.setLastScanTime(qrCodeScanTime);
        } else {
            if (decodedVisit.isBackward()) {
                if (backwardsVisitIsScannedAfterLessThanExposureTime(qrCodeScanTime)) {
                    this.incrementScanInSameUnitCount();
                } else {
                    this.setLastScanTime(qrCodeScanTime);
                }
            } else {
                if (forwardVisitIsScannedAfterLessThanExposureTime(qrCodeScanTime)) {
                    this.incrementScanInSameUnitCount();
                } else {
                    this.setLastScanTime(qrCodeScanTime);
                }

            }
        }
        return decodedVisit;
    }

    private boolean forwardVisitIsScannedAfterLessThanExposureTime(Instant qrCodeScanTime) {
        return this.getLastScanTime().compareTo(qrCodeScanTime) >= exposureTimeUnit;
    }

    private boolean backwardsVisitIsScannedAfterLessThanExposureTime(Instant qrCodeScanTime) {
        return this.getLastScanTime().compareTo(qrCodeScanTime) < exposureTimeUnit;
    }
}
