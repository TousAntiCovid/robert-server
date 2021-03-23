package fr.gouv.clea.consumer.service.impl;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import fr.gouv.clea.consumer.model.DecodedVisit;
import fr.gouv.clea.consumer.model.ExposedVisitEntity;
import fr.gouv.clea.consumer.service.IConsumerService;
import fr.gouv.clea.consumer.service.IExposedVisitEntityService;
import fr.gouv.clea.consumer.service.IDecodedVisitService;

@Component
public class ConsumerService implements IConsumerService {

    private final IDecodedVisitService decodedVisitService;
    private final IExposedVisitEntityService exposedVisitEntityService;

    @Autowired
    public ConsumerService(
            IDecodedVisitService decodedVisitService,
            IExposedVisitEntityService exposedVisitEntityService) {
        this.decodedVisitService = decodedVisitService;
        this.exposedVisitEntityService = exposedVisitEntityService;
    }

    @Override
    @KafkaListener(topics = "${spring.kafka.template.default-topic}")
    public void consume(DecodedVisit decodedVisit) {
        Optional<ExposedVisitEntity> exposedVisitEntity = decodedVisitService.decryptAndValidate(decodedVisit);
        exposedVisitEntity.ifPresent(exposedVisitEntityService::persist);
    }
}
