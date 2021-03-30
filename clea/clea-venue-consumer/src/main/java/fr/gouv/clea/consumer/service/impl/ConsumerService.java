package fr.gouv.clea.consumer.service.impl;

import fr.gouv.clea.consumer.model.DecodedVisit;
import fr.gouv.clea.consumer.model.Visit;
import fr.gouv.clea.consumer.service.IConsumerService;
import fr.gouv.clea.consumer.service.IDecodedVisitService;
import fr.gouv.clea.consumer.service.IVisitExpositionAggregatorService;
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
    private final IVisitExpositionAggregatorService visitExpositionAggregatorService;

    @Autowired
    public ConsumerService(
            IDecodedVisitService decodedVisitService,
            IVisitExpositionAggregatorService visitExpositionAggregatorService) {
        this.decodedVisitService = decodedVisitService;
        this.visitExpositionAggregatorService = visitExpositionAggregatorService;
    }

    @Override
    @KafkaListener(topics = "${spring.kafka.template.default-topic}")
    public void consume(DecodedVisit decodedVisit) {
        log.info("[locationTemporaryPublicId: {}, qrCodeScanTime: {}] retrieved from queue", MessageFormatter.truncateUUID(decodedVisit.getStringLocationTemporaryPublicId()), decodedVisit.getQrCodeScanTime());
        Optional<Visit> optionalVisit = decodedVisitService.decryptAndValidate(decodedVisit);
        optionalVisit.ifPresent(visitExpositionAggregatorService::updateExposureCount);
    }
}