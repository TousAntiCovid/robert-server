package fr.gouv.clea.consumer.service.impl;

import fr.gouv.clea.consumer.model.ExposedVisitEntity;
import fr.gouv.clea.consumer.model.Visit;
import fr.gouv.clea.consumer.repository.IExposedVisitRepository;
import fr.gouv.clea.consumer.service.IAggregateService;
import fr.inria.clea.lsp.utils.TimeUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class AggregateService implements IAggregateService {

    private final static long EXPOSURE_TIME_UNIT = TimeUtils.NB_SECONDS_PER_HOUR;
    private final static int EXPOSURE_TIME = 3;
    private final IExposedVisitRepository repository;

    @Autowired
    public AggregateService(IExposedVisitRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<ExposedVisitEntity> aggregate(Visit visit) {
        // TODO use a service to aggregate Visit and produce ExposedVisitEntities
        //   + --> compute visits slots that a person may have been in contact with the covid+ report
        //   + --> check in DB if there is already an entry with the same LTid, periodStart, timeSlot
        //         - if so, update the record
        //         - else add a new entry
        long scanTimeSlot = TimeUtils.ntpTimestampFromInstant(visit.getQrCodeScanTime()) - ((long) visit.getCompressedPeriodStartTime() * TimeUtils.NB_SECONDS_PER_HOUR) / EXPOSURE_TIME_UNIT;
        return null;
    }

    private int getExposureTime(String venueType, String venueCategory1, String venueCategory2, String staff) {
        return 3 * TimeUtils.NB_SECONDS_PER_HOUR;
    }
}
