package fr.gouv.tacw.database.service;


import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import fr.gouv.tacw.database.model.ExposedStaticVisitTokenEntity;
import fr.gouv.tacw.database.repository.StaticTokenRepository;

@Service
public class TokenService {
	@Autowired
	StaticTokenRepository staticTokenRepository;

	public void registerExposedStaticToken(long timestamp, String token) {
		staticTokenRepository.save(new ExposedStaticVisitTokenEntity(timestamp, token));
	}

	public void registerExposedStaticTokens(List<ExposedStaticVisitTokenEntity> exposedStaticVisitTokenEntities ) {
		staticTokenRepository.saveAll(exposedStaticVisitTokenEntities);
	}
	public boolean exposedStaticTokensIncludes(String token) {
		return staticTokenRepository.findByToken(token).isPresent();
	}
}
