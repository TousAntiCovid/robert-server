package fr.gouv.clea.consumer.service.impl;

import fr.gouv.clea.consumer.repository.IExposedVisitRepository;
import fr.gouv.clea.consumer.service.IExposedVisitEntityService;
import fr.inria.clea.lsp.utils.TimeUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.transaction.Transactional;

@Component
@Slf4j
public class ExposedVisitEntityService implements IExposedVisitEntityService {

    private final IExposedVisitRepository repository;

    private final int retentionDurationInDays;

    private final int durationUnitInSeconds;

    @Autowired
    public ExposedVisitEntityService(
            IExposedVisitRepository repository,
            @Value("${clea.conf.retentionDurationInDays}") int retentionDurationInDays,
            @Value("${clea.conf.durationUnitInSeconds}") int durationUnitInSeconds
    ) {
        this.repository = repository;
        this.retentionDurationInDays = retentionDurationInDays;
        this.durationUnitInSeconds = durationUnitInSeconds;
    }

    @Override
    @Transactional
    @Scheduled(cron = "${clea.conf.scheduling.purge.cron}")
    public void deleteOutdatedExposedVisits() {
        try {
            long start = System.currentTimeMillis();
            int count = this.repository.purge(TimeUtils.currentNtpTime(), durationUnitInSeconds, TimeUtils.NB_SECONDS_PER_HOUR * 24, retentionDurationInDays);
            long end = System.currentTimeMillis();
            log.info("successfully purged {} entries from DB in {} seconds", count, (end - start) / 1000);
        } catch (Exception e) {
            log.error("error during purge");
            throw e;
        }
    }
}
