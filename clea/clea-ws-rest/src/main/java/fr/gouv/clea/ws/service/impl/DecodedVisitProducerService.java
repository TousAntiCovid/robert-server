package fr.gouv.clea.ws.service.impl;

import fr.gouv.clea.ws.model.DecodedVisit;
import fr.gouv.clea.ws.service.IDecodedVisitProducerService;
import fr.gouv.clea.ws.utils.MessageFormatter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFutureCallback;

import java.util.List;

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
                                log.error("error sending [locationTemporaryPublicId: {}, qrCodeScanTime: {}] to queue. message: {}", MessageFormatter.truncateUUID(it.getStringLocationTemporaryPublicId()), it.getQrCodeScanTime(), ex.getLocalizedMessage());
                            }

                            @Override
                            public void onSuccess(SendResult<String, DecodedVisit> result) {
                                log.info("[locationTemporaryPublicId: {}, qrCodeScanTime: {}] sent to queue", MessageFormatter.truncateUUID(it.getStringLocationTemporaryPublicId()), it.getQrCodeScanTime());
                            }
                        }
                )
        );
    }
}
