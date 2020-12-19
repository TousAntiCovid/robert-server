package fr.gouv.tacw.ws.service.impl;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.transaction.Transactional;

import org.springframework.stereotype.Service;

import fr.gouv.tacw.database.model.ExposedStaticVisitEntity;
import fr.gouv.tacw.database.service.ExposedStaticVisitService;
import fr.gouv.tacw.database.utils.TimeUtils;
import fr.gouv.tacw.model.OpaqueVisit;
import fr.gouv.tacw.model.RiskLevel;
import fr.gouv.tacw.ws.configuration.TacWarningWsRestConfiguration;
import fr.gouv.tacw.ws.service.ExposedTokenGeneratorService;
import fr.gouv.tacw.ws.service.WarningService;
import fr.gouv.tacw.ws.vo.ReportRequestVo;
import fr.gouv.tacw.ws.vo.VisitVo;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class WarningServiceImpl implements WarningService {
    private ExposedStaticVisitService exposedStaticVisitService;
    private TacWarningWsRestConfiguration configuration;
    private ExposedTokenGeneratorService exposedTokenGeneratorService;

    public WarningServiceImpl(ExposedStaticVisitService exposedStaticVisitService, TacWarningWsRestConfiguration configuration, ExposedTokenGeneratorService exposedTokenGeneratorService) {
        super();
        this.exposedStaticVisitService = exposedStaticVisitService;
        this.configuration = configuration;
        this.exposedTokenGeneratorService = exposedTokenGeneratorService;
    }

    public RiskLevel getStatus(Stream<OpaqueVisit> opaqueVisits) {
        long currentTimestamp = TimeUtils.roundedCurrentTimeTimestamp();
        boolean atRisk = opaqueVisits
                .filter(opaqueVisit -> this.isValidDelta(currentTimestamp - opaqueVisit.getVisitTime()))
                .anyMatch(opaqueVisit -> {
                    long score = this.exposedStaticVisitService.riskScore(opaqueVisit.getPayload(), opaqueVisit.getVisitTime());
                    return score >= this.configuration.getScoreThreshold();
                });
        if (atRisk) {
            // TODO refine the risk algo
            return RiskLevel.TACW_HIGH;
        } else {
            return RiskLevel.NONE;
        }
    }

    @Transactional
    public void reportVisitsWhenInfected(ReportRequestVo reportRequestVo) {
        long currentTimestamp = TimeUtils.roundedCurrentTimeTimestamp();
        int nbVisits = reportRequestVo.getVisits().size();
        log.info(String.format("Reporting %d visits while infected", nbVisits));
        int nbRejectedVisits = nbVisits - this.configuration.getMaxVisits();
        if (nbRejectedVisits > 0) {
            log.info(String.format("Filtered %d visits out of %d while reporting", nbRejectedVisits, nbVisits));
        }
        reportRequestVo.getVisits().stream()
        .limit(this.configuration.getMaxVisits())
        .filter(visit -> this.isValidTimestamp(visit.getTimestamp(), currentTimestamp))
        .filter(visit -> visit.getQrCode().getType().isStatic())
        .forEach(visit -> this.registerAllExposedStaticTokens(visit));
    }

    protected void registerAllExposedStaticTokens(VisitVo visit) {
        exposedStaticVisitService.registerExposedStaticVisitEntities(this.allExposedStaticVisitsFromVisit(visit));
    }

    protected List<ExposedStaticVisitEntity> allExposedStaticVisitsFromVisit(VisitVo visit) {
        return exposedTokenGeneratorService.generateAllExposedTokens(visit).collect(Collectors.toList());
    }

    protected boolean isValidTimestamp(String timestampString, long currentTimestamp) {
        try {
            long delta = currentTimestamp - TimeUtils.roundedTimestamp(Long.parseLong(timestampString));
            boolean isValid = this.isValidDelta(delta);
            if (!isValid)
                log.info(String.format("Ignoring invalid timestamp: %d, currentTimestamp: %d", timestampString, currentTimestamp));
            return isValid;
        } catch (NumberFormatException e) {
            log.error(String.format("Wrong timestamp format: %s, visit ignored. %s", timestampString, e.getMessage()));
            return false;
        }
    }

    protected boolean isValidDelta(long delta) {
        return true; // TODO choose if we filter by timestamps
        //	    return delta > 0
        //	            && delta <= TimeUnit.DAYS.toSeconds(visitTokenRetentionPeriodDays);
    }

}
