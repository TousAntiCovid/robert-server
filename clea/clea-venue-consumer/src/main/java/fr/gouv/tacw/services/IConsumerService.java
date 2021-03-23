package fr.gouv.tacw.services;

import fr.gouv.tacw.dto.DecodedLocationSpecificPart;

public interface IConsumerService {

    void consumeVenue(DecodedLocationSpecificPart decodedLocationSpecificPart);
}
