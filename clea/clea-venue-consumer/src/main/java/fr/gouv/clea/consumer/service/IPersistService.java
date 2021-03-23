package fr.gouv.clea.consumer.service;

import fr.gouv.clea.consumer.data.ExposedVisit;

public interface IPersistService {

    ExposedVisit persist(ExposedVisit exposedVisit);

    void deleteOutdatedExposedVisits();
}
