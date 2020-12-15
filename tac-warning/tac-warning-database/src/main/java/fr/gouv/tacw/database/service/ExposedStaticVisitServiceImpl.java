package fr.gouv.tacw.database.service;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import fr.gouv.tacw.database.TacWarningDatabaseConfiguration;
import fr.gouv.tacw.database.model.ExposedStaticVisitEntity;
import fr.gouv.tacw.database.repository.ExposedStaticVisitRepository;
import fr.gouv.tacw.database.utils.TimeUtils;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ExposedStaticVisitServiceImpl implements ExposedStaticVisitService {
    private ExposedStaticVisitRepository exposedStaticVisitRepository;

    private final TacWarningDatabaseConfiguration configuration;

    public ExposedStaticVisitServiceImpl(TacWarningDatabaseConfiguration configuration,
            ExposedStaticVisitRepository exposedStaticVisitRepository) {
        super();
        this.exposedStaticVisitRepository = exposedStaticVisitRepository;
        this.configuration = configuration;
    }

    @Override
    public long riskScore(String token, long visitTime) {
        return exposedStaticVisitRepository.riskScore(token, visitTime);
    }

    @Scheduled(cron = "${tacw.database.visit_token_deletion_job_cron_expression}")
    public long deleteExpiredTokens() {
        final long currentNtpTime = TimeUtils.convertUnixMillistoNtpSeconds(System.currentTimeMillis());
        final long retentionStart = currentNtpTime - TimeUnit.DAYS.toSeconds(configuration.getVisitTokenRetentionPeriodDays());
        log.debug(String.format("Purge expired tokens before %d", retentionStart));
        final long nbDeletedTokens = exposedStaticVisitRepository.deleteByVisitEndTimeLessThan(retentionStart);
        log.info(String.format("Deleted %d static tokens from exposed tokens", nbDeletedTokens));
        return nbDeletedTokens;
    }

    @Override
    public void registerExposedStaticVisitEntities(List<ExposedStaticVisitEntity> exposedStaticVisitEntityToSave) {
        log.debug(String.format("Registering %d new exposed visit entities", exposedStaticVisitEntityToSave.size()));
        exposedStaticVisitRepository.saveAll(exposedStaticVisitEntityToSave);
    }

}
