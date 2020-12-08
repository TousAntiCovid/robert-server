package fr.gouv.tacw.database.service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import fr.gouv.tacw.database.model.ExposedStaticVisitEntity;
import fr.gouv.tacw.database.repository.ExposedStaticVisitRepository;
import fr.gouv.tacw.database.utils.TimeUtils;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@PropertySource("classpath:application.properties")
@Service
public class ExposedStaticVisitServiceImpl implements ExposedStaticVisitService {
	private ExposedStaticVisitRepository exposedStaticVisitRepository;

	@Value("${tacw.database.visit_token_retention_period_days}")
	private long visitTokenRetentionPeriodDays;

	public ExposedStaticVisitServiceImpl(ExposedStaticVisitRepository exposedStaticVisitRepository) {
		super();
		this.exposedStaticVisitRepository = exposedStaticVisitRepository;
	}

	@Override
	public long riskScore(String token, long visitTime) {
		return exposedStaticVisitRepository.riskScore(token, visitTime);
	}

	@Scheduled(cron = "${tacw.database.visit_token_deletion_job_cron_expression}")
	public long deleteExpiredTokens() {
		final long currentNtpTime = TimeUtils.convertUnixMillistoNtpSeconds(System.currentTimeMillis());
		final long retentionStart = currentNtpTime - TimeUnit.DAYS.toSeconds(visitTokenRetentionPeriodDays);
		log.debug(String.format("Purge expired tokens before %d", retentionStart));
		final long nbDeletedTokens = exposedStaticVisitRepository.deleteByVisitEndTimeLessThan(retentionStart);
		log.info(String.format("Deleted %d static tokens from exposed tokens", nbDeletedTokens));
		return nbDeletedTokens;
	}

	@Override
	public void registerOrIncrementExposedStaticVisits(List<ExposedStaticVisitEntity> exposedStaticVisitEntities) {
		List<ExposedStaticVisitEntity> exposedStaticVisitEntitiesToSave;

		exposedStaticVisitEntitiesToSave = exposedStaticVisitEntities.stream()
			.filter(entity -> !this.tryToUpdateExistingStaticVisitEntity(entity))
			.collect(Collectors.toList());
		this.registerExposedStaticVisitEntities(exposedStaticVisitEntitiesToSave);
	}

	protected boolean tryToUpdateExistingStaticVisitEntity(ExposedStaticVisitEntity entity) {
		Optional<ExposedStaticVisitEntity> optSyncEntity = exposedStaticVisitRepository
				.findByTokenAndStartEnd(entity.getToken(), entity.getVisitStartTime(), entity.getVisitEndTime());
		if ( !optSyncEntity.isPresent() ) {
			return false;
		}
		ExposedStaticVisitEntity exposedStaticVisitEntity = optSyncEntity.get();
		long newExposureCount = exposedStaticVisitEntity.getExposureCount() + entity.getExposureCount();
		log.debug(String.format("Updating existing exposed visit %s (%d, %d) exposure count from %d to %d",
				exposedStaticVisitEntity.getToken(), exposedStaticVisitEntity.getVisitStartTime(), exposedStaticVisitEntity.getVisitEndTime(),
				exposedStaticVisitEntity.getExposureCount(), newExposureCount));
		exposedStaticVisitEntity.setExposureCount(newExposureCount);
		exposedStaticVisitRepository.saveAndFlush(exposedStaticVisitEntity);
		return true;
	}

	private void registerExposedStaticVisitEntities(List<ExposedStaticVisitEntity> exposedStaticVisitEntityToSave) {
		log.debug(String.format("Registering %d new exposed visit entities", exposedStaticVisitEntityToSave.size()));
		log.debug(exposedStaticVisitEntityToSave.toString());
		exposedStaticVisitRepository.saveAll(exposedStaticVisitEntityToSave);
	}

}
