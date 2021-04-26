package fr.gouv.clea.identification;

import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 
 * Mock implementation that return same value for any venue type/categories
 * 
 */
@Component
public class RiskConfigurationService {

	static final RiskLevelConfig DEFAULT=new RiskLevelConfig(3, 1, 3.0f, 2.0f);
	
	public Optional<RiskLevelConfig> evaluate(int venueType, int venueCategory1, int venueCategory2) {
		return Optional.of(DEFAULT);
	}
}
