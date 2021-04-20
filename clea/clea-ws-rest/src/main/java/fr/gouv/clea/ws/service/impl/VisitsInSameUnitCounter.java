package fr.gouv.clea.ws.service.impl;

import fr.gouv.clea.ws.model.DecodedVisit;
import lombok.Data;

import java.time.Duration;
import java.time.Instant;

@Data
public class VisitsInSameUnitCounter {

    private final long exposureTimeUnit;

    private int count;

    private Instant lastScanTime = null;

    public int incrementScanInSameUnitCount() {
        return ++count;
    }

    DecodedVisit incrementIfScannedInSameTimeUnitThanLastScanTime(final DecodedVisit decodedVisit) {
        final Instant qrCodeScanTime = decodedVisit.getQrCodeScanTime();
        if (this.getLastScanTime() == null) {
            this.setLastScanTime(qrCodeScanTime);
        } else if (visitIsScannedAfterLessThanExposureTime(qrCodeScanTime)) {
            this.incrementScanInSameUnitCount();
        }
        return decodedVisit;
    }

    private boolean visitIsScannedAfterLessThanExposureTime(Instant qrCodeScanTime) {
        return Duration.between(this.getLastScanTime(), qrCodeScanTime).getSeconds() < exposureTimeUnit;
    }
}
