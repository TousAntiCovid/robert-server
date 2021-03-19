package fr.gouv.tacw.services;

import fr.gouv.tacw.data.DecodedLocationSpecificPart;

import java.util.List;

public interface IProducerService {

    void produce(List<DecodedLocationSpecificPart> decodedLocationSpecificParts);
}
