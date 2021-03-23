package fr.gouv.clea.ws.service.impl;

import fr.gouv.clea.ws.model.DecodedVisit;
import fr.gouv.clea.ws.service.IAuthorizationService;
import fr.gouv.clea.ws.service.IProducerService;
import fr.gouv.clea.ws.service.IReportService;
import fr.gouv.clea.ws.vo.ReportRequest;
import fr.gouv.clea.ws.vo.Visit;
import fr.inria.clea.lsp.*;
import fr.inria.clea.lsp.utils.TimeUtils;
import org.apache.tomcat.util.codec.binary.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

class ReportServiceTest {

    private final int retentionDuration = 14;
    private final long duplicateScanThresholdInSeconds = 10800L;
    private final LocationSpecificPartDecoder decoder = mock(LocationSpecificPartDecoder.class);
    private final IProducerService processService = mock(IProducerService.class);
    private final IAuthorizationService authorizationService = mock(IAuthorizationService.class);
    private final IReportService reportService = new ReportService(retentionDuration, duplicateScanThresholdInSeconds, decoder, processService, authorizationService);
    private Instant now;

    @BeforeEach
    void init() {
        now = Instant.now();
        assertThat(decoder).isNotNull();
        assertThat(processService).isNotNull();
        assertThat(reportService).isNotNull();
        doNothing().when(processService).produce(anyList());
        when(authorizationService.checkAuthorization(any())).thenReturn(true);
    }

    @Test
    @DisplayName("test successful report with no rejection")
    void report() throws CleaEncodingException {
        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();
        UUID uuid3 = UUID.randomUUID();
        List<Visit> visits = List.of(
                newVisit(uuid1, TimeUtils.ntpTimestampFromInstant(now.minus(2, ChronoUnit.DAYS))), // pass
                newVisit(uuid2, TimeUtils.ntpTimestampFromInstant(now.minus(1, ChronoUnit.DAYS))), // pass
                newVisit(uuid3, TimeUtils.ntpTimestampFromInstant(now)) /* pass */);

        List<DecodedVisit> processed = reportService.report("", new ReportRequest(visits, 0L));

        assertThat(processed.size()).isEqualTo(3);
        assertThat(processed.stream().filter(it -> it.getLocationTemporaryPublicId().equals(uuid1)).findAny()).isPresent();
        assertThat(processed.stream().filter(it -> it.getLocationTemporaryPublicId().equals(uuid2)).findAny()).isPresent();
        assertThat(processed.stream().filter(it -> it.getLocationTemporaryPublicId().equals(uuid3)).findAny()).isPresent();
    }

    @Test
    @DisplayName("test report with non valid qr codes")
    void testWithNonValidReports() throws CleaEncodingException {
        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();
        UUID uuid3 = UUID.randomUUID();
        List<Visit> visits = List.of(
                newVisit(uuid1, TimeUtils.ntpTimestampFromInstant(now.minus(1, ChronoUnit.DAYS))), // pass
                newVisit(uuid2, TimeUtils.ntpTimestampFromInstant(now)), // pass
                newVisit(uuid3, TimeUtils.ntpTimestampFromInstant(now.plus(1, ChronoUnit.DAYS))) /* don't pass */);


        List<DecodedVisit> processed = reportService.report("", new ReportRequest(visits, 0L));

        assertThat(processed.size()).isEqualTo(2);
        assertThat(processed.stream().filter(it -> it.getLocationTemporaryPublicId().equals(uuid1)).findAny()).isPresent();
        assertThat(processed.stream().filter(it -> it.getLocationTemporaryPublicId().equals(uuid2)).findAny()).isPresent();
        assertThat(processed.stream().filter(it -> it.getLocationTemporaryPublicId().equals(uuid3)).findAny()).isNotPresent();
    }

