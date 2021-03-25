package fr.gouv.clea.consumer.service.impl;

import fr.gouv.clea.consumer.model.DecodedVisit;
import fr.gouv.clea.consumer.model.ExposedVisitEntity;
import fr.gouv.clea.consumer.model.Visit;
import fr.gouv.clea.consumer.service.IAggregateService;
import fr.gouv.clea.consumer.service.IConsumerService;
import fr.gouv.clea.consumer.service.IDecodedVisitService;
import fr.gouv.clea.consumer.service.IExposedVisitEntityService;
import fr.gouv.clea.consumer.utils.MessageFormatter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@Slf4j
public class ConsumerService implements IConsumerService {

    private final IDecodedVisitService decodedVisitService;
    private final IAggregateService aggregateService;
    private final IExposedVisitEntityService exposedVisitEntityService;

    @Autowired
    public ConsumerService(
            IDecodedVisitService decodedVisitService,
            IAggregateService aggregateService,
            IExposedVisitEntityService exposedVisitEntityService
    ) {
        this.decodedVisitService = decodedVisitService;
        this.aggregateService = aggregateService;
        this.exposedVisitEntityService = exposedVisitEntityService;
    }

    @Override
    @KafkaListener(topics = "${spring.kafka.template.default-topic}")
    public void consume(DecodedVisit decodedVisit) {
        log.info("[locationTemporaryPublicId: {}, qrCodeScanTime: {}] retrieved from queue", MessageFormatter.truncateUUID(decodedVisit.getStringLocationTemporaryPublicId()), decodedVisit.getQrCodeScanTime());
        Optional<Visit> optional = decodedVisitService.decryptAndValidate(decodedVisit);
        if (optional.isPresent()) {
            List<ExposedVisitEntity> exposedVisitEntities = aggregateService.aggregate(optional.get());
            exposedVisitEntityService.persistMany(exposedVisitEntities);
        }
    }
}
