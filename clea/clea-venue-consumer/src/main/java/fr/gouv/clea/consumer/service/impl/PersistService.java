package fr.gouv.clea.consumer.service.impl;

import fr.gouv.clea.consumer.data.ExposedVisit;
import fr.gouv.clea.consumer.data.IExposedVisitRepository;
import fr.gouv.clea.consumer.service.IPersistService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.transaction.Transactional;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
@Slf4j
public class PersistService implements IPersistService {

    private final IExposedVisitRepository repository;

    private final int retentionDuration;

    @Autowired
    public PersistService(
            IExposedVisitRepository repository,
            @Value("${clea.conf.retentionDuration}") int retentionDuration
    ) {
        this.repository = repository;
        this.retentionDuration = retentionDuration;
    }

    @Override
    public ExposedVisit persist(ExposedVisit exposedVisit) {
        return repository.save(exposedVisit);
    }

    @Override
    @Transactional
    @Scheduled(cron = "${clea.conf.scheduling.purge.cron}")
    public void deleteOutdatedExposedVisits() {
        this.repository.deleteAllByQrCodeScanTimeBefore(Instant.now().minus(retentionDuration, ChronoUnit.DAYS));
    }
}
