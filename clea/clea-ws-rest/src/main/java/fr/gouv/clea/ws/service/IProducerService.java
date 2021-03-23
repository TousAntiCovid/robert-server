package fr.gouv.clea.ws.service;

import fr.gouv.clea.ws.model.SerializableDecodedVisit;

import java.util.List;

public interface IProducerService {

    void produce(List<SerializableDecodedVisit> decodedVisits);
}
