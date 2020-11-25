package fr.gouv.tacw.ws.service;

import java.util.List;
import java.util.stream.Collectors;

import javax.transaction.Transactional;

import org.springframework.stereotype.Service;

import fr.gouv.tacw.database.model.ExposedStaticVisitTokenEntity;
import fr.gouv.tacw.database.service.TokenService;
import fr.gouv.tacw.model.ExposedTokenGenerator;
import fr.gouv.tacw.ws.vo.ExposureStatusRequestVo;
import fr.gouv.tacw.ws.vo.ReportRequestVo;
import fr.gouv.tacw.ws.vo.VisitVo;
import fr.gouv.tacw.ws.vo.mapper.TokenMapper;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class WarningServiceImpl implements WarningService {
	private TokenService tokenService;
	private TokenMapper tokenMapper;
	private ExposureStatusService exposureStatusService;
	
	public WarningServiceImpl(TokenService tokenService, TokenMapper tokenMapper,
			ExposureStatusService exposureStatusService) {
		super();
		this.tokenService = tokenService;
		this.tokenMapper = tokenMapper;
		this.exposureStatusService = exposureStatusService;
	}

	public boolean getStatus(ExposureStatusRequestVo statusRequestVo) {
		log.info(String.format("Exposure status request for %d visits", statusRequestVo.getVisitTokens().size()));
		return statusRequestVo.getVisitTokens().stream().
				map(tokenVo -> tokenMapper.getToken(tokenVo)).
				anyMatch(token -> exposureStatusService.isExposed(token));
	}

	@Transactional
	public void reportVisitsWhenInfected(ReportRequestVo reportRequestVo) {
		log.info(String.format("Reporting %d visits while infected", reportRequestVo.getVisits().size()));
		reportRequestVo.getVisits().stream()
				.filter(reportRequest -> reportRequest.getQrCode().getType().isStatic())
				.forEach(visit -> this.registerAllExposedStaticTokens(visit));
	}
	
	protected void registerAllExposedStaticTokens(VisitVo visit) {
		tokenService.registerExposedStaticTokens( this.allTokensFromVisit(visit) );
	}

	protected List<ExposedStaticVisitTokenEntity> allTokensFromVisit(VisitVo visit) {
		long timestamp = Long.parseLong(visit.getTimestamp());
		return new ExposedTokenGenerator(visit).generateAllExposedTokens().stream()
				.map(exposedToken -> new ExposedStaticVisitTokenEntity(timestamp, exposedToken.getPayload()))
				.collect(Collectors.toList());
	}
}
