package fr.gouv.tacw.services.impl;

import fr.gouv.tacw.dtos.DecodedLocationSpecificPart;
import fr.gouv.tacw.dtos.Report;
import fr.gouv.tacw.dtos.Reports;
import fr.gouv.tacw.services.IAuthorizationService;
import fr.gouv.tacw.services.IProducerService;
import fr.gouv.tacw.services.IReportService;
import fr.inria.clea.lsp.CleaEncryptionException;
import fr.inria.clea.lsp.LocationSpecificPart;
import fr.inria.clea.lsp.LocationSpecificPartDecoder;
import fr.inria.clea.lsp.utils.TimeUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ReportServiceTest {

    private final int retentionDuration = 14;
    private final long duplicateScanThreshold = 10800L;
    private final LocationSpecificPartDecoder decoder = mock(LocationSpecificPartDecoder.class);
    private final IProducerService processService = mock(IProducerService.class);
    private final IAuthorizationService authorizationService = mock(IAuthorizationService.class);
    private final IReportService reportService = new ReportService(retentionDuration, duplicateScanThreshold, decoder, processService, authorizationService);
    private Instant now;

    private static LocationSpecificPart createLocationSpecificPart(UUID locationTemporaryPublicId) {
        return LocationSpecificPart.builder()
                .locationTemporaryPublicId(locationTemporaryPublicId)
                .build();
    }

    @BeforeEach
    void init() {
        now = Instant.now();
        doNothing().when(processService).produce(anyList());
        when(authorizationService.checkAuthorization(any())).thenReturn(true);
    }

    @Test
    @DisplayName("test successful report with no rejections")
    void report() throws CleaEncryptionException {
        Reports reports = new Reports(
                List.of(
                        new Report("qr1", TimeUtils.ntpTimestampFromInstant(now.minus(2, ChronoUnit.DAYS))), // pass
                        new Report("qr2", TimeUtils.ntpTimestampFromInstant(now.minus(1, ChronoUnit.DAYS))), // pass
                        new Report("qr3", TimeUtils.ntpTimestampFromInstant(now)) // pass
                )
        );

        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();
        UUID uuid3 = UUID.randomUUID();

        LocationSpecificPart decoded1 = createLocationSpecificPart(uuid1);
        LocationSpecificPart decoded2 = createLocationSpecificPart(uuid2);
        LocationSpecificPart decoded3 = createLocationSpecificPart(uuid3);

        when(decoder.decrypt("qr1")).thenReturn(decoded1);
        when(decoder.decrypt("qr2")).thenReturn(decoded2);
        when(decoder.decrypt("qr3")).thenReturn(decoded3);

        List<DecodedLocationSpecificPart> processed = reportService.report("", reports);

        assertThat(processed.size()).isEqualTo(3);
        assertThat(processed.stream().filter(it -> it.getLocationTemporaryPublicId().equals(uuid1)).findAny()).isPresent();
        assertThat(processed.stream().filter(it -> it.getLocationTemporaryPublicId().equals(uuid2)).findAny()).isPresent();
        assertThat(processed.stream().filter(it -> it.getLocationTemporaryPublicId().equals(uuid3)).findAny()).isPresent();
    }

    @Test
    @DisplayName("test report with non valid qr codes")
    void testWithNonValidReports() throws CleaEncryptionException {
        Reports reports = new Reports(
                List.of(
                        new Report("qr1", TimeUtils.ntpTimestampFromInstant(now.minus(1, ChronoUnit.DAYS))), // pass
                        new Report("qr2", TimeUtils.ntpTimestampFromInstant(now)), // pass
                        new Report("qr3", TimeUtils.ntpTimestampFromInstant(now.plus(1, ChronoUnit.DAYS))) // don't pass
                )
        );

        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();
        UUID uuid3 = UUID.randomUUID();

        LocationSpecificPart decoded1 = createLocationSpecificPart(uuid1);
        LocationSpecificPart decoded2 = createLocationSpecificPart(uuid2);

        when(decoder.decrypt("qr1")).thenReturn(decoded1);
        when(decoder.decrypt("qr2")).thenReturn(decoded2);
        when(decoder.decrypt("qr3")).thenThrow(new CleaEncryptionException(new Exception()));

        List<DecodedLocationSpecificPart> processed = reportService.report("", reports);

        assertThat(processed.size()).isEqualTo(2);
        assertThat(processed.stream().filter(it -> it.getLocationTemporaryPublicId().equals(uuid1)).findAny()).isPresent();
        assertThat(processed.stream().filter(it -> it.getLocationTemporaryPublicId().equals(uuid2)).findAny()).isPresent();
        assertThat(processed.stream().filter(it -> it.getLocationTemporaryPublicId().equals(uuid3)).findAny()).isNotPresent();
    }

    @Test
    @DisplayName("test report with outdated scans")
    void testWithOutdatedReports() throws CleaEncryptionException {
        Reports reports = new Reports(
                List.of(
                        new Report("qr1", TimeUtils.ntpTimestampFromInstant(now.minus(15, ChronoUnit.DAYS))), // don't pass
                        new Report("qr2", TimeUtils.ntpTimestampFromInstant(now.minus(14, ChronoUnit.DAYS))), // pass
                        new Report("qr3", TimeUtils.ntpTimestampFromInstant(now.minus(2, ChronoUnit.DAYS))), // pass
                        new Report("qr4", TimeUtils.ntpTimestampFromInstant(now)) // pass
                )
        );

        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();
        UUID uuid3 = UUID.randomUUID();
        UUID uuid4 = UUID.randomUUID();

        LocationSpecificPart decoded1 = createLocationSpecificPart(uuid1);
        LocationSpecificPart decoded2 = createLocationSpecificPart(uuid2);
        LocationSpecificPart decoded3 = createLocationSpecificPart(uuid3);
        LocationSpecificPart decoded4 = createLocationSpecificPart(uuid4);

        when(decoder.decrypt("qr1")).thenReturn(decoded1);
        when(decoder.decrypt("qr2")).thenReturn(decoded2);
        when(decoder.decrypt("qr3")).thenReturn(decoded3);
        when(decoder.decrypt("qr4")).thenReturn(decoded4);

        List<DecodedLocationSpecificPart> processed = reportService.report("", reports);

        assertThat(processed.size()).isEqualTo(3);
        assertThat(processed.stream().filter(it -> it.getLocationTemporaryPublicId().equals(uuid1)).findAny()).isNotPresent();
        assertThat(processed.stream().filter(it -> it.getLocationTemporaryPublicId().equals(uuid2)).findAny()).isPresent();
        assertThat(processed.stream().filter(it -> it.getLocationTemporaryPublicId().equals(uuid3)).findAny()).isPresent();
        assertThat(processed.stream().filter(it -> it.getLocationTemporaryPublicId().equals(uuid4)).findAny()).isPresent();
    }

    @Test
    @DisplayName("test report with future scans")
    void testWithFutureReports() throws CleaEncryptionException {
        Reports reports = new Reports(
                List.of(
                        new Report("qr1", TimeUtils.ntpTimestampFromInstant(now)), // pass
                        new Report("qr2", TimeUtils.ntpTimestampFromInstant(now.plus(1, ChronoUnit.SECONDS))) // don't pass
                )
        );

        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();

        LocationSpecificPart decoded1 = createLocationSpecificPart(uuid1);
        LocationSpecificPart decoded2 = createLocationSpecificPart(uuid2);

        when(decoder.decrypt("qr1")).thenReturn(decoded1);
        when(decoder.decrypt("qr2")).thenReturn(decoded2);

        List<DecodedLocationSpecificPart> processed = reportService.report("", reports);

        assertThat(processed.size()).isEqualTo(1);
        assertThat(processed.stream().filter(it -> it.getLocationTemporaryPublicId().equals(uuid1)).findAny()).isPresent();
        assertThat(processed.stream().filter(it -> it.getLocationTemporaryPublicId().equals(uuid2)).findAny()).isNotPresent();
    }

    @Test
    @DisplayName("test report with duplicated qr codes")
    void testWithDuplicates() throws CleaEncryptionException {
        Reports reports = new Reports(
                List.of(
                        new Report("qrA1", TimeUtils.ntpTimestampFromInstant(now.minus(4, ChronoUnit.HOURS))), // pass
                        new Report("qrA2", TimeUtils.ntpTimestampFromInstant(now)), // pass
                        new Report("qrB1", TimeUtils.ntpTimestampFromInstant(now.minus(3, ChronoUnit.HOURS))), // pass
                        new Report("qrB2", TimeUtils.ntpTimestampFromInstant(now)), // don't pass
                        new Report("qrC1", TimeUtils.ntpTimestampFromInstant(now)), // pass
                        new Report("qrC2", TimeUtils.ntpTimestampFromInstant(now)) // don't pass
                )
        );

        UUID uuidA = UUID.randomUUID();
        UUID uuidB = UUID.randomUUID();
        UUID uuidC = UUID.randomUUID();

        LocationSpecificPart decodedA1 = createLocationSpecificPart(uuidA);
        LocationSpecificPart decodedA2 = createLocationSpecificPart(uuidA);
        LocationSpecificPart decodedB1 = createLocationSpecificPart(uuidB);
        LocationSpecificPart decodedB2 = createLocationSpecificPart(uuidB);
        LocationSpecificPart decodedC1 = createLocationSpecificPart(uuidC);
        LocationSpecificPart decodedC2 = createLocationSpecificPart(uuidC);

        when(decoder.decrypt("qrA1")).thenReturn(decodedA1);
        when(decoder.decrypt("qrA2")).thenReturn(decodedA2);
        when(decoder.decrypt("qrB1")).thenReturn(decodedB1);
        when(decoder.decrypt("qrB2")).thenReturn(decodedB2);
        when(decoder.decrypt("qrC1")).thenReturn(decodedC1);
        when(decoder.decrypt("qrC2")).thenReturn(decodedC2);

        List<DecodedLocationSpecificPart> processed = reportService.report("", reports);

        // FIXME assertThat(processed.size()).isEqualTo(4);
        // FIXME assertThat(processed.stream().filter(it -> it.getLocationTemporaryPublicId().equals(uuidA)).count()).isEqualTo(2);
        assertThat(processed.stream().filter(it -> it.getLocationTemporaryPublicId().equals(uuidB)).count()).isEqualTo(1);
        assertThat(processed.stream().filter(it -> it.getLocationTemporaryPublicId().equals(uuidC)).count()).isEqualTo(1);
    }

}