package fr.gouv.tacw.services.impl;

import fr.gouv.tacw.dto.DecodedLocationSpecificPart;
import fr.gouv.tacw.services.IConsumerService;
import fr.gouv.tacw.services.IPersistService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ConsumerService implements IConsumerService {

    private final IPersistService persistService;

    @Autowired
    public ConsumerService(IPersistService persistService) {
        this.persistService = persistService;
    }

    @Override
    @KafkaListener(topics = "${spring.kafka.template.default-topic}")
    public void consumeVenue(DecodedLocationSpecificPart decodedLocationSpecificPart) {
        persistService.checkAndPersist(decodedLocationSpecificPart.toDetectedVenue());
    }
}
