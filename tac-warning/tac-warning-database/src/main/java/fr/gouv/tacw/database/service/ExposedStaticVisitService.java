package fr.gouv.tacw.database.service;

import java.util.List;

import fr.gouv.tacw.database.model.ExposedStaticVisitEntity;
import fr.gouv.tacw.database.model.ScoreResult;

public interface ExposedStaticVisitService {
    public void registerExposedStaticVisitEntities(List<ExposedStaticVisitEntity> exposedStaticVisitEntities);

    public List<ScoreResult> riskScore(String token, long visitTime);

    public long deleteExpiredTokens();
}
