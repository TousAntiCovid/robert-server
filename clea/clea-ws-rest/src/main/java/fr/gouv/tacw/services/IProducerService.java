package fr.gouv.tacw.services;

import fr.gouv.tacw.dtos.DecodedLocationSpecificPart;

import java.util.List;

public interface IProducerService {

    void produce(List<DecodedLocationSpecificPart> decodedLocationSpecificParts);
}
