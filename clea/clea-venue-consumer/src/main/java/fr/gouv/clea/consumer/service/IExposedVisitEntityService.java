package fr.gouv.clea.consumer.service;

import fr.gouv.clea.consumer.model.ExposedVisitEntity;

import java.util.List;

public interface IExposedVisitEntityService {

    ExposedVisitEntity persist(ExposedVisitEntity exposedVisitEntity);

    List<ExposedVisitEntity> persistMany(List<ExposedVisitEntity> exposedVisitEntities);

    void deleteOutdatedExposedVisits();
}
