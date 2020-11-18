package fr.gouv.tacw.database.service;

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

	public boolean exposedStaticTokensIncludes(String token) {
		return staticTokenRepository.findByToken(token).isPresent();
	}
}
