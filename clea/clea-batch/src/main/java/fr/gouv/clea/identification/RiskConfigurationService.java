package fr.gouv.clea.identification;

import fr.gouv.clea.clea.scoring.configuration.risk.RiskConfiguration;
import fr.gouv.clea.clea.scoring.configuration.risk.RiskRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 
 * Mock implementation that return same value for any venue type/categories
 *
 */
@Component
public class RiskConfigurationService {

	@Autowired
	private RiskConfiguration riskConfiguration;

	static final RiskLevelConfig DEFAULT = new RiskLevelConfig(3, 1, 3.0f, 2.0f);

	public Optional<RiskRule> evaluate(int venueType, int venueCategory1, int venueCategory2) {
		return Optional.of(riskConfiguration.getConfigurationFor(venueType, venueCategory1, venueCategory2));
	}
}
