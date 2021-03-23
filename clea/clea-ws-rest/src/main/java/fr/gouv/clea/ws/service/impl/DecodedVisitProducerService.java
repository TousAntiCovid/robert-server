package fr.gouv.clea.ws.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFutureCallback;

import fr.gouv.clea.ws.model.DecodedVisit;
import fr.gouv.clea.ws.service.IDecodedVisitProducerService;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class DecodedVisitProducerService implements IDecodedVisitProducerService {

    private final KafkaTemplate<String, DecodedVisit> kafkaTemplate;

    @Autowired
    public DecodedVisitProducerService(
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
                                // TODO: Do we want a mechanism to do not loose the message (e.g. spring retry)?
                                log.error("Cannot send Message!", ex);
                            }

                            @Override
                            public void onSuccess(SendResult<String, DecodedVisit> result) {
                                // Nothing to do
                            }
                        }
                )
        );
    }
}
