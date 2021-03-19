package fr.gouv.tacw.services.impl;

import fr.gouv.tacw.data.DecodedLocationSpecificPart;
import fr.gouv.tacw.services.IProducerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFutureCallback;

import java.util.List;

@Service
@Slf4j
public class ProducerService implements IProducerService {

    private final KafkaTemplate<String, DecodedLocationSpecificPart> kafkaTemplate;

    private final String topicName;

    @Autowired
    public ProducerService(
            KafkaTemplate<String, DecodedLocationSpecificPart> kafkaTemplate,
            @Value("${kafka.producer.topic.name}") String topicName
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.topicName = topicName;
    }

    @Override
    public void produce(List<DecodedLocationSpecificPart> decodedLocationSpecificParts) {
        decodedLocationSpecificParts.forEach(
                it -> kafkaTemplate.send(topicName, it)
                        .addCallback(
                                new ListenableFutureCallback<>() {
                                    @Override
                                    public void onFailure(Throwable ex) {
                                        log.error(ex.getLocalizedMessage());
                                    }

                                    @Override
                                    public void onSuccess(SendResult<String, DecodedLocationSpecificPart> result) {
                                        log.info(result.getProducerRecord().value().getQrCode());
                                    }
                                }
                        )
        );
    }
}
