package fr.gouv.clea.consumer.service.impl;

import fr.gouv.clea.consumer.model.DecodedVisit;
import fr.gouv.clea.consumer.model.Visit;
import fr.gouv.clea.consumer.service.IConsumerService;
import fr.gouv.clea.consumer.service.IDecodedVisitService;
import fr.gouv.clea.consumer.service.IExposedVisitEntityService;
import fr.gouv.clea.consumer.utils.MessageFormatter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Slf4j
public class ConsumerService implements IConsumerService {

    private final IDecodedVisitService decodedVisitService;
    private final IExposedVisitEntityService exposedVisitEntityService;

    @Autowired
    public ConsumerService(
            IDecodedVisitService decodedVisitService,
            IExposedVisitEntityService exposedVisitEntityService
    ) {
        this.decodedVisitService = decodedVisitService;
        this.exposedVisitEntityService = exposedVisitEntityService;
    }

    @Override
    @KafkaListener(topics = "${spring.kafka.template.default-topic}")
    public void consume(DecodedVisit decodedVisit) {
        log.info("[locationTemporaryPublicId: {}, qrCodeScanTime: {}] retrieved from queue", MessageFormatter.truncateUUID(decodedVisit.getStringLocationTemporaryPublicId()), decodedVisit.getQrCodeScanTime());
        Optional<Visit> exposedVisit = decodedVisitService.decryptAndValidate(decodedVisit);
        // TODO use a service to aggregate Visit and produce ExposedVisitEntities
        //   + --> compute visits slots that a person may have been in contact with the covid+ report
        //   + --> check in DB if there is already an entry with the same LTid, periodStart, timeSlot
        //         - if so, update the record
        //         - else add a new entry
        if (exposedVisit.isPresent())
            exposedVisitEntityService.persist(null);

    }
}
