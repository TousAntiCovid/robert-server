package fr.gouv.clea.consumer.service;

import java.util.Optional;

import fr.gouv.clea.consumer.model.DecodedVisit;
import fr.gouv.clea.consumer.model.Visit;

public interface IDecodedVisitService {

    Optional<Visit> decryptAndValidate(DecodedVisit decodedVisit);
}
