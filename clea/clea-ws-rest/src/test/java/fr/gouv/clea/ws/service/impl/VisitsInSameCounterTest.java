package fr.gouv.clea.ws.service.impl;

import fr.gouv.clea.ws.model.DecodedVisit;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class VisitsInSameCounterTest {

    @Mock
    private DecodedVisit decodedVisit;

    @Test
    void incrementIfScannedInSameTimeUnitThanLastScanTime_increments_counter_when_difference_between_scan_time_is_lower_than_exposureTimeUnit() {
        final long exposureTimeUnit = 1800L;
        final VisitsInSameUnitCounter counter = new VisitsInSameUnitCounter(exposureTimeUnit);
        final int initialCount = 0;
        counter.setCount(initialCount);

        final Instant lastScanTime = Instant.now().minusMillis(exposureTimeUnit - 1000);
        // 1s after
        final Instant currentVisitScanTime = Instant.now();
        counter.setLastScanTime(lastScanTime);
        when(decodedVisit.getQrCodeScanTime()).thenReturn(currentVisitScanTime);

        counter.incrementIfScannedInSameTimeUnitThanLastScanTime(decodedVisit);

        Assertions.assertThat(counter.getCount()).isEqualTo(initialCount+1);
    }

    @Test
    void incrementIfScannedInSameTimeUnitThanLastScanTime_does_not_increment_counter_when_difference_between_scan_time_is_greater_than_exposureTimeUnit() {
        final long exposureTimeUnit = 1800L;
        final VisitsInSameUnitCounter counter = new VisitsInSameUnitCounter(exposureTimeUnit);
        final int initialCount = 0;
        counter.setCount(initialCount);

        final Instant lastScanTime = Instant.now().minus(exposureTimeUnit + 1000, ChronoUnit.SECONDS);
        // 1s after
        final Instant currentVisitScanTime = Instant.now();
        counter.setLastScanTime(lastScanTime);
        when(decodedVisit.getQrCodeScanTime()).thenReturn(currentVisitScanTime);

        counter.incrementIfScannedInSameTimeUnitThanLastScanTime(decodedVisit);

        Assertions.assertThat(counter.getCount()).isEqualTo(initialCount);
    }
}
