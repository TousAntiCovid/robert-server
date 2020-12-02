package fr.gouv.tacw.ws.service.impl;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.transaction.Transactional;

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
	
	public WarningServiceImpl(ExposedStaticVisitService tokenService, TokenMapper tokenMapper, ScoringProperties scoringProperties) {
		super();
		this.exposedStaticVisitService = tokenService;
		this.scoringProperties = scoringProperties;
	}

	public boolean getStatus(Stream<OpaqueVisit> opaqueVisits, long threshold) {
		return opaqueVisits
			.filter(opaqueVisit -> this.isValidTimestamp(opaqueVisit.getVisitTime()))
			.anyMatch(opaqueVisit -> {
				long score = this.exposedStaticVisitService.riskScore(opaqueVisit.getPayload(), opaqueVisit.getVisitTime());
				return score >= threshold;
			});
	}

	@Transactional
	public void reportVisitsWhenInfected(ReportRequestVo reportRequestVo) {
		log.info(String.format("Reporting %d visits while infected", reportRequestVo.getVisits().size()));
		reportRequestVo.getVisits().stream()
			.filter(visit -> this.isValidTimestamp(visit.getTimestamp()))
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
	
	protected boolean isValidTimestamp(String timestampString) {
		try {
			long timestamp = Long.parseLong(timestampString);
			return this.isValidTimestamp(timestamp);
		} catch (NumberFormatException e) {
			log.error(String.format("Wrong timestamp format: %s, visit ignored. %s", timestampString, e.getMessage()));
			return false;
		}
	}

	protected boolean isValidTimestamp(long timestamp) {
		return timestamp < TimeUtils.roundedCurrentTimeTimestamp();
	}
}