    @Test
    @DisplayName("test report with outdated scans")
    void testWithOutdatedReports() throws CleaEncodingException {
        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();
        UUID uuid3 = UUID.randomUUID();
        UUID uuid4 = UUID.randomUUID();
        List<Visit> visits = List.of(
                newVisit(uuid1, TimeUtils.ntpTimestampFromInstant(now.minus(15, ChronoUnit.DAYS))), // don't pass
                newVisit(uuid2, TimeUtils.ntpTimestampFromInstant(now.minus(14, ChronoUnit.DAYS))), // pass
                newVisit(uuid3, TimeUtils.ntpTimestampFromInstant(now.minus(2, ChronoUnit.DAYS))), // pass
                newVisit(uuid4, TimeUtils.ntpTimestampFromInstant(now)) /* pass */);


        List<DecodedVisit> processed = reportService.report("", new ReportRequest(visits, 0L));

        assertThat(processed.size()).isEqualTo(3);
        assertThat(processed.stream().filter(it -> it.getLocationTemporaryPublicId().equals(uuid1)).findAny()).isNotPresent();
        assertThat(processed.stream().filter(it -> it.getLocationTemporaryPublicId().equals(uuid2)).findAny()).isPresent();
        assertThat(processed.stream().filter(it -> it.getLocationTemporaryPublicId().equals(uuid3)).findAny()).isPresent();
        assertThat(processed.stream().filter(it -> it.getLocationTemporaryPublicId().equals(uuid4)).findAny()).isPresent();
    }

    @Test
    @DisplayName("test report with future scans")
    void testWithFutureReports() throws CleaEncodingException {
        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();
        List<Visit> visits = List.of(
                newVisit(uuid1, TimeUtils.ntpTimestampFromInstant(now)), // pass
                newVisit(uuid2, TimeUtils.ntpTimestampFromInstant(now.plus(1, ChronoUnit.SECONDS))) /* don't pass */);

        List<DecodedVisit> processed = reportService.report("", new ReportRequest(visits, 0L));

        assertThat(processed.size()).isEqualTo(1);
        assertThat(processed.stream().filter(it -> it.getLocationTemporaryPublicId().equals(uuid1)).findAny()).isPresent();
        assertThat(processed.stream().filter(it -> it.getLocationTemporaryPublicId().equals(uuid2)).findAny()).isNotPresent();
    }

    @Test
    @DisplayName("test report with duplicated qr codes")
    void testWithDuplicates() throws CleaEncodingException {
        UUID uuidA = UUID.randomUUID();
        UUID uuidB = UUID.randomUUID();
        UUID uuidC = UUID.randomUUID();
        List<Visit> visits = List.of(
                newVisit(uuidA, TimeUtils.ntpTimestampFromInstant(now.minus(4, ChronoUnit.HOURS))), // pass
                newVisit(uuidA, TimeUtils.ntpTimestampFromInstant(now)), // pass
                newVisit(uuidB, TimeUtils.ntpTimestampFromInstant(now.minus(3, ChronoUnit.HOURS))), // pass
                newVisit(uuidB, TimeUtils.ntpTimestampFromInstant(now)), // don't pass
                newVisit(uuidC, TimeUtils.ntpTimestampFromInstant(now)), // pass
                newVisit(uuidC, TimeUtils.ntpTimestampFromInstant(now)) /* don't pass */);

        List<DecodedVisit> processed = reportService.report("", new ReportRequest(visits, 0L));

        assertThat(processed.size()).isEqualTo(4);
        assertThat(processed.stream().filter(it -> it.getLocationTemporaryPublicId().equals(uuidA)).count()).isEqualTo(2);
        assertThat(processed.stream().filter(it -> it.getLocationTemporaryPublicId().equals(uuidB)).count()).isEqualTo(1);
        assertThat(processed.stream().filter(it -> it.getLocationTemporaryPublicId().equals(uuidC)).count()).isEqualTo(1);
    }

    private EncryptedLocationSpecificPart createEncryptedLocationSpecificPart(UUID locationTemporaryPublicId) {
        return EncryptedLocationSpecificPart.builder()
                .locationTemporaryPublicId(locationTemporaryPublicId)
                .build();
    }

    private Visit newVisit(UUID uuid, Long qrCodeScanTime) throws CleaEncodingException {
        LocationSpecificPart lsp = LocationSpecificPart.builder()
                .locationTemporaryPublicId(uuid)
                .build();
        byte[] qrCodeHeader = new LocationSpecificPartEncoder(null).binaryEncodedHeader(lsp);
        String qrCode = Base64.encodeBase64String(qrCodeHeader);
        when(decoder.decodeHeader(qrCodeHeader)).thenReturn(createEncryptedLocationSpecificPart(uuid));
        return new Visit(qrCode, qrCodeScanTime);
    }

}