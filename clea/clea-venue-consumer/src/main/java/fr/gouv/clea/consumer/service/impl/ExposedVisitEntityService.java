package fr.gouv.clea.consumer.service.impl;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import fr.gouv.clea.consumer.model.ExposedVisitEntity;
import fr.gouv.clea.consumer.repository.IExposedVisitRepository;
import fr.gouv.clea.consumer.service.IExposedVisitEntityService;

@Component
public class ExposedVisitEntityService implements IExposedVisitEntityService {

    private final IExposedVisitRepository repository;

    private final int retentionDurationInDays;

    @Autowired
    public ExposedVisitEntityService(
            IExposedVisitRepository repository,
            @Value("${clea.conf.retentionDurationInDays}") int retentionDurationInDays) {
        this.repository = repository;
        this.retentionDurationInDays = retentionDurationInDays;
    }

    @Override
    public ExposedVisitEntity persist(ExposedVisitEntity exposedVisitEntity) {
        return repository.save(exposedVisitEntity);
    }

    @Override
    @Transactional
    @Scheduled(cron = "${clea.conf.scheduling.purge.cron}")
    public void deleteOutdatedExposedVisits() {
        this.repository.deleteAllByQrCodeScanTimeBefore(Instant.now().minus(retentionDurationInDays, ChronoUnit.DAYS));
    }
}
