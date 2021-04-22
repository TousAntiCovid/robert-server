package fr.gouv.clea.ws.service.impl;

import fr.gouv.clea.ws.configuration.CleaWsProperties;
import fr.gouv.clea.ws.model.DecodedVisit;
import fr.gouv.clea.ws.service.IDecodedVisitProducerService;
import fr.gouv.clea.ws.service.IReportService;
import fr.gouv.clea.ws.utils.MessageFormatter;
import fr.gouv.clea.ws.vo.ReportRequest;
import fr.gouv.clea.ws.vo.Visit;
import fr.inria.clea.lsp.EncryptedLocationSpecificPart;
import fr.inria.clea.lsp.LocationSpecificPartDecoder;
import fr.inria.clea.lsp.exception.CleaEncodingException;
import fr.inria.clea.lsp.utils.TimeUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ReportService implements IReportService {

    private final CleaWsProperties cleaWsProperties;

    private final LocationSpecificPartDecoder decoder;

    private final IDecodedVisitProducerService processService;


    @Autowired
    public ReportService(
            CleaWsProperties cleaWsProperties,
            LocationSpecificPartDecoder decoder,
            IDecodedVisitProducerService processService
    ) {
        this.cleaWsProperties = cleaWsProperties;
        this.decoder = decoder;
        this.processService = processService;
    }

    @Override
    public List<DecodedVisit> report(ReportRequest reportRequestVo) {
        final Instant now = Instant.now();
        final VisitsInSameUnitCounter closeScanTimeVisits = new VisitsInSameUnitCounter(cleaWsProperties.getExposureTimeUnitInSeconds());
        long validatedPivotDate = this.validatePivotDate(reportRequestVo.getPivotDateAsNtpTimestamp(), now);
        List<Visit> reportVisits = reportRequestVo.getVisits();
        List<DecodedVisit> verified = reportVisits.stream()
                .filter(visit -> !isOutdated(visit, now))
                .filter(visit -> !isFuture(visit, now))
                .map(it -> this.decode(it, validatedPivotDate))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        List<DecodedVisit> pruned = this.pruneDuplicates(verified);
        processService.produce(pruned);

        // evaluate produced visits and count close scan time visits
        pruned.stream().sorted(Comparator.comparing((DecodedVisit::getQrCodeScanTime)))
                .forEach(closeScanTimeVisits::incrementIfScannedInSameTimeUnitThanLastScanTime);

        log.info("BATCH_REPORT {}#{}#{}#{}#{}", reportVisits.size(), reportVisits.size() - pruned.size(),
                pruned.stream().filter(DecodedVisit::isBackward).count(),
                pruned.stream().filter(DecodedVisit::isForward).count(),
                closeScanTimeVisits.getCount());
        return pruned;
    }

    private DecodedVisit decode(Visit visit, long pivotDate) {
        try {
            byte[] binaryLocationSpecificPart = Base64.getUrlDecoder().decode(visit.getQrCode());
            EncryptedLocationSpecificPart encryptedLocationSpecificPart = decoder.decodeHeader(binaryLocationSpecificPart);
            Instant qrCodeScanTime = TimeUtils.instantFromTimestamp(visit.getQrCodeScanTimeAsNtpTimestamp());
            return new DecodedVisit(qrCodeScanTime, encryptedLocationSpecificPart, visit.getQrCodeScanTimeAsNtpTimestamp() < pivotDate);
        } catch (CleaEncodingException e) {
            log.warn("report: {} rejected: Invalid format", MessageFormatter.truncateQrCode(visit.getQrCode()));
            return null;
        }
    }

    private boolean isOutdated(Visit visit, Instant now) {
        boolean outdated = ChronoUnit.DAYS.between(TimeUtils.instantFromTimestamp(visit.getQrCodeScanTimeAsNtpTimestamp()), now) > cleaWsProperties.getRetentionDurationInDays();
        if (outdated) {
            log.warn("report: {} rejected: Outdated", MessageFormatter.truncateQrCode(visit.getQrCode()));
        }
        return outdated;
    }

    private boolean isFuture(Visit visit, Instant now) {
        boolean future = TimeUtils.instantFromTimestamp(visit.getQrCodeScanTimeAsNtpTimestamp()).isAfter(now);
        if (future) {
            log.warn("report: {} rejected: In future", MessageFormatter.truncateQrCode(visit.getQrCode()));
        }
        return future;
    }

    private boolean isDuplicatedScan(DecodedVisit lsp, List<DecodedVisit> cleaned) {
        return cleaned.stream().anyMatch(cleanedLsp -> this.isDuplicatedScan(lsp, cleanedLsp));
    }

    private boolean isDuplicatedScan(DecodedVisit one, DecodedVisit other) {
        if (!one.getLocationTemporaryPublicId().equals(other.getLocationTemporaryPublicId())) {
            return false;
        }

        long secondsBetweenScans = Duration.between(one.getQrCodeScanTime(), other.getQrCodeScanTime()).abs().toSeconds();
        if (secondsBetweenScans <= cleaWsProperties.getDuplicateScanThresholdInSeconds()) {
            log.warn("report: {} {} rejected: Duplicate", MessageFormatter.truncateUUID(one.getStringLocationTemporaryPublicId()), one.getQrCodeScanTime());
            return true;
        }
        return false;
    }

    private List<DecodedVisit> pruneDuplicates(List<DecodedVisit> locationSpecificParts) {
        List<DecodedVisit> cleaned = new ArrayList<>();
        locationSpecificParts.forEach(it -> {
            if (!this.isDuplicatedScan(it, cleaned)) {
                cleaned.add(it);
            }
        });
        return cleaned;
    }

    private long validatePivotDate(long pivotDate, Instant now) {
        Instant pivotDateAsInstant = TimeUtils.instantFromTimestamp(pivotDate);
        Instant nowWithoutMilis = now.truncatedTo(ChronoUnit.SECONDS);
        Instant retentionDateLimit = nowWithoutMilis.minus(cleaWsProperties.getRetentionDurationInDays(), ChronoUnit.DAYS);
        if (pivotDateAsInstant.isAfter(now) || pivotDateAsInstant.isBefore(retentionDateLimit)) {
            long retentionDateLimitAsNtp = TimeUtils.ntpTimestampFromInstant(retentionDateLimit);
            log.warn("pivotDate: {} not between retentionLimitDate: {} and now: {}", pivotDateAsInstant, retentionDateLimit, now);
            return retentionDateLimitAsNtp;
        } else {
            return pivotDate;
        }
    }
}
