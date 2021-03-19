package fr.gouv.tacw.services.impl;

import fr.gouv.tacw.data.DecodedLocationSpecificPart;
import fr.gouv.tacw.dtos.Report;
import fr.gouv.tacw.dtos.Reports;
import fr.gouv.tacw.services.IAuthorizationService;
import fr.gouv.tacw.services.IProducerService;
import fr.gouv.tacw.services.IReportService;
import fr.inria.clea.lsp.CleaEncryptionException;
import fr.inria.clea.lsp.LocationSpecificPart;
import fr.inria.clea.lsp.LocationSpecificPartDecoder;
import fr.inria.clea.lsp.utils.TimeUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ReportService implements IReportService {

    private final int retentionDuration;

    private final long duplicateScanThreshold;

    private final LocationSpecificPartDecoder decoder;

    private final IProducerService processService;

    private final IAuthorizationService authorizationService;

    @Autowired
    public ReportService(
            @Value("${clea.conf.retentionDuration}") int retentionDuration,
            @Value("${clea.conf.duplicateScanThreshold}") long duplicateScanThreshold,
            LocationSpecificPartDecoder decoder,
            IProducerService processService,
            IAuthorizationService authorizationService
    ) {
        this.retentionDuration = retentionDuration;
        this.duplicateScanThreshold = duplicateScanThreshold;
        this.decoder = decoder;
        this.processService = processService;
        this.authorizationService = authorizationService;
    }

    @Override
    public List<DecodedLocationSpecificPart> report(String jwtToken, Reports body) {
        this.authorizationService.checkAuthorization(jwtToken);

        List<DecodedLocationSpecificPart> verified = body.getReports().stream()
                .map(this::verify)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        List<DecodedLocationSpecificPart> pruned = this.pruneDuplicates(verified);
        processService.produce(List.of());
        return pruned;
    }

    private DecodedLocationSpecificPart verify(Report report) {
        try {
            LocationSpecificPart lsp = decoder.decrypt(report.getQrCode());
            if (!this.isCurrent(report)) {
                log.warn("report: " + this.truncateQrCode(report.getQrCode()) + "... rejected: Outdated");
                return null;
            } else if (this.isFuture(report)) {
                log.warn("report: " + this.truncateQrCode(report.getQrCode()) + "... rejected: In future");
                return null;
            } else {
                return DecodedLocationSpecificPart.fromLocationSpecificPart(lsp, report.getQrCodeScanTime(), report.getQrCode());
            }
        } catch (CleaEncryptionException e) {
            log.warn("report: " + this.truncateQrCode(report.getQrCode()) + "... rejected: Invalid format");
            return null;
        }
    }

    private boolean isCurrent(Report report) {
        return ChronoUnit.DAYS.between(TimeUtils.instantFromTimestamp(report.getQrCodeScanTime()), Instant.now()) <= retentionDuration; // FIXME < OR <=
    }

    private boolean isFuture(Report report) {
        return TimeUtils.instantFromTimestamp(report.getQrCodeScanTime()).isAfter(Instant.now());
    }

    private List<DecodedLocationSpecificPart> pruneDuplicates(List<DecodedLocationSpecificPart> dlsParts) {
        Map<UUID, DecodedLocationSpecificPart> cleaned = new HashMap<>();
        dlsParts.forEach(it -> {
            UUID currentUIID = it.getLocationTemporaryPublicId();
            if (cleaned.containsKey(currentUIID)) {
                long existingScan = cleaned.get(currentUIID).getQrCodeScanTime();
                long currentScan = it.getQrCodeScanTime();
                if (Math.abs(existingScan - currentScan) <= duplicateScanThreshold) { // FIXME < OR <=
                    if (currentScan < existingScan) {
                        log.warn("report: " + this.truncateQrCode(it.getQrCode()) + "... rejected: Duplicate");
                        cleaned.replace(currentUIID, it);
                    }
                } else {
                    cleaned.put(currentUIID, it);
                }
            } else {
                cleaned.put(currentUIID, it);
            }
        });
        return new ArrayList<>(cleaned.values());
    }

    private String truncateQrCode(String qrCode) {
        return qrCode.substring(0, Math.min(qrCode.length(), 25));
    }
}
