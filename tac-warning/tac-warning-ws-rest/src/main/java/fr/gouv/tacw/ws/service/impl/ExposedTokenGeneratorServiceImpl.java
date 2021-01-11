package fr.gouv.tacw.ws.service.impl;

import java.util.stream.Stream;

import org.springframework.stereotype.Service;

import fr.gouv.tacw.database.model.ExposedStaticVisitEntity;
import fr.gouv.tacw.model.ExposedTokenGenerator;
import fr.gouv.tacw.ws.configuration.TacWarningWsRestConfiguration;
import fr.gouv.tacw.ws.service.ExposedTokenGeneratorService;
import fr.gouv.tacw.ws.service.ScoringService;
import fr.gouv.tacw.ws.vo.VisitVo;

@Service
public class ExposedTokenGeneratorServiceImpl implements ExposedTokenGeneratorService {

	private final TacWarningWsRestConfiguration configuration;
    private final ScoringService scoringService;

	/**
	 * Used by tests
	 */
	public int numberOfGeneratedTokens() {
		return this.configuration.getMaxSalt();
	}
    
    public ExposedTokenGeneratorServiceImpl(TacWarningWsRestConfiguration configuration, ScoringService scoringService) {
        super();
        this.configuration = configuration;
        this.scoringService = scoringService;
    }

    @Override
    public Stream<ExposedStaticVisitEntity> generateAllExposedTokens(VisitVo visit) {
        return new ExposedTokenGenerator(visit, configuration, scoringService).generateAllExposedTokens();
    }
    
}
