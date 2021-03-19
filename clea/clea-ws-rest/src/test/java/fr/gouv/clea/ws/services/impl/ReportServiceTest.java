package fr.gouv.clea.ws.services.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import fr.gouv.clea.ws.dto.Visit;
import fr.gouv.clea.ws.model.DecodedLocationSpecificPart;
import fr.gouv.clea.ws.service.IAuthorizationService;
import fr.gouv.clea.ws.service.IProcessService;
import fr.gouv.clea.ws.service.IReportService;
import fr.gouv.clea.ws.service.impl.ReportService;
import fr.inria.clea.lsp.CleaEncryptionException;
import fr.inria.clea.lsp.LocationSpecificPart;
import fr.inria.clea.lsp.LocationSpecificPartDecoder;
import fr.inria.clea.lsp.utils.TimeUtils;

class ReportServiceTest {

    private final int retentionDuration = 14;
    private final long duplicateScanThreshold = 10800L;
    private final LocationSpecificPartDecoder decoder = mock(LocationSpecificPartDecoder.class);
    private final IProcessService processService = mock(IProcessService.class);
    private final IAuthorizationService authorizationService = mock(IAuthorizationService.class);
    private final IReportService reportService = new ReportService(retentionDuration, duplicateScanThreshold, decoder, processService, authorizationService);
    private Instant now;

    @BeforeEach
    void init() {
        now = Instant.now();
        assertThat(decoder).isNotNull();
        assertThat(processService).isNotNull();
        assertThat(reportService).isNotNull();
        doNothing().when(processService).process(anyList());
        when(authorizationService.checkAuthorization(any())).thenReturn(true);
    }

    @Test
    @DisplayName("test successful report with no rejection")
    void report() throws CleaEncryptionException {
        List<Visit> visits = List.of(
            new Visit("qr1", TimeUtils.ntpTimestampFromInstant(now.minus(2, ChronoUnit.DAYS))), // pass
            new Visit("qr2", TimeUtils.ntpTimestampFromInstant(now.minus(1, ChronoUnit.DAYS))), // pass
            new Visit("qr3", TimeUtils.ntpTimestampFromInstant(now)) /* pass */);
        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();
        UUID uuid3 = UUID.randomUUID();
        when(decoder.decrypt("qr1")).thenReturn(createLocationSpecificPart(uuid1));
        when(decoder.decrypt("qr2")).thenReturn(createLocationSpecificPart(uuid2));
        when(decoder.decrypt("qr3")).thenReturn(createLocationSpecificPart(uuid3));
        
        List<DecodedLocationSpecificPart> processed = reportService.report("", visits);
        
        assertThat(processed.size()).isEqualTo(3);
        assertThat(processed.stream().filter(it -> it.getLocationTemporaryPublicId().equals(uuid1)).findAny()).isPresent();
        assertThat(processed.stream().filter(it -> it.getLocationTemporaryPublicId().equals(uuid2)).findAny()).isPresent();
        assertThat(processed.stream().filter(it -> it.getLocationTemporaryPublicId().equals(uuid3)).findAny()).isPresent();
    }

    @Test
    @DisplayName("test report with non valid qr codes")
    void testWithNonValidReports() throws CleaEncryptionException {
        List<Visit> visits = List.of(
            new Visit("qr1", TimeUtils.ntpTimestampFromInstant(now.minus(1, ChronoUnit.DAYS))), // pass
            new Visit("qr2", TimeUtils.ntpTimestampFromInstant(now)), // pass
            new Visit("qr3", TimeUtils.ntpTimestampFromInstant(now.plus(1, ChronoUnit.DAYS))) /* don't pass */ );
        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();
        UUID uuid3 = UUID.randomUUID();
        when(decoder.decrypt("qr1")).thenReturn(createLocationSpecificPart(uuid1));
        when(decoder.decrypt("qr2")).thenReturn(createLocationSpecificPart(uuid2));
        when(decoder.decrypt("qr3")).thenThrow(new CleaEncryptionException(new Exception()));

        List<DecodedLocationSpecificPart> processed = reportService.report("", visits);
        
        assertThat(processed.size()).isEqualTo(2);
        assertThat(processed.stream().filter(it -> it.getLocationTemporaryPublicId().equals(uuid1)).findAny()).isPresent();
        assertThat(processed.stream().filter(it -> it.getLocationTemporaryPublicId().equals(uuid2)).findAny()).isPresent();
        assertThat(processed.stream().filter(it -> it.getLocationTemporaryPublicId().equals(uuid3)).findAny()).isNotPresent();
    }

