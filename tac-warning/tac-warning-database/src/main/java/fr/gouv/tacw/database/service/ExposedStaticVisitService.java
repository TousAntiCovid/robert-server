package fr.gouv.tacw.database.service;

import java.util.List;

import fr.gouv.tacw.database.model.ExposedStaticVisitEntity;

public interface ExposedStaticVisitService {
    public void registerExposedStaticVisitEntities(List<ExposedStaticVisitEntity> exposedStaticVisitEntities);

    public long riskScore(String token, long visitTime);

    public long deleteExpiredTokens();
}
