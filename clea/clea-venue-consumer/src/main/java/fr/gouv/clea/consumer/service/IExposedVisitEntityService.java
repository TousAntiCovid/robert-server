package fr.gouv.clea.consumer.service;

import fr.gouv.clea.consumer.model.ExposedVisitEntity;

public interface IExposedVisitEntityService {

    ExposedVisitEntity persist(ExposedVisitEntity exposedVisitEntity);

    void deleteOutdatedExposedVisits();
}
