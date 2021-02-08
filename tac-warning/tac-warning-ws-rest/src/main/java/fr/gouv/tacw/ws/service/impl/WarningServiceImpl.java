package fr.gouv.tacw.ws.service.impl;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.transaction.Transactional;

import org.springframework.stereotype.Service;

import fr.gouv.tacw.database.model.ExposedStaticVisitEntity;
import fr.gouv.tacw.database.model.ScoreResult;
import fr.gouv.tacw.database.service.ExposedStaticVisitService;
import fr.gouv.tacw.database.utils.TimeUtils;
import fr.gouv.tacw.model.OpaqueVisit;
import fr.gouv.tacw.model.ScoreResults;
import fr.gouv.tacw.ws.configuration.TacWarningWsRestConfiguration;
import fr.gouv.tacw.ws.service.ExposedTokenGeneratorService;
import fr.gouv.tacw.ws.service.WarningService;
import fr.gouv.tacw.ws.vo.VisitVo;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class WarningServiceImpl implements WarningService {
    public static final String WRONG_TIMESTAMP_FORMAT_LOG_MESSAGE = "Wrong timestamp format: %s, visit ignored. %s";
	public static final String IGNORING_INVALID_TIMESTAMP_LOG_MESSAGE = "Ignoring invalid timestamp: %d, currentTimestamp: %d";
    public static final String REPORT_MAX_VISITS_FILTER_LOG_MESSAGE = "Filtered %d visits out of %d while reporting";
    private ExposedStaticVisitService exposedStaticVisitService;
	private TacWarningWsRestConfiguration configuration;
    private ExposedTokenGeneratorService exposedTokenGeneratorService;

    public WarningServiceImpl(ExposedStaticVisitService exposedStaticVisitService, TacWarningWsRestConfiguration configuration, ExposedTokenGeneratorService exposedTokenGeneratorService) {
        super();
        this.exposedStaticVisitService = exposedStaticVisitService;
        this.configuration = configuration;
        this.exposedTokenGeneratorService = exposedTokenGeneratorService;
    }

    public ScoreResult getStatus(Stream<OpaqueVisit> opaqueVisits) {
        long currentTimestamp = TimeUtils.roundedCurrentTimeTimestamp();
        ScoreResults scores = opaqueVisits
                .filter(opaqueVisit -> this.isValidDelta(currentTimestamp - opaqueVisit.getVisitTime()))
                .map(opaqueVisit -> new ScoreResults(this.exposedStaticVisitService.riskScore(opaqueVisit.getPayload(), opaqueVisit.getVisitTime())))
                .reduce(new ScoreResults(), (scores1, scores2) -> scores1.merge(scores2));
        return scores.getScoreWithMaxRiskLevelReached(this.configuration.getScoreThreshold());
    }

	@Transactional
	public void reportVisitsWhenInfected(List<VisitVo> visits) {
		long currentTimestamp = TimeUtils.roundedCurrentTimeTimestamp();
		int nbVisits = visits.size();
		int nbRejectedVisits = nbVisits - this.configuration.getMaxVisits();
		if (nbRejectedVisits > 0) {
			log.info(String.format(REPORT_MAX_VISITS_FILTER_LOG_MESSAGE, nbRejectedVisits, nbVisits));
		}
		visits.stream()
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
	            log.info(String.format(IGNORING_INVALID_TIMESTAMP_LOG_MESSAGE, timestampString, currentTimestamp));
	        return isValid;
		} catch (NumberFormatException e) {
			log.error(String.format(WRONG_TIMESTAMP_FORMAT_LOG_MESSAGE, timestampString, e.getMessage()));
			return false;
		}
	}

    protected boolean isValidDelta(long delta) {
        return true; // TODO choose if we filter by timestamps
        //	    return delta > 0
        //	            && delta <= TimeUnit.DAYS.toSeconds(visitTokenRetentionPeriodDays);
    }

}
