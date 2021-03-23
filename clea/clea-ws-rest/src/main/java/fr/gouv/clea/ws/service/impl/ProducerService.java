package fr.gouv.clea.ws.service.impl;

import fr.gouv.clea.ws.model.DecodedVisit;
import fr.gouv.clea.ws.service.IProducerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFutureCallback;

import java.util.List;

@Service
public class ProducerService implements IProducerService {

    private final KafkaTemplate<String, DecodedVisit> kafkaTemplate;

    @Autowired
    public ProducerService(
            KafkaTemplate<String, DecodedVisit> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void produce(List<DecodedVisit> serializableDecodedVisits) {
        serializableDecodedVisits.forEach(
                it -> kafkaTemplate.sendDefault(it).addCallback(
                        new ListenableFutureCallback<>() {
                            @Override
                            public void onFailure(Throwable ex) {

                            }

                            @Override
                            public void onSuccess(SendResult<String, DecodedVisit> result) {

                            }
                        }
                )
        );
    }
}
