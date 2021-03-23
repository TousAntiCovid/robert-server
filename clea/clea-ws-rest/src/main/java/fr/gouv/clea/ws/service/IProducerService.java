package fr.gouv.clea.ws.service;

import fr.gouv.clea.ws.model.DecodedVisit;

import java.util.List;

public interface IProducerService {

    void produce(List<DecodedVisit> decodedVisits);
}
