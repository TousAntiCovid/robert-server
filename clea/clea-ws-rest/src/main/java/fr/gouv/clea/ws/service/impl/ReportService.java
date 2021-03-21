package fr.gouv.clea.ws.service.impl;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import fr.gouv.clea.ws.dto.Visit;
import fr.gouv.clea.ws.model.DecodedVisit;
import fr.gouv.clea.ws.service.IAuthorizationService;
import fr.gouv.clea.ws.service.IProcessService;
import fr.gouv.clea.ws.service.IReportService;
import fr.inria.clea.lsp.CleaEncodingException;
import fr.inria.clea.lsp.EncryptedLocationSpecificPart;
import fr.inria.clea.lsp.LocationSpecificPartDecoder;
import fr.inria.clea.lsp.utils.TimeUtils;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ReportService implements IReportService {

    private final int retentionDuration;

    private final long duplicateScanThreshold;

    private final LocationSpecificPartDecoder decoder;

    private final IProcessService processService;

    private final IAuthorizationService authorizationService;

    @Autowired
    public ReportService(
            @Value("${clea.conf.retentionDuration}") int retentionDuration,
            @Value("${clea.conf.duplicateScanThreshold}") long duplicateScanThreshold,
            LocationSpecificPartDecoder decoder,
            IProcessService processService,
            IAuthorizationService authorizationService) {
        this.retentionDuration = retentionDuration;
        this.duplicateScanThreshold = duplicateScanThreshold;
        this.decoder = decoder;
        this.processService = processService;
        this.authorizationService = authorizationService;
    }

    @Override
    public List<DecodedVisit> report(String jwtToken, List<Visit> visits) {
        this.authorizationService.checkAuthorization(jwtToken);

        List<DecodedVisit> verified = visits.stream()
                .filter(visit -> ! this.isOutdated(visit))
                .filter(visit -> ! this.isFuture(visit))
                .map(this::decode)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        List<DecodedVisit> pruned = this.pruneDuplicates(verified);
        processService.process(pruned);
        return pruned;
    }

    private DecodedVisit decode(Visit visit) {
        try {
            byte[] binaryLocationSpecificPart = Base64.getDecoder().decode(visit.getQrCode());
            EncryptedLocationSpecificPart encryptedLocationSpecificPart = decoder.decodeHeader(binaryLocationSpecificPart);
            return new DecodedVisit(visit.getQrCodeScanTime(), encryptedLocationSpecificPart);
        } catch (CleaEncodingException e) {
            log.warn("report: {}... rejected: Invalid format", this.truncateQrCode(visit.getQrCode()));
            return null;
        }
    }

    private boolean isOutdated(Visit visit) {
        boolean outdated = ChronoUnit.DAYS.between(TimeUtils.instantFromTimestamp(visit.getQrCodeScanTime()), Instant.now()) > retentionDuration; // FIXME < OR <=
        if (outdated) {
            log.warn("report: {} ... rejected: Outdated", this.truncateQrCode(visit.getQrCode()));
        }
        return outdated;
    }

    private boolean isFuture(Visit visit) {
        boolean future = TimeUtils.instantFromTimestamp(visit.getQrCodeScanTime()).isAfter(Instant.now());
        if (future) {
            log.warn("report: {} ... rejected: In future", this.truncateQrCode(visit.getQrCode()));
        }
        return future;
    }
    
    private boolean isDuplicatedScan(DecodedVisit lsp, List<DecodedVisit> cleaned) {
        return cleaned.stream().anyMatch(cleanedLsp -> this.isDuplicatedScan(lsp, cleanedLsp));
    }

    private boolean isDuplicatedScan(DecodedVisit one, DecodedVisit other) {
        if (one.getLocationTemporaryPublicId() != other.getLocationTemporaryPublicId()) {
            return false;
        }
        
        if (Math.abs(one.getQrCodeScanTime() - other.getQrCodeScanTime()) <= duplicateScanThreshold) { // FIXME < OR <=
            log.warn("report: {} {} rejected: Duplicate", one.getLocationTemporaryPublicId(), one.getQrCodeScanTime());
            return true;
        }
        return false;
    }
    
    private List<DecodedVisit> pruneDuplicates(List<DecodedVisit> locationSpecificParts) {
        List<DecodedVisit> cleaned = new ArrayList<>();
        locationSpecificParts.forEach(it -> {
            if ( !this.isDuplicatedScan(it, cleaned) ) {
                cleaned.add(it);
            }
        });
        return cleaned;
    }

    private String truncateQrCode(String qrCode) {
        return qrCode.substring(0, Math.min(qrCode.length(), 25));
    }
}
