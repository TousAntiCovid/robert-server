package fr.gouv.tacw.database.service;


import java.util.List;

import fr.gouv.tacw.database.model.ExposedStaticVisitTokenEntity;

public interface TokenService {
	public void registerExposedStaticToken(long timestamp, String token);
	public void registerExposedStaticTokens(List<ExposedStaticVisitTokenEntity> exposedStaticVisitTokenEntities );
	public boolean exposedStaticTokensIncludes(String token);
	public long deleteExpiredTokens();
}
