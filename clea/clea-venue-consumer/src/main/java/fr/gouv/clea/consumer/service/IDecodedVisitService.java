package fr.gouv.clea.consumer.service;

import fr.gouv.clea.consumer.model.DecodedVisit;
import fr.gouv.clea.consumer.model.ExposedVisitEntity;

import java.util.Optional;

public interface IDecodedVisitService {

    Optional<ExposedVisitEntity> decryptAndValidate(DecodedVisit decodedVisit);
}
