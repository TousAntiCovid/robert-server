package fr.gouv.clea.consumer.service;

import fr.gouv.clea.consumer.data.ExposedVisit;
import fr.gouv.clea.consumer.model.DecodedVisit;

import java.util.Optional;

public interface IVerificationService {

    Optional<ExposedVisit> decryptAndValidate(DecodedVisit decodedVisit);
}
