package fr.gouv.tacw.database.service;


import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import fr.gouv.tacw.database.utils.TimeUtils;
import lombok.extern.slf4j.Slf4j;
import fr.gouv.tacw.database.model.ExposedStaticVisitTokenEntity;
import fr.gouv.tacw.database.repository.StaticTokenRepository;

@Slf4j
@Service
public class TokenService {
	@Autowired
	StaticTokenRepository staticTokenRepository;
	
	@Value("${tacw.database.visit_token_retention_period_days}")
	private long visitTokenRetentionPeriodDays;
	
	private static final long SECONDS_PER_DAY = 86400;

	public void registerExposedStaticToken(long timestamp, String token) {
		staticTokenRepository.save(new ExposedStaticVisitTokenEntity(timestamp, token));
	}

	public void registerExposedStaticTokens(List<ExposedStaticVisitTokenEntity> exposedStaticVisitTokenEntities ) {
		log.info(String.format("Registering %d static tokens in exposed tokens", exposedStaticVisitTokenEntities.size()));
		staticTokenRepository.saveAll(exposedStaticVisitTokenEntities);
	}
	
	public boolean exposedStaticTokensIncludes(String token) {
		return staticTokenRepository.findByToken(token).isPresent();
	}
	
	@Scheduled(cron="${tacw.database.visit_token_deletion_job_cron_expression}")
	public long deleteExpiredTokens() {
		final long currentNtpTime = TimeUtils.convertUnixMillistoNtpSeconds(System.currentTimeMillis());
		final long retentionStart = currentNtpTime - (visitTokenRetentionPeriodDays * SECONDS_PER_DAY);
		final long nbDeletedTokens = staticTokenRepository.deleteByTimestampLessThan(retentionStart);
		log.info(String.format("Deleted %d static tokens from exposed tokens", nbDeletedTokens));
		return nbDeletedTokens;
	}
}
