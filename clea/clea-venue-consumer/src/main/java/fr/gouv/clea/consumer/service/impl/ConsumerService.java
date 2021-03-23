package fr.gouv.clea.consumer.service.impl;

import fr.gouv.clea.consumer.data.ExposedVisit;
import fr.gouv.clea.consumer.model.DecodedVisit;
import fr.gouv.clea.consumer.service.IConsumerService;
import fr.gouv.clea.consumer.service.IPersistService;
import fr.gouv.clea.consumer.service.IVerificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Slf4j
public class ConsumerService implements IConsumerService {

    private final IVerificationService verificationService;
    private final IPersistService persistService;

    @Autowired
    public ConsumerService(
            IVerificationService verificationService,
            IPersistService persistService
    ) {
        this.verificationService = verificationService;
        this.persistService = persistService;
    }

    @Override
    @KafkaListener(topics = "${spring.kafka.template.default-topic}")
    public void consume(DecodedVisit decodedVisit) {
        Optional<ExposedVisit> exposedVisit = verificationService.decryptAndValidate(decodedVisit);
        exposedVisit.ifPresent(persistService::persist);
    }
}
