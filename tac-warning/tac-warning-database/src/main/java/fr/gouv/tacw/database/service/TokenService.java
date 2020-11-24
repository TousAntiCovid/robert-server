package fr.gouv.tacw.database.service;


import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import fr.gouv.stopc.robert.server.common.utils.TimeUtils;

import fr.gouv.tacw.database.model.ExposedStaticVisitTokenEntity;
import fr.gouv.tacw.database.repository.StaticTokenRepository;

@Service
public class TokenService {
	@Autowired
	StaticTokenRepository staticTokenRepository;
	
	@Value("${tacw.database.visit_token_retention_period_days}")
	private long visitTokenRetentionPeriodDays;
	
	public void registerExposedStaticToken(long timestamp, String token) {
		staticTokenRepository.save(new ExposedStaticVisitTokenEntity(timestamp, token));
	}

	public void registerExposedStaticTokens(List<ExposedStaticVisitTokenEntity> exposedStaticVisitTokenEntities ) {
		staticTokenRepository.saveAll(exposedStaticVisitTokenEntities);
	}
	
	public boolean exposedStaticTokensIncludes(String token) {
		return staticTokenRepository.findByToken(token).isPresent();
	}
	
	@Scheduled(cron="${tacw.database.visit_token_deletion_job_cron_expression}")
	public void deleteExpiredTokens() {
		final long SECONDS_PER_DAY = 86400;
		final long currentNtpTime = TimeUtils.convertUnixMillistoNtpSeconds(System.currentTimeMillis());
		final long retentionStart = currentNtpTime - (visitTokenRetentionPeriodDays * SECONDS_PER_DAY);
		staticTokenRepository.deleteByTimestampLessThan(retentionStart);
		
	}
}
