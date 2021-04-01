package fr.gouv.clea.consumer.service;

import fr.gouv.clea.consumer.model.DecodedVisit;

public interface IConsumerService {

    void consume(DecodedVisit decodedVisit);
}