    @Test
    @DisplayName("test report with outdated scans")
    void testWithOutdatedReports() throws CleaEncryptionException {
        List<Visit> visits = List.of(
                        new Visit("qr1", TimeUtils.ntpTimestampFromInstant(now.minus(15, ChronoUnit.DAYS))), // don't pass
                        new Visit("qr2", TimeUtils.ntpTimestampFromInstant(now.minus(14, ChronoUnit.DAYS))), // pass
                        new Visit("qr3", TimeUtils.ntpTimestampFromInstant(now.minus(2, ChronoUnit.DAYS))), // pass
                        new Visit("qr4", TimeUtils.ntpTimestampFromInstant(now)) /* pass */);
        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();
        UUID uuid3 = UUID.randomUUID();
        UUID uuid4 = UUID.randomUUID();
        when(decoder.decrypt("qr1")).thenReturn(createLocationSpecificPart(uuid1));
        when(decoder.decrypt("qr2")).thenReturn(createLocationSpecificPart(uuid2));
        when(decoder.decrypt("qr3")).thenReturn(createLocationSpecificPart(uuid3));
        when(decoder.decrypt("qr4")).thenReturn(createLocationSpecificPart(uuid4));
        
        List<DecodedLocationSpecificPart> processed = reportService.report("", visits);
        
        assertThat(processed.size()).isEqualTo(3);
        assertThat(processed.stream().filter(it -> it.getLocationTemporaryPublicId().equals(uuid1)).findAny()).isNotPresent();
        assertThat(processed.stream().filter(it -> it.getLocationTemporaryPublicId().equals(uuid2)).findAny()).isPresent();
        assertThat(processed.stream().filter(it -> it.getLocationTemporaryPublicId().equals(uuid3)).findAny()).isPresent();
        assertThat(processed.stream().filter(it -> it.getLocationTemporaryPublicId().equals(uuid4)).findAny()).isPresent();
    }

    @Test
    @DisplayName("test report with future scans")
    void testWithFutureReports() throws CleaEncryptionException {
        List<Visit> visits = List.of(
            new Visit("qr1", TimeUtils.ntpTimestampFromInstant(now)), // pass
            new Visit("qr2", TimeUtils.ntpTimestampFromInstant(now.plus(1, ChronoUnit.SECONDS))) /* don't pass */);
        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();
        when(decoder.decrypt("qr1")).thenReturn(createLocationSpecificPart(uuid1));
        when(decoder.decrypt("qr2")).thenReturn(createLocationSpecificPart(uuid2));
        
        List<DecodedLocationSpecificPart> processed = reportService.report("", visits);
        
        assertThat(processed.size()).isEqualTo(1);
        assertThat(processed.stream().filter(it -> it.getLocationTemporaryPublicId().equals(uuid1)).findAny()).isPresent();
        assertThat(processed.stream().filter(it -> it.getLocationTemporaryPublicId().equals(uuid2)).findAny()).isNotPresent();
    }

    @Test
    @DisplayName("test report with duplicated qr codes")
    void testWithDuplicates() throws CleaEncryptionException {
        List<Visit> visits = List.of(
            new Visit("qrA1", TimeUtils.ntpTimestampFromInstant(now.minus(4, ChronoUnit.HOURS))), // pass
            new Visit("qrA2", TimeUtils.ntpTimestampFromInstant(now)), // pass
            new Visit("qrB1", TimeUtils.ntpTimestampFromInstant(now.minus(3, ChronoUnit.HOURS))), // pass
            new Visit("qrB2", TimeUtils.ntpTimestampFromInstant(now)), // don't pass
            new Visit("qrC1", TimeUtils.ntpTimestampFromInstant(now)), // pass
            new Visit("qrC2", TimeUtils.ntpTimestampFromInstant(now)) /* don't pass */ );
        UUID uuidA = UUID.randomUUID();
        UUID uuidB = UUID.randomUUID();
        UUID uuidC = UUID.randomUUID();
        when(decoder.decrypt("qrA1")).thenReturn(createLocationSpecificPart(uuidA));
        when(decoder.decrypt("qrA2")).thenReturn(createLocationSpecificPart(uuidA));
        when(decoder.decrypt("qrB1")).thenReturn(createLocationSpecificPart(uuidB));
        when(decoder.decrypt("qrB2")).thenReturn(createLocationSpecificPart(uuidB));
        when(decoder.decrypt("qrC1")).thenReturn(createLocationSpecificPart(uuidC));
        when(decoder.decrypt("qrC2")).thenReturn(createLocationSpecificPart(uuidC));
        
        List<DecodedLocationSpecificPart> processed = reportService.report("", visits);
        
        assertThat(processed.size()).isEqualTo(4);
        assertThat(processed.stream().filter(it -> it.getLocationTemporaryPublicId().equals(uuidA)).count()).isEqualTo(2);
        assertThat(processed.stream().filter(it -> it.getLocationTemporaryPublicId().equals(uuidB)).count()).isEqualTo(1);
        assertThat(processed.stream().filter(it -> it.getLocationTemporaryPublicId().equals(uuidC)).count()).isEqualTo(1);
    }

    private static LocationSpecificPart createLocationSpecificPart(UUID locationTemporaryPublicId) {
        return LocationSpecificPart.builder()
                .locationTemporaryPublicId(locationTemporaryPublicId)
                .build();
    }

}