package fr.gouv.clea.consumer.service;

import fr.gouv.clea.consumer.model.ExposedVisitEntity;
import fr.gouv.clea.consumer.model.Visit;

import java.util.List;

public interface IAggregateService {

    List<ExposedVisitEntity> aggregate(Visit visit);
}
