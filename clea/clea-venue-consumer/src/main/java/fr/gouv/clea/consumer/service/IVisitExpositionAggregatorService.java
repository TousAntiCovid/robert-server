package fr.gouv.clea.consumer.service;

import fr.gouv.clea.consumer.model.Visit;

public interface IVisitExpositionAggregatorService {

    void updateExposureCount(Visit visit);
}
