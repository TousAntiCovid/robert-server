package fr.gouv.tacw.ws.service.impl;

import java.util.Map;

import org.springframework.stereotype.Service;

import fr.gouv.tacw.database.model.RiskLevel;
import fr.gouv.tacw.ws.configuration.TacWarningWsRestConfiguration;
import fr.gouv.tacw.ws.configuration.VenueConfiguration;
import fr.gouv.tacw.ws.service.ScoringService;
import fr.gouv.tacw.ws.vo.VenueTypeVo;

@Service
public class ScoringServiceImpl implements ScoringService {
    private final TacWarningWsRestConfiguration configuration;
    private Map<String, VenueConfiguration> venuesConfig;

    public ScoringServiceImpl(TacWarningWsRestConfiguration configuration) {
        super();
        this.configuration = configuration;
        venuesConfig = this.configuration.getVenues();
    }

    @Override
    public long getScoreIncrement(VenueTypeVo venueType) {
        VenueConfiguration venueConfig = this.getVenueConfiguration(venueType);
        int positiveCasesThreshold = venueConfig.getPositiveCasesThreshold() == null ? 
                this.getDefaultVenueConfiguration().getPositiveCasesThreshold() : venueConfig.getPositiveCasesThreshold();
        return (long) Math.ceil(configuration.getScoreThreshold() / (double) positiveCasesThreshold);
    }

    @Override
    public RiskLevel getVenueRiskLevel(VenueTypeVo venueType) {
        VenueConfiguration venueConfig = this.getVenueConfiguration(venueType);
        return venueConfig.getRiskLevel() == null ? 
                this.getDefaultVenueConfiguration().getRiskLevel() : venueConfig.getRiskLevel();
    }

    protected VenueConfiguration getVenueConfiguration(VenueTypeVo venueType) {
        return venuesConfig.getOrDefault(venueType.toString(), this.getDefaultVenueConfiguration());
    }

    protected VenueConfiguration getDefaultVenueConfiguration() {
        return venuesConfig.get("default");
    }

}
