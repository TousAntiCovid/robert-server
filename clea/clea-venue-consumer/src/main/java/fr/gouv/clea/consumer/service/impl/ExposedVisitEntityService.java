package fr.gouv.clea.consumer.service.impl;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import fr.gouv.clea.consumer.configuration.VenueConsumerConfiguration;
import fr.gouv.clea.consumer.repository.IExposedVisitRepository;
import fr.gouv.clea.consumer.service.IExposedVisitEntityService;
import fr.inria.clea.lsp.utils.TimeUtils;
import lombok.extern.slf4j.Slf4j;

@Component
@RefreshScope
@Slf4j
public class ExposedVisitEntityService implements IExposedVisitEntityService {

    private final IExposedVisitRepository repository;

    private final VenueConsumerConfiguration config;

    @Autowired
    public ExposedVisitEntityService(
            IExposedVisitRepository repository,
            VenueConsumerConfiguration config) {
        this.repository = repository;
        this.config = config;
    }

    @Override
    @Transactional
    @Scheduled(cron = "${clea.conf.scheduling.purge.cron}")
    public void deleteOutdatedExposedVisits() {
        try {
            long start = System.currentTimeMillis();
            int count = this.repository.purge(TimeUtils.currentNtpTime(), (int) config.getDurationUnitInSeconds(), TimeUtils.NB_SECONDS_PER_HOUR * 24, config.getRetentionDurationInDays());
            long end = System.currentTimeMillis();
            log.info("successfully purged {} entries from DB in {} seconds", count, (end - start) / 1000);
        } catch (Exception e) {
            log.error("error during purge");
            throw e;
        }
    }
}
