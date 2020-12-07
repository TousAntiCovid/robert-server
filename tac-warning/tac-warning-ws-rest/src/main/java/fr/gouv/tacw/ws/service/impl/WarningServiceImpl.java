package fr.gouv.tacw.ws.service.impl;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import fr.gouv.tacw.database.model.ExposedStaticVisitEntity;
import fr.gouv.tacw.database.service.ExposedStaticVisitService;
import fr.gouv.tacw.database.utils.TimeUtils;
import fr.gouv.tacw.model.ExposedTokenGenerator;
import fr.gouv.tacw.model.OpaqueVisit;
import fr.gouv.tacw.ws.properties.ScoringProperties;
import fr.gouv.tacw.ws.service.WarningService;
import fr.gouv.tacw.ws.vo.ReportRequestVo;
import fr.gouv.tacw.ws.vo.VisitVo;
import fr.gouv.tacw.ws.vo.mapper.TokenMapper;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class WarningServiceImpl implements WarningService {
	private ExposedStaticVisitService exposedStaticVisitService;
	private ScoringProperties scoringProperties;
	@Value("${tacw.database.visit_token_retention_period_days}")
	private long visitTokenRetentionPeriodDays;
	@Value("${tacw.rest.max_visits}")
	private int maxVisits;
	
	public WarningServiceImpl(ExposedStaticVisitService tokenService, TokenMapper tokenMapper, ScoringProperties scoringProperties) {
		super();
		this.exposedStaticVisitService = tokenService;
		this.scoringProperties = scoringProperties;
	}

	public boolean getStatus(Stream<OpaqueVisit> opaqueVisits, long threshold) {
		long currentTimestamp = TimeUtils.roundedCurrentTimeTimestamp();
		return opaqueVisits
			.filter(opaqueVisit -> this.isValidDelta(currentTimestamp - opaqueVisit.getVisitTime()))
			.anyMatch(opaqueVisit -> {
				long score = this.exposedStaticVisitService.riskScore(opaqueVisit.getPayload(), opaqueVisit.getVisitTime());
				return score >= threshold;
			});
	}

	@Transactional
	public void reportVisitsWhenInfected(ReportRequestVo reportRequestVo) {
		long currentTimestamp = TimeUtils.roundedCurrentTimeTimestamp();
		int nbVisits = reportRequestVo.getVisits().size();
		log.info(String.format("Reporting %d visits while infected", nbVisits));
		int nbRejectedVisits = nbVisits - maxVisits;
		if (nbRejectedVisits > 0)
			log.info(String.format("Filtered %d visits out of %d while reporting", nbRejectedVisits, nbVisits));
		reportRequestVo.getVisits().stream()
			.limit(maxVisits)
			.filter(visit -> this.isValidTimestamp(visit.getTimestamp(), currentTimestamp))
			.filter(visit -> visit.getQrCode().getType().isStatic())
			.forEach(visit -> this.registerAllExposedStaticTokens(visit));
	}

	protected void registerAllExposedStaticTokens(VisitVo visit) {
		exposedStaticVisitService.registerOrIncrementExposedStaticVisits(this.allExposedStaticVisitsFromVisit(visit));
	}

	protected List<ExposedStaticVisitEntity> allExposedStaticVisitsFromVisit(VisitVo visit) {
		return new ExposedTokenGenerator(visit, scoringProperties)
						.generateAllExposedTokens()
						.collect(Collectors.toList());
	}
	
	protected boolean isValidTimestamp(String timestampString, long currentTimestamp) {
		try {
			long delta = currentTimestamp - Long.parseLong(timestampString);
			return this.isValidDelta(delta);
		} catch (NumberFormatException e) {
			log.error(String.format("Wrong timestamp format: %s, visit ignored. %s", timestampString, e.getMessage()));
			return false;
		}
	}

	protected boolean isValidDelta(long delta) {
		return delta > 0
				&& delta <= TimeUnit.DAYS.toSeconds(visitTokenRetentionPeriodDays);
	}
}
